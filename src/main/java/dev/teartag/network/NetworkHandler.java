package dev.teartag.network;

import dev.teartag.config.ConfigManager;
import dev.teartag.config.NameTagConfig;
import dev.teartag.game.NametagService;
import dev.teartag.integration.AccessoryIntegrations;
import dev.teartag.state.NametagSavedData;
import dev.teartag.state.PlayerNametag;
import dev.teartag.state.NametagEntityData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(AttemptTearPayload.TYPE, AttemptTearPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncNametagPayload.TYPE, SyncNametagPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RemoveNametagPayload.TYPE, RemoveNametagPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FeedbackPayload.TYPE, FeedbackPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AttemptTearPayload.TYPE,
            (payload, context) -> NametagService.tryTear(context.player(), payload.targetId()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joined = handler.getPlayer();
            syncAllTo(joined, server);
            PlayerNametag joinedState = NametagSavedData.get(server).get(joined.getUUID());
            if (joinedState != null && joinedState.enabled) {
                NametagEntityData.write(joined, joinedState);
                AccessoryIntegrations.ensureNametagEquipped(joined);
                syncToAll(joined, joinedState);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerNametag state = NametagSavedData.get(newPlayer.level().getServer()).get(newPlayer.getUUID());
            if (state != null && state.enabled) {
                NametagEntityData.write(newPlayer, state);
                AccessoryIntegrations.ensureNametagEquipped(newPlayer);
                syncToAll(newPlayer, state);
            }
        });
        ServerPlayerEvents.LEAVE.register(NetworkHandler::removeFromAll);
    }

    public static void syncAllTo(ServerPlayer receiver, MinecraftServer server) {
        NametagSavedData data = NametagSavedData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerNametag state = data.get(player.getUUID());
            if (state != null && state.enabled) send(receiver, player, state);
        }
    }

    public static void syncToAll(ServerPlayer subject, PlayerNametag state) {
        for (ServerPlayer receiver : subject.level().getServer().getPlayerList().getPlayers()) send(receiver, subject, state);
    }

    public static void removeFromAll(ServerPlayer subject) {
        RemoveNametagPayload payload = new RemoveNametagPayload(subject.getUUID());
        for (ServerPlayer receiver : subject.level().getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(receiver, RemoveNametagPayload.TYPE)) ServerPlayNetworking.send(receiver, payload);
        }
    }

    public static void feedbackToAll(FeedbackPayload.Kind kind, ServerPlayer target) {
        FeedbackPayload payload = new FeedbackPayload(kind, target.getId());
        for (ServerPlayer receiver : target.level().getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(receiver, FeedbackPayload.TYPE)) ServerPlayNetworking.send(receiver, payload);
        }
    }

    private static void send(ServerPlayer receiver, ServerPlayer subject, PlayerNametag state) {
        if (!ServerPlayNetworking.canSend(receiver, SyncNametagPayload.TYPE)) return;
        NameTagConfig c = ConfigManager.get();
        ServerPlayNetworking.send(receiver, new SyncNametagPayload(subject.getId(), subject.getUUID(), state.enabled, state.text.copy(),
            state.tears, state.eliminated, c.requiredTears(), c.attackDistance(), c.tagWidth(), c.tagHeight(), c.tagBackOffset(), c.tagVerticalOffset(),
            c.textScale(), c.maxTextLines(), c.paperColor(), c.borderColor(), c.textColor(), c.soundsEnabled(), c.tornAnimationTicks()));
    }
}
