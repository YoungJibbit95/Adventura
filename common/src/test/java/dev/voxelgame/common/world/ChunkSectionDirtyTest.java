package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSectionDirtyTest {
    @Test
    void blockChangesMarkGeometryLightAndFluidIndependently() {
        ChunkSection section = new ChunkSection(-1);

        section.setBlockId(1, 2, 3, Blocks.WATER);

        assertTrue(section.isDirty(ChunkSection.DirtyAspect.GEOMETRY));
        assertTrue(section.isDirty(ChunkSection.DirtyAspect.LIGHT));
        assertTrue(section.isDirty(ChunkSection.DirtyAspect.FLUID));
        assertFalse(section.isDirty(ChunkSection.DirtyAspect.BLOCK_ENTITY));
    }

    @Test
    void lightAndBlockEntityDirtyBitsDoNotForceGeometryDirty() {
        ChunkSection section = new ChunkSection(4);

        section.setSkyLight(1, 2, 3, 15);
        section.markDirty(ChunkSection.DirtyAspect.BLOCK_ENTITY);

        assertFalse(section.isDirty(ChunkSection.DirtyAspect.GEOMETRY));
        assertTrue(section.isDirty(ChunkSection.DirtyAspect.LIGHT));
        assertFalse(section.isDirty(ChunkSection.DirtyAspect.FLUID));
        assertTrue(section.isDirty(ChunkSection.DirtyAspect.BLOCK_ENTITY));

        section.clearDirty(ChunkSection.DirtyAspect.LIGHT);

        assertFalse(section.isDirty(ChunkSection.DirtyAspect.LIGHT));
        assertTrue(section.isDirty(ChunkSection.DirtyAspect.BLOCK_ENTITY));
    }
}
