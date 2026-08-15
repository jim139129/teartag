package dev.teartag.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FeedbackPayload(Kind kind, int targetEntityId) implements CustomPacketPayload {
    public enum Kind { SUCCESS, WALL_FAILED, ELIMINATED }
    public static final Type<FeedbackPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("teartag", "feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FeedbackPayload> CODEC = CustomPacketPayload.codec(
        (payload, buf) -> { buf.writeVarInt(payload.kind.ordinal()); buf.writeVarInt(payload.targetEntityId); },
        buf -> new FeedbackPayload(Kind.values()[Math.min(buf.readVarInt(), Kind.values().length - 1)], buf.readVarInt())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
