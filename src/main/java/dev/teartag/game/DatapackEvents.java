package dev.teartag.game;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public final class DatapackEvents {
    private static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath("teartag", "event");
    private static final ThreadLocal<UUID> ACTIVE_ELIMINATION = new ThreadLocal<>();

    private DatapackEvents() {
    }

    public static void fire(String name, ServerPlayer subject, @Nullable ServerPlayer attacker, boolean wallAttempt) {
        fire(name, subject, attacker, wallAttempt, null);
    }

    public static void fireParticipantDamage(ServerPlayer target, ServerPlayer attacker, float amount, boolean allowed) {
        fire("on_participant_damage", target, attacker, false, event -> {
            event.putDouble("damage_amount", amount);
            event.putBoolean("damage_allowed", allowed);
        });
    }

    private static void fire(String name, ServerPlayer subject, @Nullable ServerPlayer attacker, boolean wallAttempt,
                              @Nullable Consumer<CompoundTag> eventExtras) {
        MinecraftServer server = subject.level().getServer();
        CompoundTag previousStorage = server.getCommandStorage().get(STORAGE_ID).copy();
        boolean targetTagExisted = subject.entityTags().contains("teartag.event_target");
        boolean attackerTagExisted = attacker != null && attacker.entityTags().contains("teartag.event_attacker");
        UUID previousElimination = ACTIVE_ELIMINATION.get();
        CompoundTag event = new CompoundTag();
        event.putString("event", name);
        event.putString("target_uuid", subject.getUUID().toString());
        event.putString("target_name", subject.getGameProfile().name());
        event.putBoolean("wall_attempt", wallAttempt);
        var state = dev.teartag.state.NametagSavedData.get(server).get(subject.getUUID());
        if (state != null) {
            event.putInt("tear_count", state.tears);
            event.putInt("required_tears", dev.teartag.config.ConfigManager.get().requiredTears());
            event.putString("rule", state.ruleId.toString());
            net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, state.text).result()
                .ifPresent(tag -> event.put("text", tag));
        }
        if (eventExtras != null) eventExtras.accept(event);
        if (attacker != null) {
            event.putString("attacker_uuid", attacker.getUUID().toString());
            event.putString("attacker_name", attacker.getGameProfile().name());
            attacker.addTag("teartag.event_attacker");
        }
        subject.addTag("teartag.event_target");
        server.getCommandStorage().set(STORAGE_ID, event);
        boolean elimination = "on_eliminate".equals(name);
        if (elimination) ACTIVE_ELIMINATION.set(subject.getUUID());
        try {
            CommandSourceStack source = server.createCommandSourceStack().withEntity(subject).withLevel(subject.level()).withPosition(subject.position());
            for (var function : server.getFunctions().getTag(Identifier.fromNamespaceAndPath("teartag", name))) {
                server.getFunctions().execute(function, source);
            }
        } finally {
            if (previousElimination == null) ACTIVE_ELIMINATION.remove();
            else ACTIVE_ELIMINATION.set(previousElimination);
            if (!targetTagExisted) subject.removeTag("teartag.event_target");
            if (attacker != null && !attackerTagExisted) attacker.removeTag("teartag.event_attacker");
            server.getCommandStorage().set(STORAGE_ID, previousStorage);
        }
    }

    public static boolean claim(ServerPlayer player) {
        UUID active = ACTIVE_ELIMINATION.get();
        if (active == null || !active.equals(player.getUUID())) return false;
        var state = dev.teartag.state.NametagSavedData.get(player.level().getServer()).get(active);
        if (state == null || !state.eliminated) return false;
        state.teleportClaimed = true;
        dev.teartag.state.NametagSavedData.get(player.level().getServer()).setDirty();
        return true;
    }
}
