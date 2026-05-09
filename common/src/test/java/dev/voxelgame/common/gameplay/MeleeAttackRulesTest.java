package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.Items;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeleeAttackRulesTest {
    @Test
    void weaponProfilesUseWeaponStatsForDamageCooldownAndKnockback() {
        var items = Items.createDefaultRegistry();
        var stoneSword = items.requireByKey("voxel:stone_sword");
        var titanSword = items.requireByKey("voxel:titan_sword");

        MeleeAttackRules.AttackProfile stone = MeleeAttackRules.profileFor(new ItemStack(stoneSword.id(), 1), items);
        MeleeAttackRules.AttackProfile titan = MeleeAttackRules.profileFor(new ItemStack(titanSword.id(), 1), items);

        assertEquals(5, stone.damage());
        assertTrue(stone.range() > 2.8);
        assertTrue(titan.damage() > stone.damage());
        assertTrue(titan.knockbackStrength() > stone.knockbackStrength());
        assertEquals(1, titan.durabilityDamage());
    }

    @Test
    void toolsAndEmptyHandsRemainValidFallbackAttacks() {
        var items = Items.createDefaultRegistry();
        var pickaxe = items.requireByKey("voxel:stone_pickaxe");

        MeleeAttackRules.AttackProfile hand = MeleeAttackRules.profileFor(ItemStack.EMPTY, items);
        MeleeAttackRules.AttackProfile tool = MeleeAttackRules.profileFor(new ItemStack(pickaxe.id(), 1), items);

        assertEquals(1, hand.damage());
        assertEquals(0, hand.durabilityDamage());
        assertTrue(tool.damage() > hand.damage());
        assertEquals(1, tool.durabilityDamage());
    }

    @Test
    void targetRulesRejectNonDamageableAndOwnedTargets() {
        UUID playerId = UUID.randomUUID();

        assertFalse(MeleeAttackRules.canAttack(playerId, new EntitySnapshot(1L, "voxel:player", playerId, 0.0, 80.0, 0.0, 0.0f, 0.0f, 20)).accepted());
        assertFalse(MeleeAttackRules.canAttack(playerId, new EntitySnapshot(2L, ItemDropType.typeKey("voxel:moss_clump"), null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 1)).accepted());
        assertFalse(MeleeAttackRules.canAttack(playerId, new EntitySnapshot(3L, "voxel:arrow_projectile", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE, 0.0, 0.0, 0.0)).accepted());
        assertEquals(
                MeleeAttackRules.RejectionReason.PROTECTED_CREATURE,
                MeleeAttackRules.canAttack(playerId, new EntitySnapshot(4L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)).reason()
        );
        assertTrue(MeleeAttackRules.canAttack(playerId, new EntitySnapshot(4L, "voxel:little_boar", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)).accepted());
    }
}
