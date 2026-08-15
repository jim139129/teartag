package dev.teartag.client;

import dev.teartag.network.FeedbackPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientFeedback {
    private static final Map<Integer, Integer> TORN_ANIMATIONS = new ConcurrentHashMap<>();
    private ClientFeedback() {
    }

    public static void handle(FeedbackPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientNametagState target = ClientNametagStore.byEntity(payload.targetEntityId());
        boolean sound = target == null || target.soundsEnabled();
        switch (payload.kind()) {
            case SUCCESS -> { if (sound) player.playSound(SoundEvents.ITEM_BREAK.value(), 0.7F, 1.25F); }
            case WALL_FAILED -> { if (sound) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.6F); }
            case ELIMINATED -> {
                int ticks = target == null ? 30 : target.tornAnimationTicks();
                if (ticks > 0) TORN_ANIMATIONS.put(payload.targetEntityId(), ticks);
                if (sound) player.playSound(SoundEvents.TOTEM_USE, 0.8F, 0.9F);
            }
        }
    }

    public static void tick() {
        TORN_ANIMATIONS.replaceAll((id, ticks) -> ticks - 1);
        TORN_ANIMATIONS.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public static void clear() {
        TORN_ANIMATIONS.clear();
    }

    public static float animationProgress(int entityId) {
        Integer ticks = TORN_ANIMATIONS.get(entityId);
        ClientNametagState state = ClientNametagStore.byEntity(entityId);
        int duration = state == null ? 30 : Math.max(1, state.tornAnimationTicks());
        return ticks == null ? -1.0F : 1.0F - ticks / (float) duration;
    }
}
