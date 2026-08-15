package dev.teartag.config;

import java.util.ArrayList;
import java.util.List;

public record NameTagConfig(
    double attackDistance,
    int attackCooldownTicks,
    int maxAttemptPacketsPerSecond,
    int wallAttemptCooldownTicks,
    double wallDetectionDistance,
    double wallSuccessChance,
    int requiredTears,
    int firstRecoveryDelayTicks,
    int recoveryIntervalTicks,
    int eliminationDelayTicks,
    String eliminationDimension,
    double eliminationX,
    double eliminationY,
    double eliminationZ,
    float eliminationYaw,
    float eliminationPitch,
    boolean defaultTeleport,
    boolean soundsEnabled,
    boolean particlesEnabled,
    boolean titlesEnabled,
    int tornAnimationTicks,
    boolean actionbarEnabled,
    int attackerActionbarTicks,
    int commandPermissionLevel,
    int maxTextCodePoints,
    int maxTextLines,
    double tagWidth,
    double tagHeight,
    double tagBackOffset,
    double tagVerticalOffset,
    float textScale,
    int paperColor,
    int borderColor,
    boolean trinketsAutoEquipOnEnable,
    boolean trinketsEnableOnEquip,
    boolean trinketsDisableOnUnequip,
    boolean debugLogging
) {
    public static NameTagConfig defaults() {
        return new NameTagConfig(
            3.0, 2, 20, 10, 0.15, 0.25, 5, 20, 20, 200,
            "minecraft:overworld", 0.5, 64.0, 0.5, 0.0F, 0.0F, true,
            true, true, true, 30, true, 100, 2, 64, 2,
            0.82, 0.55, 0.32, 0.95, 0.010F,
            0xFFF5F1E8, 0xFF34312C,
            true, true, true, false
        );
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        range(errors, "combat.attack_distance", attackDistance, 0.5, 8.0);
        range(errors, "combat.attack_cooldown_ticks", attackCooldownTicks, 0, 200);
        range(errors, "combat.max_attempt_packets_per_second", maxAttemptPacketsPerSecond, 1, 200);
        range(errors, "wall.attempt_cooldown_ticks", wallAttemptCooldownTicks, 0, 200);
        range(errors, "wall.detection_distance", wallDetectionDistance, 0.01, 1.0);
        range(errors, "wall.success_chance", wallSuccessChance, 0.0, 1.0);
        range(errors, "progress.required_tears", requiredTears, 1, 100);
        range(errors, "progress.first_recovery_delay_ticks", firstRecoveryDelayTicks, 1, 72000);
        range(errors, "progress.recovery_interval_ticks", recoveryIntervalTicks, 1, 72000);
        range(errors, "elimination.delay_ticks", eliminationDelayTicks, 0, 72000);
        range(errors, "feedback.torn_animation_ticks", tornAnimationTicks, 0, 1200);
        range(errors, "ui.attacker_actionbar_ticks", attackerActionbarTicks, 0, 72000);
        range(errors, "commands.permission_level", commandPermissionLevel, 0, 4);
        range(errors, "text.max_code_points", maxTextCodePoints, 1, 1024);
        range(errors, "text.max_lines", maxTextLines, 1, 8);
        range(errors, "render.width", tagWidth, 0.1, 4.0);
        range(errors, "render.height", tagHeight, 0.1, 4.0);
        range(errors, "render.back_offset", tagBackOffset, 0.0, 2.0);
        range(errors, "render.vertical_offset", tagVerticalOffset, 0.0, 3.0);
        range(errors, "render.text_scale", textScale, 0.001, 0.1);
        if (eliminationDimension == null || eliminationDimension.isBlank()) {
            errors.add("elimination.dimension must be a resource identifier");
        }
        return errors;
    }

    private static void range(List<String> errors, String key, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) {
            errors.add(key + " must be in [" + min + ", " + max + "]");
        }
    }
}
