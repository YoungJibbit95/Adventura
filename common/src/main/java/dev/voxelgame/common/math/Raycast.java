package dev.voxelgame.common.math;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.WorldView;
import org.joml.Vector3d;

import java.util.Optional;

public final class Raycast {
    private Raycast() {
    }

    public static Optional<Hit> firstSolid(WorldView world, Vector3d origin, Vector3d direction, double maxDistance) {
        Vector3d dir = new Vector3d(direction).normalize();
        double step = 0.05;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        boolean hasLastEmpty = false;
        for (double distance = 0.0; distance <= maxDistance; distance += step) {
            int x = fastFloor(origin.x + dir.x * distance);
            int y = fastFloor(origin.y + dir.y * distance);
            int z = fastFloor(origin.z + dir.z * distance);
            if (x == lastX && y == lastY && z == lastZ) {
                continue;
            }
            BlockType block = world.blockType(world.blockId(x, y, z));
            if (isTargetable(block)) {
                int faceX;
                int faceY;
                int faceZ;
                if (hasLastEmpty && manhattan(x, y, z, lastX, lastY, lastZ) == 1) {
                    faceX = lastX - x;
                    faceY = lastY - y;
                    faceZ = lastZ - z;
                } else {
                    Axis axis = dominantAxis(dir);
                    faceX = axis == Axis.X ? -Integer.signum((int) Math.copySign(1.0, dir.x)) : 0;
                    faceY = axis == Axis.Y ? -Integer.signum((int) Math.copySign(1.0, dir.y)) : 0;
                    faceZ = axis == Axis.Z ? -Integer.signum((int) Math.copySign(1.0, dir.z)) : 0;
                }
                return Optional.of(new Hit(x, y, z, faceX, faceY, faceZ, distance));
            }
            lastX = x;
            lastY = y;
            lastZ = z;
            hasLastEmpty = true;
        }
        return Optional.empty();
    }

    private static int manhattan(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    private static boolean isTargetable(BlockType block) {
        return block.id() != 0 && (block.collidable() || block.dropItemKey() != null);
    }

    private static Axis dominantAxis(Vector3d direction) {
        double ax = Math.abs(direction.x);
        double ay = Math.abs(direction.y);
        double az = Math.abs(direction.z);
        if (ax >= ay && ax >= az) {
            return Axis.X;
        }
        if (ay >= az) {
            return Axis.Y;
        }
        return Axis.Z;
    }

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    public record Hit(int x, int y, int z, int faceX, int faceY, int faceZ, double distance) {
        public int placeX() {
            return x + faceX;
        }

        public int placeY() {
            return y + faceY;
        }

        public int placeZ() {
            return z + faceZ;
        }
    }
}
