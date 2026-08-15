package dev.teartag.integration;

import dev.teartag.NameTagMod;
import dev.teartag.config.ConfigManager;
import dev.teartag.game.NametagService;
import dev.teartag.item.NametagItems;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Optional Trinkets Updated integration. Loaded only when the mod is present. */
public final class TrinketsIntegration implements AccessoryIntegration {
    public static final String MOD_ID = "trinkets_updated";
    public static final String SLOT_ID = "teartag/nametag";

    private TrinketsIntegration() {
    }

    public static TrinketsIntegration initialize() {
        TrinketsIntegration integration = new TrinketsIntegration();
        TrinketCallback.setCallback(NametagItems.NAMETAG, new TrinketCallback() {
            @Override
            public void onEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
                if (entity instanceof ServerPlayer player && ConfigManager.get().trinketsEnableOnEquip()) {
                    var state = NametagService.state(player);
                    if (state == null || !state.enabled) NametagService.enable(player);
                }
            }

            @Override
            public void onUnequip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
                if (entity instanceof ServerPlayer player) {
                    var state = NametagService.state(player);
                    if (state != null && state.enabled) AccessoryIntegrations.onNametagUnequipped(player);
                }
            }

            @Override
            public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
                return true;
            }
        });
        NameTagMod.LOGGER.info("Trinkets Updated compatibility enabled; registered slot {}", SLOT_ID);
        return integration;
    }

    @Override
    public void ensureNametagEquipped(ServerPlayer player) {
        if (!ConfigManager.get().trinketsAutoEquipOnEnable()) return;
        var attachment = TrinketsApi.getAttachment(player);
        TrinketSlotAccess slot = attachment.getSlotAccess(SLOT_ID, 0);
        if (slot == null || !slot.get().isEmpty()) return;

        ItemStack stack = findInventoryStack(player);
        slot.set(stack.isEmpty() ? new ItemStack(NametagItems.NAMETAG) : stack.split(1));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onNametagUnequipped(ServerPlayer player) {
        if (ConfigManager.get().trinketsDisableOnUnequip()) {
            NametagService.disable(player);
        }
    }

    @Override
    public boolean isNametagEquipped(ServerPlayer player) {
        return TrinketsApi.getAttachment(player).isEquipped(NametagItems.NAMETAG);
    }

    private static ItemStack findInventoryStack(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(NametagItems.NAMETAG)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
