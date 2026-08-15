package dev.teartag.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.teartag.api.NametagView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

public final class PlayerNametag {
    public static final Codec<PlayerNametag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(s -> s.enabled),
        ComponentSerialization.CODEC.fieldOf("text").forGetter(s -> s.text),
        Codec.INT.optionalFieldOf("tears", 0).forGetter(s -> s.tears),
        Codec.BOOL.optionalFieldOf("eliminated", false).forGetter(s -> s.eliminated),
        Identifier.CODEC.optionalFieldOf("rule", Identifier.fromNamespaceAndPath("teartag", "any_participant")).forGetter(s -> s.ruleId),
        Codec.LONG.optionalFieldOf("next_recovery_at_ms", 0L).forGetter(s -> s.nextRecoveryAtMillis),
        Codec.LONG.optionalFieldOf("eliminate_at_ms", 0L).forGetter(s -> s.eliminateAtMillis),
        Codec.BOOL.optionalFieldOf("teleport_claimed", false).forGetter(s -> s.teleportClaimed)
    ).apply(instance, PlayerNametag::new));

    public boolean enabled;
    public Component text;
    public int tears;
    public boolean eliminated;
    public Identifier ruleId;
    public long nextRecoveryAtMillis;
    public long eliminateAtMillis;
    public boolean teleportClaimed;

    public PlayerNametag(boolean enabled, Component text, int tears, boolean eliminated, Identifier ruleId,
                         long nextRecoveryAtMillis, long eliminateAtMillis, boolean teleportClaimed) {
        this.enabled = enabled;
        this.text = text;
        this.tears = tears;
        this.eliminated = eliminated;
        this.ruleId = ruleId;
        this.nextRecoveryAtMillis = nextRecoveryAtMillis;
        this.eliminateAtMillis = eliminateAtMillis;
        this.teleportClaimed = teleportClaimed;
    }

    public static PlayerNametag create(Component text) {
        return new PlayerNametag(true, text.copy(), 0, false,
            Identifier.fromNamespaceAndPath("teartag", "any_participant"), 0L, 0L, false);
    }

    public NametagView view(java.util.UUID id) {
        return new NametagView(id, enabled, text.copy(), tears, eliminated, ruleId);
    }
}
