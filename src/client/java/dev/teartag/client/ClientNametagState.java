package dev.teartag.client;

import java.util.UUID;
import net.minecraft.network.chat.Component;

public record ClientNametagState(
    int entityId, UUID playerId, boolean enabled, Component text, int tears, boolean eliminated, int requiredTears,
    double attackDistance, double width, double height, double backOffset, double verticalOffset, float textScale, int maxTextLines,
    int paperColor, int borderColor, boolean soundsEnabled, int tornAnimationTicks
) {
}
