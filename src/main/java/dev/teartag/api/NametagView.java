package dev.teartag.api;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record NametagView(
    UUID playerId,
    boolean enabled,
    Component text,
    int tearCount,
    boolean eliminated,
    Identifier ruleId
) {
}
