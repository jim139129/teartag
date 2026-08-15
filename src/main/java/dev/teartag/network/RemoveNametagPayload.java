package dev.teartag.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveNametagPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<RemoveNametagPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("teartag", "remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNametagPayload> CODEC = CustomPacketPayload.codec(
        (payload, buf) -> buf.writeUUID(payload.playerId), buf -> new RemoveNametagPayload(buf.readUUID())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
