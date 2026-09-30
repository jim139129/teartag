package dev.teartag.client;

import dev.teartag.network.SyncNametagPayload;
import dev.teartag.config.ConfigManager;
import dev.teartag.item.NametagItemData;
import dev.teartag.item.NametagItems;
import dev.teartag.state.NametagEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNametagStore {
    private static final Map<UUID, ClientNametagState> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> BY_ENTITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> REMOVED = new ConcurrentHashMap<>();

    private ClientNametagStore() {
    }

    public static void put(SyncNametagPayload payload) {
        REMOVED.remove(payload.playerId());
        ClientNametagState state = new ClientNametagState(payload.entityId(), payload.playerId(), payload.enabled(), payload.text(), payload.tears(),
            payload.eliminated(), payload.requiredTears(), payload.attackDistance(), payload.width(), payload.height(), payload.backOffset(), payload.verticalOffset(),
            payload.textScale(), payload.maxTextLines(), payload.paperColor(), payload.borderColor(), payload.textColor(), payload.soundsEnabled(), payload.tornAnimationTicks());
        ClientNametagState previous = BY_UUID.put(payload.playerId(), state);
        if (previous != null) BY_ENTITY.remove(previous.entityId());
        BY_ENTITY.put(payload.entityId(), payload.playerId());
    }

    public static ClientNametagState byEntity(int id) {
        var entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
        if (entity instanceof AbstractClientPlayer player && player instanceof NametagEntityData data
            && (data.teartag$isNametagEnabled() || !data.teartag$getNametagText().isEmpty())) {
            return fromEntity(data, player);
        }
        UUID uuid = BY_ENTITY.get(id);
        if (uuid != null) return BY_UUID.get(uuid);
        if (!(entity instanceof AbstractClientPlayer player)) return null;
        if (REMOVED.containsKey(player.getUUID())) return null;
        return fromItem(player);
    }

    public static ClientNametagState byUuid(UUID id) {
        return BY_UUID.get(id);
    }

    public static void remove(UUID id) {
        ClientNametagState state = BY_UUID.remove(id);
        if (state != null) BY_ENTITY.remove(state.entityId());
        REMOVED.put(id, Boolean.TRUE);
    }

    public static void clear() {
        BY_UUID.clear();
        BY_ENTITY.clear();
        REMOVED.clear();
    }

    private static ClientNametagState fromItem(AbstractClientPlayer player) {
        ItemStack item = TrinketsClientBridge.nametagStack(player);
        if (item.isEmpty()) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.is(NametagItems.NAMETAG)) {
                    item = stack;
                    break;
                }
            }
        }
        var snapshot = NametagItemData.read(item).orElse(null);
        if (snapshot == null) return null;
        var c = ConfigManager.get();
        return new ClientNametagState(player.getId(), player.getUUID(), true, snapshot.text(), 0, false,
            c.requiredTears(), c.attackDistance(), c.tagWidth(), c.tagHeight(), c.tagBackOffset(), c.tagVerticalOffset(),
            c.textScale(), c.maxTextLines(), c.paperColor(), c.borderColor(), c.textColor(), c.soundsEnabled(), c.tornAnimationTicks());
    }

    private static ClientNametagState fromEntity(NametagEntityData data, AbstractClientPlayer player) {
        var c = ConfigManager.get();
        return new ClientNametagState(player.getId(), player.getUUID(), data.teartag$isNametagEnabled(), NametagEntityData.text(data), data.teartag$getNametagTears(), data.teartag$isNametagEliminated(),
            c.requiredTears(), c.attackDistance(), c.tagWidth(), c.tagHeight(), c.tagBackOffset(), c.tagVerticalOffset(),
            c.textScale(), c.maxTextLines(), c.paperColor(), c.borderColor(), c.textColor(), c.soundsEnabled(), c.tornAnimationTicks());
    }
}
