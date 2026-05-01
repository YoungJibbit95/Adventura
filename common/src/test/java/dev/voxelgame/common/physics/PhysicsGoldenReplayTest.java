package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
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
                (x, y, z) -> false,
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
