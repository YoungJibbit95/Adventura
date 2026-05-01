package dev.voxelgame.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageModelTest {
    @Test
    void damageSourceCarriesStableTypeAndAttacker() {
        UUID attacker = UUID.randomUUID();

        DamageSource source = DamageSource.projectile(attacker, -10L, "voxel:arrow_projectile");

        assertEquals(DamageSource.Type.PROJECTILE, source.type());
        assertEquals(attacker, source.attackerPlayerId());
        assertEquals(-10L, source.sourceEntityId());
        assertEquals("voxel:arrow_projectile", source.causeKey());
    }

    @Test
    void blankDamageCauseFallsBackToTypeKey() {
        DamageSource source = DamageSource.environment(" ");

        assertEquals("environment", source.causeKey());
    }

    @Test
    void rejectedDamageResultHasNoSnapshotOrKnockback() {
        DamageResult result = DamageResult.rejected(12L, DamageResult.RejectionReason.INVULNERABLE);

        assertFalse(result.accepted());
        assertEquals(0, result.amount());
        assertTrue(result.snapshot().isEmpty());
        assertEquals(0.0, result.knockbackX(), 0.001);
    }

    @Test
    void acceptedDamageResultRejectsNonFiniteKnockback() {
        EntitySnapshot target = new EntitySnapshot(12L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 1);

        assertThrows(IllegalArgumentException.class, () ->
                DamageResult.accepted(target.entityId(), 1, false, target, Double.NaN, 0.0, 0.0));
    }
}
