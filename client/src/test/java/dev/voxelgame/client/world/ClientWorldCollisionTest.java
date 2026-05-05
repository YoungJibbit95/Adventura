package dev.voxelgame.client.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.BlockSurfacePhysics;
import dev.voxelgame.common.physics.PlayerWaterState;
import org.joml.Vector3f;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldCollisionTest {
    @Test
    void unloadedChunksBlockPlayerCollisionQueries() {
        ClientWorld world = new ClientWorld(123L);

        assertTrue(world.collidesPlayer(8.5, 80.0, 8.5));
    }

    @Test
    void loadedAirDoesNotBlockPlayerCollisionQueries() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.AIR));

        assertFalse(world.collidesPlayer(8.5, 80.0, 8.5));
    }

    @Test
    void physicsLoadingStatusReportsMissingCollisionChunks() {
        ClientWorld world = new ClientWorld(123L);

        ClientWorld.PhysicsLoadingStatus missing = world.physicsLoadingStatus(new Vector3f(8.5f, 80.0f, 8.5f));
        assertTrue(missing.blocked());
        assertEquals(0, missing.loadedChunks());
        assertEquals(1, missing.requiredChunks());

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.AIR));
        world.collidesPlayer(8.5, 80.0, 8.5);

        ClientWorld.PhysicsLoadingStatus loaded = world.physicsLoadingStatus(new Vector3f(8.5f, 80.0f, 8.5f));
        assertFalse(loaded.blocked());
        assertEquals(1, loaded.loadedChunks());
    }

    @Test
    @Tag("physicsRegression")
    void collisionUsesPartialBlockShapes() {
        ClientWorld world = new ClientWorld(123L);
        for (int x = 8; x <= 9; x++) {
            for (int z = 7; z <= 8; z++) {
                world.applyBlock(new GamePacket.BlockUpdate(x, 64, z, Blocks.AIR));
            }
        }
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.MOSSY_PATH));
        world.applyBlock(new GamePacket.BlockUpdate(9, 64, 8, Blocks.GARDEN_FENCE));

        assertTrue(world.collidesPlayer(8.5, 65.68, 8.5));
        assertFalse(world.collidesPlayer(8.5, 65.82, 8.5));
        assertTrue(world.collidesPlayer(9.5, 65.62, 8.5));
        assertFalse(world.collidesPlayer(8.95, 65.62, 7.65));
        assertEquals(2, world.collisionShapeBoundsAround(new Vector3f(8.5f, 65.0f, 8.5f), 2).size());
    }

    @Test
    void collisionAtChunkBoundaryUsesActualPlayerOverlap() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(15, 119, 8, Blocks.AIR));
        world.applyBlock(new GamePacket.BlockUpdate(16, 119, 8, Blocks.STONE));

        assertFalse(world.collidesPlayer(15.65, 120.0, 8.5));
        assertTrue(world.collidesPlayer(15.75, 120.0, 8.5));
    }

    @Test
    void blockUpdatesInvalidateClientCollisionCache() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        assertTrue(world.collidesPlayer(8.5, 80.0, 8.5));

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.AIR));

        assertFalse(world.collidesPlayer(8.5, 80.0, 8.5));
    }

    @Test
    void localPlacementCannotIntersectPlayerBounds() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.AIR));
        Raycast.Hit hit = new Raycast.Hit(8, 63, 8, 0, 1, 0, 1.0);

        assertFalse(world.placeBlock(hit, Blocks.STONE, new Vector3f(8.5f, 65.62f, 8.5f)));
    }

    @Test
    void localPlacementOutsidePlayerBoundsStillWorks() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(10, 64, 8, Blocks.AIR));
        Raycast.Hit hit = new Raycast.Hit(10, 63, 8, 0, 1, 0, 1.0);

        assertTrue(world.placeBlock(hit, Blocks.STONE, new Vector3f(8.5f, 65.62f, 8.5f)));
    }

    @Test
    @Tag("physicsRegression")
    void localPlacementUsesPlacedBlockShape() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.AIR));
        Raycast.Hit hit = new Raycast.Hit(8, 63, 8, 0, 1, 0, 1.0);

        assertTrue(world.placeBlock(hit, Blocks.MOSSY_PATH, new Vector3f(8.5f, 65.82f, 8.5f)));
    }

    @Test
    void localPlacementCannotIntersectVisibleEntityBounds() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 120, 9, Blocks.AIR));
        world.applyEntitySnapshots(List.of(new EntitySnapshot(
                77L,
                "voxel:cozy_sheep",
                null,
                8.5,
                120.0,
                9.5,
                0.0f,
                0.0f,
                10
        )));
        Raycast.Hit hit = new Raycast.Hit(8, 119, 9, 0, 1, 0, 1.0);

        assertFalse(world.placeBlock(hit, Blocks.STONE, new Vector3f(8.5f, 121.62f, 8.5f)));
    }

    @Test
    @Tag("physicsRegression")
    void waterStateSeparatesFeetBodyAndHead() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.WATER));
        world.applyBlock(new GamePacket.BlockUpdate(8, 65, 8, Blocks.AIR));
        world.applyBlock(new GamePacket.BlockUpdate(8, 66, 8, Blocks.AIR));

        PlayerWaterState feetOnly = world.playerWaterState(new Vector3f(8.5f, 66.05f, 8.5f));
        assertTrue(feetOnly.feetInWater());
        assertFalse(feetOnly.bodyInWater());
        assertFalse(feetOnly.headUnderwater());
        assertTrue(feetOnly.movementAffected());

        world.applyBlock(new GamePacket.BlockUpdate(8, 65, 8, Blocks.WATER));
        PlayerWaterState bodyWater = world.playerWaterState(new Vector3f(8.5f, 66.05f, 8.5f));
        assertTrue(bodyWater.bodyInWater());
        assertFalse(bodyWater.headUnderwater());

        world.applyBlock(new GamePacket.BlockUpdate(8, 66, 8, Blocks.WATER));
        PlayerWaterState submerged = world.playerWaterState(new Vector3f(8.5f, 66.05f, 8.5f));
        assertTrue(submerged.headUnderwater());
    }

    @Test
    void playerSurfaceUsesLoadedBlockBelowFeet() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.ICE));
        world.applyBlock(new GamePacket.BlockUpdate(8, 65, 8, Blocks.AIR));

        BlockSurfacePhysics.SurfaceMaterial surface = world.playerSurface(new Vector3f(8.5f, 66.62f, 8.5f));

        assertEquals(BlockSurfacePhysics.ICE, surface);
        assertEquals(BlockSurfacePhysics.DEFAULT, world.surfaceAt(128.5, 64.0, 128.5));
    }

    @Test
    @Tag("physicsRegression")
    void projectileSweepDebugBoundsUseVelocity() {
        ClientWorld world = new ClientWorld(123L);
        world.applyEntitySnapshots(List.of(new EntitySnapshot(
                700L,
                "voxel:arrow_projectile",
                null,
                8.0,
                65.0,
                8.0,
                0.0f,
                0.0f,
                1,
                EntitySnapshot.STATE_PROJECTILE,
                10.0,
                0.0,
                0.0
        )), 1.0);

        List<dev.voxelgame.client.render.ChunkMesh.Bounds> bounds = world.projectileSweepBoundsAround(new Vector3f(8.0f, 65.0f, 8.0f), 32, 1.0);

        assertEquals(1, bounds.size());
        assertTrue(bounds.getFirst().maxX() > 9.9f);
        assertTrue(bounds.getFirst().minX() < 8.0f);
    }
}
