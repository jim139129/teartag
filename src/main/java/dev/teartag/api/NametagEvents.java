package dev.teartag.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;

public final class NametagEvents {
    public static final List<TearListener> BEFORE_TEAR = new CopyOnWriteArrayList<>();
    public static final List<TearListener> AFTER_TEAR = new CopyOnWriteArrayList<>();
    public static final List<PlayerListener> ENABLED = new CopyOnWriteArrayList<>();
    public static final List<PlayerListener> DISABLED = new CopyOnWriteArrayList<>();
    public static final List<PlayerListener> RECOVERED = new CopyOnWriteArrayList<>();
    public static final List<TearListener> ELIMINATED = new CopyOnWriteArrayList<>();
    public static final List<DamageListener> PARTICIPANT_DAMAGE = new CopyOnWriteArrayList<>();

    private NametagEvents() {
    }

    @FunctionalInterface
    public interface PlayerListener {
        void accept(ServerPlayer player, NametagView state);
    }

    @FunctionalInterface
    public interface TearListener {
        boolean accept(ServerPlayer attacker, ServerPlayer target, NametagView state, boolean wallAttempt);
    }

    @FunctionalInterface
    public interface DamageListener {
        void accept(ServerPlayer attacker, ServerPlayer target, float amount, DamageDecision decision);
    }

    /** Mutable result passed to Java integrations before participant damage is applied. */
    public static final class DamageDecision {
        private boolean allowed;

        public DamageDecision(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public void allow() {
            allowed = true;
        }

        public void deny() {
            allowed = false;
        }
    }
}
