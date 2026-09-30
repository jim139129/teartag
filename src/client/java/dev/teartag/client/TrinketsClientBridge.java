package dev.teartag.client;

import dev.teartag.item.NametagItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;

/** Optional client-side lookup used when replaying without TearTag packets. */
final class TrinketsClientBridge {
    private static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("trinkets_updated");

    private TrinketsClientBridge() {
    }

    static ItemStack nametagStack(AbstractClientPlayer player) {
        if (!AVAILABLE) return ItemStack.EMPTY;
        try {
            Class<?> api = Class.forName("eu.pb4.trinkets.api.TrinketsApi");
            Object attachment = api.getMethod("getAttachment", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object slot = attachment.getClass().getMethod("getSlotAccess", String.class, int.class)
                .invoke(attachment, "teartag/nametag", 0);
            ItemStack stack = slot == null ? ItemStack.EMPTY : (ItemStack) slot.getClass().getMethod("get").invoke(slot);
            return stack.is(NametagItems.NAMETAG) ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }
}
