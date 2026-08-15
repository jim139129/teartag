package dev.teartag.game;

import dev.teartag.api.NametagEvents;
import dev.teartag.config.ConfigManager;
import dev.teartag.network.NetworkHandler;
import dev.teartag.network.FeedbackPayload;
import dev.teartag.rule.AttackRule;
import dev.teartag.rule.AttackRuleManager;
import dev.teartag.integration.AccessoryIntegrations;
import dev.teartag.state.NametagSavedData;
import dev.teartag.state.PlayerNametag;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

public final class NametagService {
    private static final Map<UUID, Long> ATTACK_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, HudTarget> ATTACKER_HUD = new HashMap<>();
    private static final Map<UUID, PacketWindow> PACKET_WINDOWS = new HashMap<>();
    private NametagService() {
    }

    public static PlayerNametag enable(ServerPlayer player) {
        NametagSavedData data = NametagSavedData.get(player.level().getServer());
        PlayerNametag state = data.get(player.getUUID());
        if (state == null) {
            state = PlayerNametag.create(player.getDisplayName());
            data.put(player.getUUID(), state);
        } else {
            state.enabled = true;
            data.setDirty();
        }
        NetworkHandler.syncToAll(player, state);
        AccessoryIntegrations.ensureNametagEquipped(player);
        DatapackEvents.fire("on_enable", player, null, false);
        for (var listener : NametagEvents.ENABLED) listener.accept(player, state.view(player.getUUID()));
        return state;
    }

    public static void disable(ServerPlayer player) {
        PlayerNametag state = state(player);
        if (state == null) return;
        state.enabled = false;
        NametagSavedData.get(player.level().getServer()).setDirty();
        NetworkHandler.removeFromAll(player);
        DatapackEvents.fire("on_disable", player, null, false);
        for (var listener : NametagEvents.DISABLED) listener.accept(player, state.view(player.getUUID()));
    }

    public static void setText(ServerPlayer player, Component text) {
        PlayerNametag state = requireState(player);
        state.text = sanitize(text);
        changed(player, state);
    }

    public static void resetText(ServerPlayer player) {
        setText(player, player.getDisplayName());
    }

    public static void resetRound(ServerPlayer player) {
        PlayerNametag state = requireState(player);
        state.tears = 0;
        state.eliminated = false;
        state.nextRecoveryAtMillis = 0;
        state.eliminateAtMillis = 0;
        state.teleportClaimed = false;
        changed(player, state);
        DatapackEvents.fire("on_reset", player, null, false);
    }

    public static void resetAll(ServerPlayer player) {
        NametagSavedData.get(player.level().getServer()).remove(player.getUUID());
        NetworkHandler.removeFromAll(player);
        DatapackEvents.fire("on_reset", player, null, false);
    }

    public static boolean setRule(ServerPlayer player, Identifier rule) {
        if (!AttackRuleManager.exists(rule)) return false;
        PlayerNametag state = requireState(player);
        state.ruleId = rule;
        changed(player, state);
        return true;
    }

