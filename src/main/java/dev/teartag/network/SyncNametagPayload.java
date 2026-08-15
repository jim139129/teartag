package dev.teartag.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncNametagPayload(
    int entityId, UUID playerId, boolean enabled, Component text, int tears, boolean eliminated, int requiredTears,
    double attackDistance, double width, double height, double backOffset, double verticalOffset, float textScale, int maxTextLines,
    int paperColor, int borderColor, boolean soundsEnabled, int tornAnimationTicks
) implements CustomPacketPayload {
    public static final Type<SyncNametagPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("teartag", "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNametagPayload> CODEC = CustomPacketPayload.codec(
        SyncNametagPayload::write, SyncNametagPayload::read
    );

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUUID(playerId);
        buf.writeBoolean(enabled);
        ComponentSerialization.STREAM_CODEC.encode(buf, text);
        buf.writeVarInt(tears);
        buf.writeBoolean(eliminated);
        buf.writeVarInt(requiredTears);
        buf.writeDouble(attackDistance);
        buf.writeDouble(width);
        buf.writeDouble(height);
        buf.writeDouble(backOffset);
        buf.writeDouble(verticalOffset);
        buf.writeFloat(textScale);
        buf.writeVarInt(maxTextLines);
        buf.writeInt(paperColor);
        buf.writeInt(borderColor);
        buf.writeBoolean(soundsEnabled);
        buf.writeVarInt(tornAnimationTicks);
    }

    private static SyncNametagPayload read(RegistryFriendlyByteBuf buf) {
        return new SyncNametagPayload(buf.readVarInt(), buf.readUUID(), buf.readBoolean(), ComponentSerialization.STREAM_CODEC.decode(buf),
            buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
