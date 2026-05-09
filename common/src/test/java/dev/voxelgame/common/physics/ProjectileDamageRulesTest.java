package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileDamageRulesTest {
    @Test
    void playerOwnedProjectilesRespectProtectedCreatureRules() {
        ProjectileState arrow = arrow(UUID.randomUUID());
        EntitySnapshot sheep = new EntitySnapshot(1L, "voxel:cozy_sheep", null, 1.0, 80.0, 0.0, 0.0f, 0.0f, 10);

        ProjectileDamageRules.HitDecision decision = ProjectileDamageRules.canHit(arrow, sheep);

        assertEquals(ProjectileDamageRules.Reason.PROTECTED_CREATURE, decision.reason());
    }

    @Test
    void playerOwnedProjectilesCanStillHitRareDangerCreatures() {
        ProjectileState arrow = arrow(UUID.randomUUID());
        EntitySnapshot crawler = new EntitySnapshot(1L, "voxel:dune_crawler", null, 1.0, 80.0, 0.0, 0.0f, 0.0f, 10);

        assertTrue(ProjectileDamageRules.canHit(arrow, crawler).accepted());
    }

    @Test
    void unownedProjectilesKeepEnvironmentalTrapEscapeHatch() {
        ProjectileState arrow = arrow(null);
        EntitySnapshot sheep = new EntitySnapshot(1L, "voxel:cozy_sheep", null, 1.0, 80.0, 0.0, 0.0f, 0.0f, 10);

        assertTrue(ProjectileDamageRules.canHit(arrow, sheep).accepted());
    }

    private static ProjectileState arrow(UUID owner) {
        return new ProjectileState(99L, owner, "voxel:arrow_projectile", 0.0, 80.45, 0.0, 1.0, 0.0, 0.0, 0);
    }
}
