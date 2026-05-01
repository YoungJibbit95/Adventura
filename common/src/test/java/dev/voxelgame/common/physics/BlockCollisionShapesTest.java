package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntityBounds;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void projectileRaycastUsesSamePartialShapeSource() {
        BlockCollisionShape fence = BlockCollisionShapes.collisionShape(Blocks.GARDEN_FENCE);
        EntityBounds bounds = EntityBounds.forType("voxel:forest_bunny");

        assertTrue(fence.intersectsPlayer(PlayerBounds.DEFAULT, 8.5, 65.62, 8.5, 8, 64, 8));
        assertTrue(fence.intersectsEntity(bounds, 8.5, 64.0, 8.5, 8, 64, 8));

        PartialShapeImpactResolver.ImpactResult hit = fence.raycastProjectile(
                7.0,
                64.5,
                8.5,
                9.0,
                64.5,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();

        assertEquals(8, hit.blockX());
        assertEquals(ProjectileHit.BlockFace.WEST, hit.face());
        assertEquals(-1, hit.normalX());
        assertEquals(8.36, hit.impactX(), 0.0001);
        assertTrue(hit.fraction() > 0.0 && hit.fraction() < 1.0);
    }

    @Test
    void projectileRaycastIgnoresEmptySpaceInsidePartialBlockCell() {
        BlockCollisionShape fence = BlockCollisionShapes.collisionShape(Blocks.GARDEN_FENCE);

        Optional<PartialShapeImpactResolver.ImpactResult> hit = fence.raycastProjectile(
                7.0,
                64.5,
                8.1,
                9.0,
                64.5,
                8.1,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        );

        assertTrue(hit.isEmpty());
    }

    @Test
    void projectileRaycastHitsFurniturePartialShapes() {
        PartialShapeImpactResolver.ImpactResult table = BlockCollisionShapes.collisionShape(Blocks.SMALL_TABLE).raycastProjectile(
                7.0,
                64.5,
                8.5,
                9.0,
                64.5,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();
        PartialShapeImpactResolver.ImpactResult chair = BlockCollisionShapes.collisionShape(Blocks.WOODEN_CHAIR).raycastProjectile(
                7.0,
                64.5,
                8.5,
                9.0,
                64.5,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();

        assertEquals(8.08, table.impactX(), 0.0001);
        assertEquals(8.18, chair.impactX(), 0.0001);
        assertEquals(ProjectileHit.BlockFace.WEST, table.face());
        assertEquals(ProjectileHit.BlockFace.WEST, chair.face());

        PartialShapeImpactResolver.ImpactResult reverseChair = BlockCollisionShapes.collisionShape(Blocks.WOODEN_CHAIR).raycastProjectile(
                9.0,
                64.5,
                8.5,
                7.0,
                64.5,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();
        assertEquals(8.82, reverseChair.impactX(), 0.0001);
        assertEquals(ProjectileHit.BlockFace.EAST, reverseChair.face());
    }

    @Test
    void projectileRaycastChoosesNearestBoxInMultiBoxShape() {
        BlockCollisionShape shape = BlockCollisionShape.boxes(
                new BlockCollisionShape.Box(0.70, 0.0, 0.40, 0.90, 1.0, 0.60),
                new BlockCollisionShape.Box(0.20, 0.0, 0.40, 0.30, 1.0, 0.60)
        );

        PartialShapeImpactResolver.ImpactResult hit = shape.raycastProjectile(
                8.0,
                64.5,
                8.5,
                9.0,
                64.5,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();

        assertEquals(8.20, hit.impactX(), 0.0001);
        assertEquals(ProjectileHit.BlockFace.WEST, hit.face());
    }

    @Test
    void projectileShapeCanHitSmallNonMovementDecorations() {
        assertTrue(BlockCollisionShapes.collisionShape(Blocks.CAMPFIRE).empty());

        PartialShapeImpactResolver.ImpactResult hit = BlockCollisionShapes.projectileShape(Blocks.CAMPFIRE).raycastProjectile(
                7.0,
                64.25,
                8.5,
                9.0,
                64.25,
                8.5,
                ProjectileBounds.ARROW,
                8,
                64,
                8
        ).orElseThrow();

        assertEquals(ProjectileHit.BlockFace.WEST, hit.face());
        assertEquals(8.25, hit.impactX(), 0.0001);
    }
}
