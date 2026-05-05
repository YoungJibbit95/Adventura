package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.FluidBlocks;

import java.util.Objects;

public final class BlockSurfacePhysics {
    public static final SurfaceMaterial DEFAULT = new SurfaceMaterial("voxel:default", 1.0f, 1.0f, 1.0f, 1.0f);
    public static final SurfaceMaterial ICE = new SurfaceMaterial("voxel:ice", 0.16f, 0.42f, 1.06f, 1.0f);
    public static final SurfaceMaterial SNOW = new SurfaceMaterial("voxel:snow", 1.35f, 0.82f, 0.78f, 0.96f);
    public static final SurfaceMaterial PATH = new SurfaceMaterial("voxel:mossy_path", 1.10f, 1.12f, 1.08f, 1.0f);
    public static final SurfaceMaterial SAND = new SurfaceMaterial("voxel:sand", 1.18f, 0.88f, 0.90f, 0.98f);
    public static final SurfaceMaterial FARMLAND = new SurfaceMaterial("voxel:farmland", 1.16f, 0.86f, 0.86f, 0.97f);

    private BlockSurfacePhysics() {
    }

    @FunctionalInterface
    public interface SurfaceQuery {
        SurfaceMaterial surfaceAt(double eyeX, double footY, double eyeZ);
    }

    public static SurfaceMaterial forBlock(short blockId) {
        if (FluidBlocks.isFluid(blockId)) {
            return DEFAULT;
        }
        return switch (blockId) {
            case Blocks.ICE -> ICE;
            case Blocks.SNOW -> SNOW;
            case Blocks.MOSSY_PATH -> PATH;
            case Blocks.SAND, Blocks.RED_SAND -> SAND;
            case Blocks.FARMLAND -> FARMLAND;
            default -> DEFAULT;
        };
    }

    public record SurfaceMaterial(
            String key,
            float frictionMultiplier,
            float accelerationMultiplier,
            float speedMultiplier,
            float jumpMultiplier
    ) {
        public SurfaceMaterial {
            key = key == null ? "" : key;
            frictionMultiplier = requirePositive("frictionMultiplier", frictionMultiplier);
            accelerationMultiplier = requirePositive("accelerationMultiplier", accelerationMultiplier);
            speedMultiplier = requirePositive("speedMultiplier", speedMultiplier);
            jumpMultiplier = requirePositive("jumpMultiplier", jumpMultiplier);
        }

        public boolean defaultMaterial() {
            return Objects.equals(key, DEFAULT.key);
        }

        private static float requirePositive(String name, float value) {
            if (!Float.isFinite(value) || value <= 0.0f) {
                throw new IllegalArgumentException("Surface " + name + " must be finite and > 0");
            }
            return value;
        }
    }
}
