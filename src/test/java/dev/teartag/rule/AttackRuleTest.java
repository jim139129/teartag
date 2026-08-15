package dev.teartag.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttackRuleTest {
    @Test
    void legacyRulesDefaultToDeniedParticipantDamage() {
        JsonObject json = new JsonObject();
        json.addProperty("allow_self", false);
        json.add("any_of", new JsonArray());

        AttackRule rule = AttackRule.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

        assertEquals(AttackRule.DamagePolicy.DENY, rule.participantDamage());
        assertTrue(rule.damageRules().isEmpty());
    }

    @Test
    void decodesDirectionalDamageRules() {
        JsonObject damageRule = new JsonObject();
        JsonArray attackerTags = new JsonArray();
        attackerTags.add("game.team_a");
        damageRule.add("attacker_requires", attackerTags);
        damageRule.addProperty("policy", "allow");

        JsonArray damageRules = new JsonArray();
        damageRules.add(damageRule);
        JsonObject json = new JsonObject();
        json.add("damage_rules", damageRules);

        AttackRule rule = AttackRule.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

        assertEquals(1, rule.damageRules().size());
        assertEquals(List.of("game.team_a"), rule.damageRules().getFirst().attackerRequires());
        assertEquals(AttackRule.DamagePolicy.ALLOW, rule.damageRules().getFirst().policy());
    }
}
