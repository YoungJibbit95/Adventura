package dev.voxelgame.common.physics;

import java.util.Objects;

public record ProjectileHit(
        Type type,
        ProjectileState state,
        int blockX,
        int blockY,
        int blockZ,
        long entityId,
        double impactX,
        double impactY,
        double impactZ,
        BlockFace blockFace
) {
    public static ProjectileHit miss(ProjectileState state) {
        return new ProjectileHit(Type.MISS, state, 0, 0, 0, 0L, state.x(), state.y(), state.z(), BlockFace.NONE);
    }

    public static ProjectileHit block(ProjectileState state, int blockX, int blockY, int blockZ) {
        return block(state, blockX, blockY, blockZ, state.x(), state.y(), state.z(), BlockFace.NONE);
    }

    public static ProjectileHit block(
            ProjectileState state,
            int blockX,
            int blockY,
            int blockZ,
            double impactX,
            double impactY,
            double impactZ,
            BlockFace blockFace
    ) {
        return new ProjectileHit(Type.BLOCK, state, blockX, blockY, blockZ, 0L, impactX, impactY, impactZ, blockFace);
    }

    public static ProjectileHit entity(ProjectileState state, long entityId) {
        return new ProjectileHit(Type.ENTITY, state, 0, 0, 0, entityId, state.x(), state.y(), state.z(), BlockFace.NONE);
    }

    public static ProjectileHit expired(ProjectileState state) {
        return new ProjectileHit(Type.EXPIRED, state, 0, 0, 0, 0L, state.x(), state.y(), state.z(), BlockFace.NONE);
    }

    public boolean terminal() {
        return type != Type.MISS;
    }

    public ProjectileHit {
        type = Objects.requireNonNull(type, "type");
        state = Objects.requireNonNull(state, "state");
        blockFace = Objects.requireNonNull(blockFace, "blockFace");
        if (!Double.isFinite(impactX) || !Double.isFinite(impactY) || !Double.isFinite(impactZ)) {
            throw new IllegalArgumentException("Projectile impact coordinates must be finite");
        }
    }

    public enum Type {
        MISS,
        BLOCK,
        ENTITY,
        EXPIRED
    }

    public enum BlockFace {
        NONE(0, 0, 0),
        WEST(-1, 0, 0),
        EAST(1, 0, 0),
        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1);

        private final int normalX;
        private final int normalY;
        private final int normalZ;

        BlockFace(int normalX, int normalY, int normalZ) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }

        public int normalX() {
            return normalX;
        }

        public int normalY() {
            return normalY;
        }

        public int normalZ() {
            return normalZ;
        }
    }
}
