package dev.voxelgame.common.loot;

import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.structure.StructureMarker;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void duplicateLootTablesAreRejected() {
        LootTableRegistry registry = new LootTableRegistry();
        LootTable table = LootTables.createDefaultRegistry().requireByKey("voxel:ruin_crate");

        registry.register(table);

        assertThrows(IllegalArgumentException.class, () -> registry.register(table));
    }
}
