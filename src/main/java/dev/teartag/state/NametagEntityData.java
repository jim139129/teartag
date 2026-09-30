package dev.teartag.state;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;

/** State bridge stored on the player and exposed through entity metadata for replay recording. */
public interface NametagEntityData {
    String teartag$getNametagText();
    int teartag$getNametagTears();
    boolean teartag$isNametagEliminated();
    boolean teartag$isNametagEnabled();
    void teartag$setNametagState(Component text, int tears, boolean eliminated, boolean enabled);

    static void write(ServerPlayer player, PlayerNametag state) {
        ((NametagEntityData) (Object) player).teartag$setNametagState(state.text, state.tears, state.eliminated, state.enabled);
    }

    static Component text(NametagEntityData data) {
        String encoded = data.teartag$getNametagText();
        if (encoded.isEmpty()) return Component.empty();
        try {
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(encoded)).result().orElse(Component.literal(encoded));
        } catch (RuntimeException ignored) {
            return Component.literal(encoded);
        }
    }
}
