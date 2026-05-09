package dev.voxelgame.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDamageRulesTest {
    @Test
    void appliesAmbientDamageCooldownAndKnockback() {
        EntitySnapshot target = new EntitySnapshot(42L, "voxel:moss_snail", null, 4.0, 80.0, 1.0, 0.0f, 0.0f, 6);

        EntityDamageRules.DamageResolution resolution = EntityDamageRules.resolveAmbientDamage(
                target.entityId(),
                target,
                2,
                DamageSource.playerMelee(UUID.randomUUID()),
                10.0,
                0.0,
                0.25,
                0.5,
                new EntityDamageRules.KnockbackOrigin(2.0, 80.0, 1.0)
        );

        DamageResult result = resolution.result();
        assertTrue(result.accepted());
        assertEquals(2, result.amount());
        assertEquals(4, result.updatedSnapshot().health());
        assertEquals(EntitySnapshot.STATE_FLEE, result.updatedSnapshot().stateKey());
        assertTrue(result.knockbackX() > 0.0);
        assertEquals(0.10, result.knockbackY(), 0.001);
        assertEquals(10.25, resolution.nextDamageAllowedAt(), 0.001);
    }

    @Test
    void rejectsDamageDuringInvulnerabilityWindow() {
        EntitySnapshot target = new EntitySnapshot(42L, "voxel:moss_snail", null, 4.0, 80.0, 1.0, 0.0f, 0.0f, 6);

        EntityDamageRules.DamageResolution resolution = EntityDamageRules.resolveAmbientDamage(
                target.entityId(),
                target,
                2,
                DamageSource.environment("test"),
                10.1,
                10.25,
                0.25,
                0.5,
                null
        );

        assertFalse(resolution.result().accepted());
        assertEquals(DamageResult.RejectionReason.INVULNERABLE, resolution.result().rejectionReason());
        assertEquals(10.25, resolution.nextDamageAllowedAt(), 0.001);
    }

    @Test
    void killingDamageClampsAmountAndReturnsIdleSnapshot() {
        EntitySnapshot target = new EntitySnapshot(42L, "voxel:moss_snail", null, 4.0, 80.0, 1.0, 0.0f, 0.0f, 3);

        DamageResult result = EntityDamageRules.resolveAmbientDamage(
                target.entityId(),
                target,
                12,
                DamageSource.fire(),
                10.0,
                0.0,
                0.0,
                0.5,
                null
        ).result();

        assertTrue(result.accepted());
        assertTrue(result.killed());
        assertEquals(3, result.amount());
        assertEquals(0, result.updatedSnapshot().health());
        assertEquals(EntitySnapshot.STATE_IDLE, result.updatedSnapshot().stateKey());
    }

    @Test
    void rejectsInvalidOrUnknownTargets() {
        EntitySnapshot target = new EntitySnapshot(42L, "voxel:moss_snail", null, 4.0, 80.0, 1.0, 0.0f, 0.0f, 6);

        DamageResult invalid = EntityDamageRules.resolveAmbientDamage(
                target.entityId(),
                target,
                0,
                DamageSource.unknown(),
                10.0,
                0.0,
                0.5,
                null
        ).result();
        DamageResult unknown = EntityDamageRules.resolveAmbientDamage(
                99L,
                null,
                2,
                DamageSource.unknown(),
                10.0,
                0.0,
                0.5,
                null
        ).result();

        assertEquals(DamageResult.RejectionReason.INVALID_AMOUNT, invalid.rejectionReason());
        assertEquals(DamageResult.RejectionReason.UNKNOWN_TARGET, unknown.rejectionReason());
    }

    @Test
    void rejectsPlayerDamageAgainstProtectedCozyCreatures() {
        EntitySnapshot sheep = new EntitySnapshot(42L, "voxel:cozy_sheep", null, 4.0, 80.0, 1.0, 0.0f, 0.0f, 6);

        DamageResult result = EntityDamageRules.resolveAmbientDamage(
                sheep.entityId(),
                sheep,
                2,
                DamageSource.playerMelee(UUID.randomUUID()),
                10.0,
                0.0,
                0.5,
                null
        ).result();

        assertFalse(result.accepted());
        assertEquals(DamageResult.RejectionReason.PROTECTED_CREATURE, result.rejectionReason());
    }

    @Test
    void rejectsNonFiniteKnockbackOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new EntityDamageRules.KnockbackOrigin(Double.NaN, 0.0, 0.0));
    }
}
