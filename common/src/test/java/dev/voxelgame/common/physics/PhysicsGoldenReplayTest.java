package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class PhysicsGoldenReplayTest {
    @Test
    void projectileChunkBoundaryReplayStaysMissUntilRealCollision() throws IOException {
        Properties replay = load("physics-replays/projectile_chunk_boundary.properties");
        ProjectileState state = new ProjectileState(
                1L,
                null,
                "voxel:arrow_projectile",
                doubleValue(replay, "x"),
                doubleValue(replay, "y"),
                doubleValue(replay, "z"),
                doubleValue(replay, "vx"),
                doubleValue(replay, "vy"),
                doubleValue(replay, "vz"),
                0
        );

        ProjectileHit hit = ProjectilePhysics.step(
                state,
                doubleValue(replay, "delta"),
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> bounds.intersectsBlock(x, y, z, 18, 64, 0),
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.valueOf(replay.getProperty("expectedType")), hit.type());
        assertTrue(hit.state().x() > doubleValue(replay, "expectedMinX"));
        assertNotEquals(
                ChunkPos.fromBlock((int) Math.floor(state.x()), (int) Math.floor(state.z())),
                ChunkPos.fromBlock((int) Math.floor(hit.state().x()), (int) Math.floor(hit.state().z()))
        );
    }

    @Test
    void playerMultiStepReplayRunsThroughSharedStepContext() throws IOException {
        Properties replay = load("physics-replays/player_multi_step.properties");
        PhysicsTestWorld world = new PhysicsTestWorld();

        PlayerState state = PhysicsReplayHarness.replayPlayer(replay, world);

        assertTrue(state.x() > doubleValue(replay, "expectedMinX"));
        assertTrue(state.y() > doubleValue(replay, "expectedMinY"));
        assertEquals(Boolean.parseBoolean(replay.getProperty("expectedAirborne")), !state.onGround());
    }

    @Test
    void projectileMultiStepReplayStopsOnTerminalHit() throws IOException {
        Properties replay = load("physics-replays/projectile_multi_step_block.properties");
        PhysicsTestWorld world = new PhysicsTestWorld();
        PhysicsReplayHarness.addSolidBlocks(replay, world);

        ProjectileHit hit = PhysicsReplayHarness.replayProjectile(replay, world);

        assertEquals(ProjectileHit.Type.valueOf(replay.getProperty("expectedType")), hit.type());
        assertEquals(intValue(replay, "expectedBlockX"), hit.blockX());
        assertTrue(hit.terminal());
    }

    @Test
    void entityMultiStepReplaySeparatesFromNeighbor() throws IOException {
        Properties replay = load("physics-replays/entity_multi_step_separation.properties");

        EntitySnapshot state = PhysicsReplayHarness.replayEntity(replay);
        EntitySnapshot neighbor = new EntitySnapshot(
                intValue(replay, "neighborId"),
                replay.getProperty("neighborTypeKey"),
                null,
                doubleValue(replay, "neighborX"),
                doubleValue(replay, "neighborY"),
                doubleValue(replay, "neighborZ"),
                0.0f,
                0.0f,
                10
        );

        assertTrue(state.x() < doubleValue(replay, "expectedMaxX"));
        assertTrue(state.z() > doubleValue(replay, "expectedMinZ"));
        assertEquals(
                Boolean.parseBoolean(replay.getProperty("expectedSeparated")),
                !EntityPhysics.overlaps(state, neighbor, 0.02)
        );
    }

    @Test
    void partialShapePlacementReplayUsesBlockSpecificBounds() throws IOException {
        Properties replay = load("physics-replays/partial_shape_placement.properties");
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        short blockId = blocks.requireByKey(replay.getProperty("blockKey")).id();

        boolean intersects = InteractionRules.placementIntersectsPlayer(
                doubleValue(replay, "eyeX"),
                doubleValue(replay, "eyeY"),
                doubleValue(replay, "eyeZ"),
                intValue(replay, "blockX"),
                intValue(replay, "blockY"),
                intValue(replay, "blockZ"),
                blockId
        );

        assertEquals(Boolean.parseBoolean(replay.getProperty("expectedIntersectsPlayer")), intersects);
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = PhysicsGoldenReplayTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing replay resource: " + resource);
            }
            properties.load(input);
        }
        return properties;
    }

    private static double doubleValue(Properties properties, String key) {
        return Double.parseDouble(properties.getProperty(key));
    }

    private static int intValue(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}
