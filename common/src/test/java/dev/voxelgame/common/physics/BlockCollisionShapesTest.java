package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntityBounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockCollisionShapesTest {
    @Test
    void pathShapeOnlyCollidesWithLowPlayerOverlap() {
        BlockCollisionShape path = BlockCollisionShapes.collisionShape(Blocks.MOSSY_PATH);
        PlayerBounds bounds = PlayerBounds.DEFAULT;

        assertTrue(path.intersectsPlayer(bounds, 8.5, 65.68, 8.5, 8, 64, 8));
        assertFalse(path.intersectsPlayer(bounds, 8.5, 65.82, 8.5, 8, 64, 8));
    }

    @Test
    void fenceShapeBlocksCenterButNotWholeCell() {
        BlockCollisionShape fence = BlockCollisionShapes.collisionShape(Blocks.GARDEN_FENCE);
        EntityBounds bounds = EntityBounds.forType("voxel:forest_bunny");

        assertTrue(fence.intersectsEntity(bounds, 8.5, 64.0, 8.5, 8, 64, 8));
        assertFalse(fence.intersectsEntity(bounds, 8.12, 64.0, 8.12, 8, 64, 8));
    }

    @Test
    void decorationPlacementUsesSmallShapeInsteadOfFullCube() {
        BlockCollisionShape shape = BlockCollisionShapes.placementShape(Blocks.CAMPFIRE);

        assertTrue(shape.intersectsPlayer(PlayerBounds.DEFAULT, 8.5, 65.62, 8.5, 8, 64, 8));
        assertFalse(shape.intersectsPlayer(PlayerBounds.DEFAULT, 7.90, 65.62, 7.90, 8, 64, 8));
    }
}
