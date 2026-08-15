package dev.teartag.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public record AttackRule(
    boolean allowSelf,
    List<Clause> anyOf,
    List<DamageRule> damageRules,
    DamagePolicy participantDamage
) {
    public static final Codec<AttackRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("allow_self", false).forGetter(AttackRule::allowSelf),
        Clause.CODEC.listOf().optionalFieldOf("any_of", List.of(new Clause(List.of(), List.of(), List.of(), List.of(), TeamRelation.ANY)))
            .forGetter(AttackRule::anyOf),
        DamageRule.CODEC.listOf().optionalFieldOf("damage_rules", List.of()).forGetter(AttackRule::damageRules),
        DamagePolicy.CODEC.optionalFieldOf("participant_damage", DamagePolicy.DENY).forGetter(AttackRule::participantDamage)
    ).apply(instance, AttackRule::new));

    /** Keeps the two-argument constructor source-compatible with existing integrations. */
    public AttackRule(boolean allowSelf, List<Clause> anyOf) {
        this(allowSelf, anyOf, List.of(), DamagePolicy.DENY);
    }

    public boolean permits(ServerPlayer attacker, ServerPlayer target) {
        if (!allowSelf && attacker.getUUID().equals(target.getUUID())) return false;
        return anyOf.stream().anyMatch(clause -> clause.matches(attacker, target));
    }

    /**
     * Resolves ordinary player-to-player damage independently from tear permission.
     * Rules are checked in declaration order, so the first matching damage rule wins.
     */
    public DamagePolicy participantDamage(ServerPlayer attacker, ServerPlayer target) {
        if (!allowSelf && attacker.getUUID().equals(target.getUUID())) return DamagePolicy.DENY;
        for (DamageRule rule : damageRules) {
            if (rule.matches(attacker, target)) return rule.policy();
        }
        return participantDamage;
    }

    public record Clause(
        List<String> attackerRequires,
        List<String> attackerForbids,
        List<String> targetRequires,
        List<String> targetForbids,
        TeamRelation team
    ) {
        public static final Codec<Clause> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("attacker_requires", List.of()).forGetter(Clause::attackerRequires),
            Codec.STRING.listOf().optionalFieldOf("attacker_forbids", List.of()).forGetter(Clause::attackerForbids),
            Codec.STRING.listOf().optionalFieldOf("target_requires", List.of()).forGetter(Clause::targetRequires),
            Codec.STRING.listOf().optionalFieldOf("target_forbids", List.of()).forGetter(Clause::targetForbids),
            TeamRelation.CODEC.optionalFieldOf("team", TeamRelation.ANY).forGetter(Clause::team)
        ).apply(instance, Clause::new));

        public boolean matches(ServerPlayer attacker, ServerPlayer target) {
            if (!attacker.entityTags().containsAll(attackerRequires) || !target.entityTags().containsAll(targetRequires)) return false;
            if (attackerForbids.stream().anyMatch(attacker.entityTags()::contains)) return false;
            if (targetForbids.stream().anyMatch(target.entityTags()::contains)) return false;
            boolean same = attacker.getTeam() != null && attacker.getTeam() == target.getTeam();
            return team == TeamRelation.ANY || (team == TeamRelation.SAME && same) || (team == TeamRelation.DIFFERENT && !same);
        }
    }

    /** A conditional override for ordinary damage between active participants. */
    public record DamageRule(
        List<String> attackerRequires,
        List<String> attackerForbids,
        List<String> targetRequires,
        List<String> targetForbids,
        TeamRelation team,
        DamagePolicy policy
    ) {
        public static final Codec<DamageRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("attacker_requires", List.of()).forGetter(DamageRule::attackerRequires),
            Codec.STRING.listOf().optionalFieldOf("attacker_forbids", List.of()).forGetter(DamageRule::attackerForbids),
            Codec.STRING.listOf().optionalFieldOf("target_requires", List.of()).forGetter(DamageRule::targetRequires),
            Codec.STRING.listOf().optionalFieldOf("target_forbids", List.of()).forGetter(DamageRule::targetForbids),
            TeamRelation.CODEC.optionalFieldOf("team", TeamRelation.ANY).forGetter(DamageRule::team),
            DamagePolicy.CODEC.optionalFieldOf("policy", DamagePolicy.DENY).forGetter(DamageRule::policy)
        ).apply(instance, DamageRule::new));

        public boolean matches(ServerPlayer attacker, ServerPlayer target) {
            if (!attacker.entityTags().containsAll(attackerRequires) || !target.entityTags().containsAll(targetRequires)) return false;
            if (attackerForbids.stream().anyMatch(attacker.entityTags()::contains)) return false;
            if (targetForbids.stream().anyMatch(target.entityTags()::contains)) return false;
            boolean same = attacker.getTeam() != null && attacker.getTeam() == target.getTeam();
            return team == TeamRelation.ANY || (team == TeamRelation.SAME && same) || (team == TeamRelation.DIFFERENT && !same);
        }
    }

    public enum DamagePolicy {
        DENY, ALLOW;

        public static final Codec<DamagePolicy> CODEC = Codec.STRING.xmap(
            value -> valueOf(value.toUpperCase()), value -> value.name().toLowerCase()
        );
    }

    public enum TeamRelation {
        ANY, SAME, DIFFERENT;
        public static final Codec<TeamRelation> CODEC = Codec.STRING.xmap(value -> valueOf(value.toUpperCase()), value -> value.name().toLowerCase());
    }
}
