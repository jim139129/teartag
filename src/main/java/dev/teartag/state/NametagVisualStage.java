package dev.teartag.state;

public final class NametagVisualStage {
    public static final int TORN = 5;

    private NametagVisualStage() {
    }

    public static int fromProgress(int tears, int requiredTears, boolean eliminated) {
        if (eliminated) return TORN;
        int required = Math.max(1, requiredTears);
        int progress = Math.max(0, tears);
        return Math.min(4, progress * 5 / required);
    }
}
