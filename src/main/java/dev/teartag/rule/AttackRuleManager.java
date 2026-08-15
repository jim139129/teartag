package dev.teartag.rule;

import dev.teartag.NameTagMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class AttackRuleManager extends SimpleJsonResourceReloadListener<AttackRule> {
    public static final Identifier DEFAULT_ID = Identifier.fromNamespaceAndPath("teartag", "any_participant");
    public static final AttackRule DEFAULT = new AttackRule(false, java.util.List.of(
        new AttackRule.Clause(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), AttackRule.TeamRelation.ANY)
    ));
    private static final AttackRule DENY = new AttackRule(false, java.util.List.of());
    private static volatile Map<Identifier, AttackRule> rules = Map.of(DEFAULT_ID, DEFAULT);

    public AttackRuleManager() {
        super(AttackRule.CODEC, FileToIdConverter.json("teartag_attack_rules"));
    }

    public static AttackRule get(Identifier id) {
        return rules.getOrDefault(id, DENY);
    }

    public static boolean exists(Identifier id) {
        return DEFAULT_ID.equals(id) || rules.containsKey(id);
    }

    @Override
    protected void apply(Map<Identifier, AttackRule> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, AttackRule> loaded = new ConcurrentHashMap<>(prepared);
        loaded.put(DEFAULT_ID, DEFAULT);
        rules = Map.copyOf(loaded);
        NameTagMod.LOGGER.info("Loaded {} teartag attack rules", rules.size());
    }
}
