package dev.teartag.integration;

import net.minecraft.server.level.ServerPlayer;

/**
 * Small server-side bridge for optional accessory integrations.
 *
 * The core game logic only talks to this interface, so an absent optional mod
 * cannot make the normal nametag path resolve its classes.
 */
public interface AccessoryIntegration {
    AccessoryIntegration NONE = new AccessoryIntegration() {
    };

    default void ensureNametagEquipped(ServerPlayer player) {
    }

    default boolean isAvailable() {
        return false;
    }

    default boolean isNametagEquipped(ServerPlayer player) {
        return false;
    }

    default void onNametagUnequipped(ServerPlayer player) {
    }
}
