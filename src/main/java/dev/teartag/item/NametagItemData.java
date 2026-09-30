package dev.teartag.item;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Display text carried by the physical nametag item. Gameplay state lives on the player. */
public final class NametagItemData {
    private static final String ROOT = "teartag";
    private static final String TEXT = "text";

    private NametagItemData() {
    }

    public static void write(ItemStack stack, Component text) {
        if (stack.isEmpty() || !stack.is(NametagItems.NAMETAG)) return;
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = new CompoundTag();
        ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, text).result()
            .ifPresent(encoded -> data.put(TEXT, encoded));
        root.put(ROOT, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.CUSTOM_NAME, text.copy());
    }

    public static Optional<Snapshot> read(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(NametagItems.NAMETAG)) return Optional.empty();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ROOT)) {
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            return customName == null ? Optional.empty() : Optional.of(new Snapshot(customName.copy()));
        }
        CompoundTag data = root.getCompound(ROOT).orElse(null);
        if (data == null || !data.contains(TEXT)) {
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            return customName == null ? Optional.empty() : Optional.of(new Snapshot(customName.copy()));
        }
        var encodedText = data.get(TEXT);
        if (encodedText == null) return Optional.empty();
        Component text = ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, encodedText).result().orElse(Component.empty());
        return Optional.of(new Snapshot(text));
    }

    public record Snapshot(Component text) {
    }
}
