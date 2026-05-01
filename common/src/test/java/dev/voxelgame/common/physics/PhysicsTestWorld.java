package dev.voxelgame.common.physics;

import java.util.HashSet;
import java.util.Set;

final class PhysicsTestWorld {
    private final Set<BlockPos> solidBlocks = new HashSet<>();
    private final Set<BlockPos> waterBlocks = new HashSet<>();

    PhysicsTestWorld solid(int x, int y, int z) {
        solidBlocks.add(new BlockPos(x, y, z));
        return this;
    }

    PhysicsTestWorld water(int x, int y, int z) {
        waterBlocks.add(new BlockPos(x, y, z));
        return this;
    }

    PlayerPhysics.CollisionQuery playerCollision(PlayerPhysicsConfig config) {
        return (eyeX, eyeY, eyeZ) -> solidBlocks.stream()
                .anyMatch(block -> config.bounds().intersectsBlock(eyeX, eyeY, eyeZ, block.x(), block.y(), block.z()));
    }

    ProjectilePhysics.BlockCollisionQuery projectileCollision() {
        return (x, y, z, bounds) -> solidBlocks.stream()
                .anyMatch(block -> bounds.intersectsBlock(x, y, z, block.x(), block.y(), block.z()));
    }

    ProjectilePhysics.WaterQuery waterQuery() {
        return (x, y, z) -> waterBlocks.contains(new BlockPos(floor(x), floor(y), floor(z)));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private record BlockPos(int x, int y, int z) {
    }
}
