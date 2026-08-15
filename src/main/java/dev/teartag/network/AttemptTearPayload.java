package dev.teartag.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AttemptTearPayload(UUID targetId) implements CustomPacketPayload {
    public static final Type<AttemptTearPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("teartag", "attempt_tear"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttemptTearPayload> CODEC = CustomPacketPayload.codec(
        (payload, buf) -> buf.writeUUID(payload.targetId), buf -> new AttemptTearPayload(buf.readUUID())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
