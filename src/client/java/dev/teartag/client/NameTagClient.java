package dev.teartag.client;

import dev.teartag.client.render.NametagRenderLayer;
import dev.teartag.network.FeedbackPayload;
import dev.teartag.network.RemoveNametagPayload;
import dev.teartag.network.SyncNametagPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.player.PlayerModel;

public final class NameTagClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncNametagPayload.TYPE, (payload, context) -> ClientNametagStore.put(payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoveNametagPayload.TYPE, (payload, context) -> ClientNametagStore.remove(payload.playerId()));
        ClientPlayNetworking.registerGlobalReceiver(FeedbackPayload.TYPE, (payload, context) -> ClientFeedback.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientNametagStore.clear();
            ClientFeedback.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientAttackHandler.tick(client);
            ClientFeedback.tick();
        });
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
                @SuppressWarnings("unchecked")
                var parent = (net.minecraft.client.renderer.entity.RenderLayerParent<AvatarRenderState, PlayerModel>) avatarRenderer;
                helper.register(new NametagRenderLayer(parent));
            }
        });
    }
}
