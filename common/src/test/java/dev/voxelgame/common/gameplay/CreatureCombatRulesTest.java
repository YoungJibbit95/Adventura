package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureCombatRulesTest {
    @Test
    void protectsCozyComfortAndHintCreaturesFromPlayerDamage() {
        assertTrue(CreatureCombatRules.blocksPlayerDamage(creature("voxel:cozy_sheep")));
        assertTrue(CreatureCombatRules.blocksPlayerDamage(creature("voxel:forest_bunny")));
        assertTrue(CreatureCombatRules.blocksPlayerDamage(creature("voxel:firefly_swarm")));
        assertTrue(CreatureCombatRules.blocksPlayerDamage(creature("voxel:meadow_grazer")));
    }

    @Test
    void allowsExplicitResourceOrRareDangerCreatures() {
        assertFalse(CreatureCombatRules.blocksPlayerDamage(creature("voxel:moss_snail")));
        assertFalse(CreatureCombatRules.blocksPlayerDamage(creature("voxel:little_boar")));
        assertFalse(CreatureCombatRules.blocksPlayerDamage(creature("voxel:dune_crawler")));
    }

    @Test
    void leavesUnknownEntitiesToNormalDamageRules() {
        assertFalse(CreatureCombatRules.blocksPlayerDamage(creature("voxel:unknown_test_entity")));
    }

    private static EntitySnapshot creature(String typeKey) {
        return new EntitySnapshot(1L, typeKey, null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
    }
}