    public static void tryTear(ServerPlayer attacker, UUID targetId) {
        long now = attacker.level().getServer().getTickCount();
        if (!acceptPacket(attacker.getUUID(), now, ConfigManager.get().maxAttemptPacketsPerSecond())) return;
        ServerPlayer target = attacker.level().getServer().getPlayerList().getPlayer(targetId);
        if (target == null) return;
        PlayerNametag attackerState = state(attacker);
        PlayerNametag targetState = state(target);
        if (!active(attackerState) || !active(targetState)) return;
        if (!AttackRuleManager.get(targetState.ruleId).permits(attacker, target)) return;
        if (ATTACK_COOLDOWNS.getOrDefault(attacker.getUUID(), 0L) > now) return;
        var config = ConfigManager.get();
        HitDetector.Result hit = HitDetector.detect(attacker, target, config);
        if (hit == HitDetector.Result.MISS) return;
        boolean wall = hit == HitDetector.Result.WALL_ATTEMPT;
        if (wall && attacker.getRandom().nextDouble() >= config.wallSuccessChance()) {
            ATTACK_COOLDOWNS.put(attacker.getUUID(), now + config.wallAttemptCooldownTicks());
            sendFeedback(attacker, FeedbackPayload.Kind.WALL_FAILED, target);
            if (config.soundsEnabled()) attacker.playSound(SoundEvents.VILLAGER_NO, 0.6F, 1.4F);
            DatapackEvents.fire("on_wall_attempt_failed", target, attacker, true);
            return;
        }
        for (var listener : NametagEvents.BEFORE_TEAR) {
            if (!listener.accept(attacker, target, targetState.view(target.getUUID()), wall)) return;
        }
        ATTACK_COOLDOWNS.put(attacker.getUUID(), now + config.attackCooldownTicks());
        targetState.tears++;
        targetState.nextRecoveryAtMillis = System.currentTimeMillis() + config.firstRecoveryDelayTicks() * 50L;
        NametagSavedData.get(target.level().getServer()).setDirty();
        ATTACKER_HUD.put(attacker.getUUID(), new HudTarget(target.getUUID(), config.attackerActionbarTicks()));
        if (config.soundsEnabled()) target.playSound(SoundEvents.PLAYER_HURT, 0.8F, 1.2F);
        sendFeedback(attacker, FeedbackPayload.Kind.SUCCESS, target);
        DatapackEvents.fire("on_tear_success", target, attacker, wall);
        for (var listener : NametagEvents.AFTER_TEAR) listener.accept(attacker, target, targetState.view(target.getUUID()), wall);
        if (targetState.tears >= config.requiredTears()) eliminate(attacker, target, targetState, wall);
        NetworkHandler.syncToAll(target, targetState);
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        long now = server.getTickCount();
        long nowMillis = System.currentTimeMillis();
        NametagSavedData data = NametagSavedData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerNametag state = data.get(player.getUUID());
            if (state == null || !state.enabled) continue;
            if (state.eliminated) {
                if (ConfigManager.get().particlesEnabled()) player.level().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.25, 0.35, 0.25, 0.03);
                if (state.eliminateAtMillis > 0 && nowMillis >= state.eliminateAtMillis) finishElimination(player, state);
            } else if (state.tears > 0 && state.nextRecoveryAtMillis > 0 && nowMillis >= state.nextRecoveryAtMillis) {
                state.tears--;
                state.nextRecoveryAtMillis = state.tears == 0 ? 0 : nowMillis + ConfigManager.get().recoveryIntervalTicks() * 50L;
                data.setDirty();
                NetworkHandler.syncToAll(player, state);
                DatapackEvents.fire("on_recover", player, null, false);
                for (var listener : NametagEvents.RECOVERED) listener.accept(player, state.view(player.getUUID()));
            }
            if (ConfigManager.get().actionbarEnabled()) showHud(player, state);
        }
        ATTACK_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() + 200 < now);
        PACKET_WINDOWS.entrySet().removeIf(entry -> entry.getValue().startedAt() + 40 < now);
        ATTACKER_HUD.replaceAll((id, hud) -> new HudTarget(hud.targetId(), hud.ticks() - 1));
        ATTACKER_HUD.entrySet().removeIf(entry -> entry.getValue().ticks() <= 0);
    }

    public static boolean allowParticipantDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer target) || !(source.getEntity() instanceof ServerPlayer attacker)) return true;
        PlayerNametag a = state(attacker);
        PlayerNametag t = state(target);
        if (!active(a) || !active(t)) return true;

        boolean allowed = participantDamagePolicy(attacker, target) == AttackRule.DamagePolicy.ALLOW;
        var decision = new NametagEvents.DamageDecision(allowed);
        for (var listener : NametagEvents.PARTICIPANT_DAMAGE) listener.accept(attacker, target, amount, decision);
        DatapackEvents.fireParticipantDamage(target, attacker, amount, decision.isAllowed());
        return decision.isAllowed();
    }

    /** @deprecated Use {@link #allowParticipantDamage(LivingEntity, DamageSource, float)}. */
    @Deprecated
    public static boolean shouldCancelParticipantDamage(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer target) || !(source.getEntity() instanceof ServerPlayer attacker)) return false;
        PlayerNametag a = state(attacker);
        PlayerNametag t = state(target);
        if (!active(a) || !active(t)) return false;
        return participantDamagePolicy(attacker, target) != AttackRule.DamagePolicy.ALLOW;
    }

    public static AttackRule.DamagePolicy participantDamagePolicy(ServerPlayer attacker, ServerPlayer target) {
        if (attacker.level() != target.level()) return AttackRule.DamagePolicy.ALLOW;
        PlayerNametag a = state(attacker);
        PlayerNametag t = state(target);
        if (!active(a) || !active(t)) return AttackRule.DamagePolicy.ALLOW;
        return AttackRuleManager.get(t.ruleId).participantDamage(attacker, target);
    }

    private static void eliminate(ServerPlayer attacker, ServerPlayer target, PlayerNametag state, boolean wallAttempt) {
        state.eliminated = true;
        state.tears = ConfigManager.get().requiredTears();
        state.eliminateAtMillis = System.currentTimeMillis() + ConfigManager.get().eliminationDelayTicks() * 50L;
        state.teleportClaimed = false;
        NetworkHandler.feedbackToAll(FeedbackPayload.Kind.ELIMINATED, target);
        if (ConfigManager.get().particlesEnabled()) target.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.3, 0.5, 0.3, 0.2);
        if (ConfigManager.get().titlesEnabled()) {
            target.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 50, 20));
            target.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("title.teartag.eliminated")));
            target.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable("subtitle.teartag.eliminated")));
        }
        DatapackEvents.fire("on_eliminate", target, attacker, wallAttempt);
        for (var listener : NametagEvents.ELIMINATED) listener.accept(attacker, target, state.view(target.getUUID()), wallAttempt);
    }

    private static void finishElimination(ServerPlayer player, PlayerNametag state) {
        state.eliminateAtMillis = 0;
        NametagSavedData.get(player.level().getServer()).setDirty();
        DatapackEvents.fire("before_default_teleport", player, null, false);
        if (state.teleportClaimed || !ConfigManager.get().defaultTeleport()) return;
        var c = ConfigManager.get();
        Identifier id = Identifier.tryParse(c.eliminationDimension());
        if (id == null) return;
        var dimensionKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        var level = player.level().getServer().getLevel(dimensionKey);
        if (level != null) {
            player.teleportTo(level, c.eliminationX(), c.eliminationY(), c.eliminationZ(), java.util.Set.of(), c.eliminationYaw(), c.eliminationPitch(), true);
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    private static void showHud(ServerPlayer player, PlayerNametag state) {
        HudTarget hud = ATTACKER_HUD.get(player.getUUID());
        if (hud != null) {
            ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(hud.targetId());
            PlayerNametag targetState = target == null ? null : state(target);
            if (target != null && targetState != null) {
                player.sendSystemMessage(Component.translatable("message.teartag.target_progress", target.getDisplayName(),
                    targetState.tears, ConfigManager.get().requiredTears()), true);
                return;
            }
        }
        Component own = Component.translatable("message.teartag.own_progress", state.tears, ConfigManager.get().requiredTears());
        player.sendSystemMessage(own, true);
    }

    private record HudTarget(UUID targetId, int ticks) {
    }

    private static void sendFeedback(ServerPlayer player, FeedbackPayload.Kind kind, ServerPlayer target) {
        if (ServerPlayNetworking.canSend(player, FeedbackPayload.TYPE)) {
            ServerPlayNetworking.send(player, new FeedbackPayload(kind, target.getId()));
        }
    }

    private static boolean acceptPacket(UUID playerId, long now, int limit) {
        PacketWindow current = PACKET_WINDOWS.get(playerId);
        if (current == null || now - current.startedAt() >= 20) {
            PACKET_WINDOWS.put(playerId, new PacketWindow(now, 1));
            return true;
        }
        if (current.count() >= limit) return false;
        PACKET_WINDOWS.put(playerId, new PacketWindow(current.startedAt(), current.count() + 1));
        return true;
    }

    private record PacketWindow(long startedAt, int count) {
    }

    public static PlayerNametag state(ServerPlayer player) {
        return NametagSavedData.get(player.level().getServer()).get(player.getUUID());
    }

    private static PlayerNametag requireState(ServerPlayer player) {
        PlayerNametag state = state(player);
        return state == null ? enable(player) : state;
    }

    private static boolean active(PlayerNametag state) {
        return state != null && state.enabled && !state.eliminated;
    }

    private static void changed(ServerPlayer player, PlayerNametag state) {
        NametagSavedData.get(player.level().getServer()).setDirty();
        NetworkHandler.syncToAll(player, state);
    }

    private static Component sanitize(Component source) {
        var config = ConfigManager.get();
        String plain = source.getString();
        int length = plain.codePointCount(0, plain.length());
        if (length <= config.maxTextCodePoints()) return source.copy();
        int end = plain.offsetByCodePoints(0, config.maxTextCodePoints());
        return Component.literal(plain.substring(0, end));
    }
}
