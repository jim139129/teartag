package dev.teartag.api;

import dev.teartag.game.NametagService;
import dev.teartag.integration.AccessoryIntegrations;
import dev.teartag.rule.AttackRule;
import dev.teartag.state.NametagSavedData;
import dev.teartag.state.PlayerNametag;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class NametagApi {
    private NametagApi() {
    }

    public static Optional<NametagView> get(MinecraftServer server, UUID playerId) {
        PlayerNametag state = NametagSavedData.get(server).get(playerId);
        return state == null ? Optional.empty() : Optional.of(state.view(playerId));
    }

    public static NametagView enable(ServerPlayer player) {
        return NametagService.enable(player).view(player.getUUID());
    }

    public static boolean hasAccessoryIntegration() {
        return AccessoryIntegrations.isAvailable();
    }

    public static boolean isAccessoryEquipped(ServerPlayer player) {
        return AccessoryIntegrations.isNametagEquipped(player);
    }

    public static void disable(ServerPlayer player) {
        NametagService.disable(player);
    }

    public static void setText(ServerPlayer player, Component text) {
        NametagService.setText(player, text);
    }

    public static void resetRound(ServerPlayer player) {
        NametagService.resetRound(player);
    }

    public static void resetAll(ServerPlayer player) {
        NametagService.resetAll(player);
    }

    public static boolean setRule(ServerPlayer player, Identifier rule) {
        return NametagService.setRule(player, rule);
    }

    /**
     * Returns TearTag's current participant-damage policy for this direction.
     * {@code ALLOW} means TearTag does not suppress damage; it does not bypass
     * Minecraft's other damage checks or other mods' callbacks.
     */
    public static AttackRule.DamagePolicy participantDamagePolicy(ServerPlayer attacker, ServerPlayer target) {
        return NametagService.participantDamagePolicy(attacker, target);
    }

    public static void tryTear(ServerPlayer attacker, UUID target) {
        NametagService.tryTear(attacker, target);
    }
}
