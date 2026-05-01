package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class PhysicsFuzzTest {
    @Test
    void finiteMovementProjectileAndPlacementSamplesStayFinite() {
        Random random = new Random(0xAD0EA17AL);
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        short[] placementBlocks = {
                Blocks.STONE,
                Blocks.MOSSY_PATH,
                Blocks.GARDEN_FENCE,
                Blocks.CAMPFIRE,
                Blocks.STORAGE_CRATE
        };
        EntitySnapshot sheep = new EntitySnapshot(99L, "voxel:cozy_sheep", null, 8.5, 64.0, 8.5, 0.0f, 0.0f, 10);

        for (int i = 0; i < 256; i++) {
            double x = random.nextDouble(-32.0, 32.0);
            double y = random.nextDouble(48.0, 120.0);
            double z = random.nextDouble(-32.0, 32.0);
            PlayerState state = new PlayerState(
                    x,
                    y,
                    z,
                    random.nextFloat(-4.0f, 4.0f),
                    random.nextFloat(-8.0f, 4.0f),
                    random.nextFloat(-4.0f, 4.0f),
                    random.nextBoolean(),
                    random.nextBoolean(),
                    0.0f
            );
            PlayerInput input = new PlayerInput(random.nextFloat(-1.0f, 1.0f), random.nextFloat(-1.0f, 1.0f), random.nextBoolean(), false, random.nextBoolean());
            PlayerState next = PlayerPhysics.stepSurvival(state, input, new PlayerWaterState(random.nextBoolean(), random.nextBoolean(), random.nextBoolean()), random.nextFloat(0.001f, 0.08f), config, (eyeX, eyeY, eyeZ) -> false);
            assertTrue(Double.isFinite(next.x()));
            assertTrue(Double.isFinite(next.y()));
            assertTrue(Double.isFinite(next.z()));

            ProjectileState projectile = new ProjectileState(
                    i + 1L,
                    null,
                    "voxel:arrow_projectile",
                    x,
                    y,
                    z,
                    random.nextDouble(-24.0, 24.0),
                    random.nextDouble(-8.0, 8.0),
                    random.nextDouble(-24.0, 24.0),
                    0
            );
            ProjectileHit hit = ProjectilePhysics.step(projectile, random.nextDouble(0.001, 0.12), ProjectilePhysicsConfig.arrow(), (px, py, pz, bounds) -> false, (ProjectilePhysics.WaterQuery) (px, py, pz) -> random.nextBoolean(), List.of(sheep));
            assertTrue(Double.isFinite(hit.state().x()));
            assertTrue(Double.isFinite(hit.state().y()));
            assertTrue(Double.isFinite(hit.state().z()));

            short blockId = placementBlocks[random.nextInt(placementBlocks.length)];
            BlockType block = blocks.requireById(blockId);
            int blockX = random.nextInt(-16, 16);
            int blockY = random.nextInt(40, 120);
            int blockZ = random.nextInt(-16, 16);
            assertDoesNotThrow(() -> InteractionRules.placementIntersectsPlayer(x, y, z, blockX, blockY, blockZ, block.id()));
            assertDoesNotThrow(() -> InteractionRules.placementIntersectsEntity(sheep, blockX, blockY, blockZ, block.id()));
        }
    }
}
