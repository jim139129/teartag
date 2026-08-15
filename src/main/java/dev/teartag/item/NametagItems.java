package dev.teartag.item;

import dev.teartag.NameTagMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class NametagItems {
    public static final ResourceKey<Item> NAMETAG_KEY = ResourceKey.create(Registries.ITEM, NameTagMod.id("nametag"));
    public static final Item NAMETAG = new Item(new Item.Properties().setId(NAMETAG_KEY).stacksTo(1));

    private NametagItems() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, NAMETAG_KEY, NAMETAG);
    }
}
