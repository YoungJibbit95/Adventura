package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntityBounds;

import java.util.List;
import java.util.Objects;

public record BlockCollisionShape(List<Box> boxes) {
    public static final BlockCollisionShape NONE = new BlockCollisionShape(List.of());
    public static final BlockCollisionShape FULL = box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public BlockCollisionShape {
        boxes = List.copyOf(Objects.requireNonNull(boxes, "boxes"));
    }

    public static BlockCollisionShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new BlockCollisionShape(List.of(new Box(minX, minY, minZ, maxX, maxY, maxZ)));
    }

    public static BlockCollisionShape boxes(Box... boxes) {
        return new BlockCollisionShape(List.of(boxes));
    }

    public boolean empty() {
        return boxes.isEmpty();
    }

    public boolean intersectsPlayer(PlayerBounds bounds, double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        Objects.requireNonNull(bounds, "bounds");
        return intersectsAabb(
                bounds.minX(eyeX),
                bounds.minY(eyeY),
                bounds.minZ(eyeZ),
                bounds.maxX(eyeX),
                bounds.maxY(eyeY),
                bounds.maxZ(eyeZ),
                blockX,
                blockY,
                blockZ
        );
    }

    public boolean intersectsEntity(EntityBounds bounds, double centerX, double baseY, double centerZ, int blockX, int blockY, int blockZ) {
        Objects.requireNonNull(bounds, "bounds");
        return intersectsAabb(
                bounds.minX(centerX),
                bounds.minY(baseY),
                bounds.minZ(centerZ),
                bounds.maxX(centerX),
                bounds.maxY(baseY),
                bounds.maxZ(centerZ),
                blockX,
                blockY,
                blockZ
        );
    }

    public boolean intersectsProjectile(ProjectileBounds bounds, double x, double y, double z, int blockX, int blockY, int blockZ) {
        Objects.requireNonNull(bounds, "bounds");
        double radius = bounds.radius();
        return intersectsAabb(
                x - radius,
                y - radius,
                z - radius,
                x + radius,
                y + radius,
                z + radius,
                blockX,
                blockY,
                blockZ
        );
    }

    public boolean intersectsAabb(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            int blockX,
            int blockY,
            int blockZ
    ) {
        if (!PhysicsNumericGuard.allFinite(minX, minY, minZ, maxX, maxY, maxZ)) {
            return true;
        }
        for (Box box : boxes) {
            if (maxX > blockX + box.minX()
                    && minX < blockX + box.maxX()
                    && maxY > blockY + box.minY()
                    && minY < blockY + box.maxY()
                    && maxZ > blockZ + box.minZ()
                    && minZ < blockZ + box.maxZ()) {
                return true;
            }
        }
        return false;
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Box {
            PhysicsNumericGuard.requireFiniteBounds(minX, minY, minZ, maxX, maxY, maxZ);
            if (minX < 0.0 || minY < 0.0 || minZ < 0.0
                    || maxX > 1.0 || maxY > 1.5 || maxZ > 1.0
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("Invalid block collision box");
            }
        }
    }
}
