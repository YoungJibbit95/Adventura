package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponItemRulesTest {
    @Test
    void swordStatsScaleAcrossMaterialProgression() {
        var items = Items.createDefaultRegistry();

        var stone = WeaponItemRules.statsFor(items.requireByKey("voxel:stone_sword")).orElseThrow();
        var iron = WeaponItemRules.statsFor(items.requireByKey("voxel:iron_sword")).orElseThrow();
        var platin = WeaponItemRules.statsFor(items.requireByKey("voxel:platin_sword")).orElseThrow();
        var sapphire = WeaponItemRules.statsFor(items.requireByKey("voxel:sapphire_sword")).orElseThrow();
        var titan = WeaponItemRules.statsFor(items.requireByKey("voxel:titan_sword")).orElseThrow();

        assertTrue(iron.damage() > stone.damage());
        assertTrue(platin.damage() >= iron.damage());
        assertTrue(titan.damage() > platin.damage());
        assertTrue(sapphire.crystalline());
        assertFalse(stone.crystalline());
    }

    @Test
    void nonWeaponsDoNotExposeWeaponStats() {
        var items = Items.createDefaultRegistry();

        assertFalse(WeaponItemRules.isWeapon(items.requireByKey("voxel:apple")));
        assertFalse(WeaponItemRules.statsFor(items.requireByKey("voxel:stone_pickaxe")).isPresent());
    }
}
