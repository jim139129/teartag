package dev.teartag.client;

import dev.teartag.network.SyncNametagPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNametagStore {
    private static final Map<UUID, ClientNametagState> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> BY_ENTITY = new ConcurrentHashMap<>();

    private ClientNametagStore() {
    }

    public static void put(SyncNametagPayload payload) {
        ClientNametagState state = new ClientNametagState(payload.entityId(), payload.playerId(), payload.enabled(), payload.text(), payload.tears(),
            payload.eliminated(), payload.requiredTears(), payload.attackDistance(), payload.width(), payload.height(), payload.backOffset(), payload.verticalOffset(),
            payload.textScale(), payload.maxTextLines(), payload.paperColor(), payload.borderColor(), payload.soundsEnabled(), payload.tornAnimationTicks());
        ClientNametagState previous = BY_UUID.put(payload.playerId(), state);
        if (previous != null) BY_ENTITY.remove(previous.entityId());
        BY_ENTITY.put(payload.entityId(), payload.playerId());
    }

    public static ClientNametagState byEntity(int id) {
        UUID uuid = BY_ENTITY.get(id);
        return uuid == null ? null : BY_UUID.get(uuid);
    }

    public static ClientNametagState byUuid(UUID id) {
        return BY_UUID.get(id);
    }

    public static void remove(UUID id) {
        ClientNametagState state = BY_UUID.remove(id);
        if (state != null) BY_ENTITY.remove(state.entityId());
    }

    public static void clear() {
        BY_UUID.clear();
        BY_ENTITY.clear();
    }
}
