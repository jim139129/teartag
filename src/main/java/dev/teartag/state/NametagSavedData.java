package dev.teartag.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class NametagSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerNametag>> PLAYERS_CODEC = Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), PlayerNametag.CODEC);
    private static final Codec<NametagSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PLAYERS_CODEC.optionalFieldOf("players", Map.of()).forGetter(data -> data.players)
    ).apply(instance, NametagSavedData::new));
    public static final SavedDataType<NametagSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("teartag", "players"), NametagSavedData::new, CODEC, DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerNametag> players;

    public NametagSavedData() {
        this.players = new LinkedHashMap<>();
    }

    private NametagSavedData(Map<UUID, PlayerNametag> players) {
        this.players = new LinkedHashMap<>(players);
    }

    public static NametagSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayerNametag get(UUID id) {
        return players.get(id);
    }

    public Map<UUID, PlayerNametag> entries() {
        return players;
    }

    public void put(UUID id, PlayerNametag state) {
        players.put(id, state);
        setDirty();
    }

    public PlayerNametag remove(UUID id) {
        PlayerNametag removed = players.remove(id);
        if (removed != null) setDirty();
        return removed;
    }
}
