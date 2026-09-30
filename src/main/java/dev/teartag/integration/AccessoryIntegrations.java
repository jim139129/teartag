package dev.teartag.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AccessoryIntegrations {
    private static volatile AccessoryIntegration current = AccessoryIntegration.NONE;

    private AccessoryIntegrations() {
    }

    public static void install(AccessoryIntegration integration) {
        current = integration == null ? AccessoryIntegration.NONE : integration;
    }

    public static void ensureNametagEquipped(ServerPlayer player) {
        current.ensureNametagEquipped(player);
    }

    public static boolean isAvailable() {
        return current.isAvailable();
    }

    public static boolean isNametagEquipped(ServerPlayer player) {
        return current.isNametagEquipped(player);
    }

    public static ItemStack nametagStack(ServerPlayer player) {
        return current.nametagStack(player);
    }

    public static void onNametagUnequipped(ServerPlayer player) {
        current.onNametagUnequipped(player);
    }
}
