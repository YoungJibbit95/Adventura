package dev.voxelgame.common.gameplay;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityDropsTest {
    @Test
    void mossSnailAlwaysDropsMossClump() {
        assertEquals(
                List.of(new EntityDrops.Drop("voxel:moss_clump", 1)),
                EntityDrops.dropsFor("voxel:moss_snail", 1L)
        );
    }

    @Test
    void mossSnailRareSlimeDropIsDeterministic() {
        List<EntityDrops.Drop> drops = EntityDrops.dropsFor("voxel:moss_snail", 3L);

        assertEquals(List.of(
                new EntityDrops.Drop("voxel:moss_clump", 1),
                new EntityDrops.Drop("voxel:slime_drop", 1)
        ), drops);
        assertEquals(drops, EntityDrops.dropsFor("voxel:moss_snail", 3L));
    }

    @Test
    void littleBoarDropsLeatherAndOptionalForageDeterministically() {
        List<EntityDrops.Drop> drops = EntityDrops.dropsFor("voxel:little_boar", 11L);

        assertEquals("voxel:leather_strip", drops.getFirst().itemKey());
        assertEquals(1, drops.getFirst().count());
        assertEquals(drops, EntityDrops.dropsFor("voxel:little_boar", 11L));
    }

    @Test
    void duneCrawlerDropsLeatherAndOptionalOreDeterministically() {
        List<EntityDrops.Drop> drops = EntityDrops.dropsFor("voxel:dune_crawler", 17L);

        assertEquals("voxel:leather_strip", drops.getFirst().itemKey());
        assertEquals(1, drops.getFirst().count());
        assertEquals(drops, EntityDrops.dropsFor("voxel:dune_crawler", 17L));
    }

    @Test
    void unrelatedAmbientEntitiesDoNotDropItemsYet() {
        assertEquals(List.of(), EntityDrops.dropsFor("voxel:forest_bunny", 3L));
    }
}
