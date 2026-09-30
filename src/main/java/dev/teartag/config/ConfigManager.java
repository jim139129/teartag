package dev.teartag.config;

import dev.teartag.NameTagMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public final class ConfigManager {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("teartag.toml");
    private static volatile NameTagConfig current = NameTagConfig.defaults();

    private ConfigManager() {
    }

    public static NameTagConfig get() {
        return current;
    }

    public static synchronized List<String> reload() {
        try {
            if (Files.notExists(PATH)) {
                writeDefaults();
                current = NameTagConfig.defaults();
                return List.of();
            }
            TomlParseResult doc = Toml.parse(PATH);
            if (doc.hasErrors()) {
                return doc.errors().stream().map(Object::toString).toList();
            }
            NameTagConfig d = NameTagConfig.defaults();
            double[] renderSize = NameTagConfig.migrateLegacyRenderSize(
                number(doc, "render.width", d.tagWidth()), number(doc, "render.height", d.tagHeight()), d.tagWidth(), d.tagHeight());
            double backOffset = NameTagConfig.migrateLegacyBackOffset(number(doc, "render.back_offset", d.tagBackOffset()), d.tagBackOffset());
            double verticalOffset = NameTagConfig.migrateLegacyVerticalOffset(number(doc, "render.vertical_offset", d.tagVerticalOffset()), d.tagVerticalOffset());
            float textScale = NameTagConfig.migrateLegacyTextScale((float) number(doc, "render.text_scale", d.textScale()), d.textScale());
            NameTagConfig loaded = new NameTagConfig(
                number(doc, "combat.attack_distance", d.attackDistance()),
                integer(doc, "combat.attack_cooldown_ticks", d.attackCooldownTicks()),
                integer(doc, "combat.max_attempt_packets_per_second", d.maxAttemptPacketsPerSecond()),
                integer(doc, "wall.attempt_cooldown_ticks", d.wallAttemptCooldownTicks()),
                number(doc, "wall.detection_distance", d.wallDetectionDistance()),
                number(doc, "wall.success_chance", d.wallSuccessChance()),
                integer(doc, "progress.required_tears", d.requiredTears()),
                integer(doc, "progress.first_recovery_delay_ticks", d.firstRecoveryDelayTicks()),
                integer(doc, "progress.recovery_interval_ticks", d.recoveryIntervalTicks()),
                integer(doc, "elimination.delay_ticks", d.eliminationDelayTicks()),
                string(doc, "elimination.dimension", d.eliminationDimension()),
                number(doc, "elimination.x", d.eliminationX()),
                number(doc, "elimination.y", d.eliminationY()),
                number(doc, "elimination.z", d.eliminationZ()),
                (float) number(doc, "elimination.yaw", d.eliminationYaw()),
                (float) number(doc, "elimination.pitch", d.eliminationPitch()),
                bool(doc, "elimination.default_teleport", d.defaultTeleport()),
                bool(doc, "feedback.sounds", d.soundsEnabled()),
                bool(doc, "feedback.particles", d.particlesEnabled()),
                bool(doc, "feedback.titles", d.titlesEnabled()),
                integer(doc, "feedback.torn_animation_ticks", d.tornAnimationTicks()),
                bool(doc, "ui.actionbar", d.actionbarEnabled()),
                integer(doc, "ui.attacker_actionbar_ticks", d.attackerActionbarTicks()),
                integer(doc, "commands.permission_level", d.commandPermissionLevel()),
                integer(doc, "text.max_code_points", d.maxTextCodePoints()),
                integer(doc, "text.max_lines", d.maxTextLines()),
                renderSize[0],
                renderSize[1],
                backOffset,
                verticalOffset,
                textScale,
                color(doc, "render.paper_color", d.paperColor()),
                color(doc, "render.border_color", d.borderColor()),
                color(doc, "render.text_color", d.textColor()),
                bool(doc, "trinkets.auto_equip_on_enable", d.trinketsAutoEquipOnEnable()),
                bool(doc, "trinkets.enable_on_equip", d.trinketsEnableOnEquip()),
                bool(doc, "trinkets.disable_on_unequip", d.trinketsDisableOnUnequip()),
                bool(doc, "debug.logging", d.debugLogging())
            );
            List<String> errors = loaded.validate();
            if (errors.isEmpty()) {
                current = loaded;
            }
            return errors;
        } catch (Exception e) {
            NameTagMod.LOGGER.error("Failed to reload config", e);
            return List.of(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static void writeDefaults() throws IOException {
        Files.createDirectories(PATH.getParent());
        Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        Files.writeString(temp, DEFAULT_FILE, StandardCharsets.UTF_8);
        Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static double number(TomlParseResult doc, String key, double fallback) {
        Double value = doc.getDouble(key);
        return value == null ? fallback : value;
    }

    private static int integer(TomlParseResult doc, String key, int fallback) {
        Long value = doc.getLong(key);
        return value == null ? fallback : Math.toIntExact(value);
    }

    private static boolean bool(TomlParseResult doc, String key, boolean fallback) {
        Boolean value = doc.getBoolean(key);
        return value == null ? fallback : value;
    }

    private static String string(TomlParseResult doc, String key, String fallback) {
        String value = doc.getString(key);
        return value == null ? fallback : value;
    }

    private static int color(TomlParseResult doc, String key, int fallback) {
        String value = doc.getString(key);
        if (value == null) return fallback;
        String hex = value.startsWith("#") ? value.substring(1) : value;
        long parsed = Long.parseUnsignedLong(hex, 16);
        return hex.length() <= 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
    }

    private static final String DEFAULT_FILE = """
        # TearTag server-authoritative configuration.
        # Distances and dimensions are blocks. Time values are ticks (20 ticks = 1 second).
        # Run /teartag config reload after editing. Invalid values leave the live config unchanged.
        [combat]
        attack_distance = 3.0          # Maximum eye-to-plate ray distance; range 0.5..8.0
        attack_cooldown_ticks = 2      # Attacker cooldown after any successful tear; range 0..200
        max_attempt_packets_per_second = 20 # Per-player packet limit before geometry checks; range 1..200

        [wall]
        attempt_cooldown_ticks = 10    # Attacker cooldown after a failed wall roll; range 0..200
        detection_distance = 0.15      # Probe depth behind the plate; range 0.01..1.0
        success_chance = 0.25          # Front-through-body success probability; range 0.0..1.0

        [progress]
        required_tears = 5             # Successful tears needed for elimination; range 1..100
        first_recovery_delay_ticks = 20 # Quiet time after a tear before recovery; range 1..72000
        recovery_interval_ticks = 20   # Time between recovered layers; range 1..72000

        [elimination]
        delay_ticks = 200              # Particle phase before completion; range 0..72000
        dimension = "minecraft:overworld" # Default destination dimension ID
        x = 0.5                         # Default destination X
        y = 64.0                        # Default destination Y
        z = 0.5                         # Default destination Z
        yaw = 0.0                       # Default destination horizontal rotation
        pitch = 0.0                     # Default destination vertical rotation
        default_teleport = true         # Teleport and set spectator unless datapack claims the event

        [feedback]
        sounds = true                   # Play success, failure, target-hit, and elimination sounds
        particles = true                # Emit elimination server particles
        titles = true                   # Show the eliminated player a title
        torn_animation_ticks = 30       # Client flying-plate animation; 0 disables, range 0..1200

        [ui]
        actionbar = true                # Continuously show participant progress
        attacker_actionbar_ticks = 100  # Show last target instead of self for this long; range 0..72000

        [commands]
        permission_level = 2            # Vanilla command level required for administration; range 0..4

        [text]
        max_code_points = 64            # Maximum Unicode code points accepted; range 1..1024
        max_lines = 2                   # Maximum wrapped lines rendered; range 1..8

        [render]
        width = 0.60                    # Rendered plate width; range 0.1..4.0
        height = 0.40                   # Rendered plate height; range 0.1..4.0
        back_offset = 0.19              # Plate center behind player origin; range 0.0..2.0
        vertical_offset = 1.16          # Plate center above player feet; range 0.0..3.0
        text_scale = 0.015              # Text pixel size in world blocks; range 0.001..0.1
        paper_color = "#FFFFFFFF"       # ARGB or RGB hexadecimal color
        border_color = "#FF585858"      # ARGB or RGB hexadecimal color
        text_color = "#FF105BD7"        # Default text color; styled components can override it

        [trinkets]
        auto_equip_on_enable = true     # /teartag enable fills the dedicated slot when Trinkets Updated is installed
        enable_on_equip = true          # Equipping teartag:nametag enables the player's nametag state
        disable_on_unequip = true       # Removing teartag:nametag disables the player's nametag state

        [debug]
        logging = false                 # Reserved for verbose diagnostics
        """;
}
