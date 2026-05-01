package dev.voxelgame.common.loot;

import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.structure.StructureMarker;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootTableTest {
    @Test
    void lootRollsAreDeterministicForSameContext() {
        LootTable table = LootTables.createDefaultRegistry().requireByKey("voxel:campsite_crate");
        LootContext context = new LootContext(1234L, Structures.campsite().key(), "voxel:campsite_crate", 8, 72, 10);

        assertEquals(table.roll(Items.createDefaultRegistry(), context), table.roll(Items.createDefaultRegistry(), context));
        assertFalse(table.roll(Items.createDefaultRegistry(), context).isEmpty());
    }

    @Test
    void defaultLootTablesCoverStructureLootMarkers() {
        LootTableRegistry tables = LootTables.createDefaultRegistry();

        Stream.of(
                        Structures.smallRuin(),
                        Structures.campsite(),
                        Structures.compactVillage()
                )
                .flatMap(template -> template.lootMarkers().stream())
                .map(StructureMarker::key)
                .forEach(tables::requireByKey);
    }

    @Test
    void defaultLootEntriesReferenceRegisteredItems() {
        var items = Items.createDefaultRegistry();

        for (LootTable table : LootTables.createDefaultRegistry().values()) {
            assertFalse(table.entries().isEmpty(), table.key());
            for (LootEntry entry : table.entries()) {
                assertFalse(items.findByKey(entry.itemKey()).isEmpty(), table.key() + " drops missing item " + entry.itemKey());
                assertTrue(entry.minCount() >= 1, table.key() + " has invalid min count");
                assertTrue(entry.maxCount() >= entry.minCount(), table.key() + " has invalid max count");
                assertTrue(entry.chance() > 0.0, table.key() + " has inert loot entry");
            }
        }
    }

    @Test
    void ruinCratesSeedAncientFragmentProgression() {
        LootTable table = LootTables.createDefaultRegistry().requireByKey("voxel:ruin_crate");

        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:ancient_fragment")));
        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:ancient_lantern")));
        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:ruin_key")));
    }

    @Test
    void rareRuinCratesSeedLateGameKeysSealsAndIron() {
        LootTable table = LootTables.createDefaultRegistry().requireByKey("voxel:ruin_rare_crate");

        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:iron_ingot")));
        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:ruin_key")));
        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:ruin_seal")));
        assertTrue(table.entries().stream().anyMatch(entry -> entry.itemKey().equals("voxel:lost_charm")));
    }

    @Test
    void duplicateLootTablesAreRejected() {
        LootTableRegistry registry = new LootTableRegistry();
        LootTable table = LootTables.createDefaultRegistry().requireByKey("voxel:ruin_crate");

        registry.register(table);

        assertThrows(IllegalArgumentException.class, () -> registry.register(table));
    }
}
