package dev.voxelgame.common.physics;

import java.util.Objects;
import java.util.Optional;

public final class PartialShapeImpactResolver {
    private static final double EPSILON = 0.0000001;

    private PartialShapeImpactResolver() {
    }

    public static Optional<ImpactResult> raycastProjectile(
            BlockCollisionShape shape,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            ProjectileBounds bounds,
            int blockX,
            int blockY,
            int blockZ
    ) {
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(bounds, "bounds");
        if (!PhysicsNumericGuard.allFinite(fromX, fromY, fromZ, toX, toY, toZ)) {
            return Optional.of(blocking(
                    blockX,
                    blockY,
                    blockZ,
                    finiteOrZero(fromX),
                    finiteOrZero(fromY),
                    finiteOrZero(fromZ),
                    ProjectileHit.BlockFace.NONE,
                    0.0
            ));
        }
        if (shape.empty()) {
            return Optional.empty();
        }

        ImpactResult best = null;
        for (BlockCollisionShape.Box box : shape.boxes()) {
            Optional<ImpactResult> hit = raycastBox(
                    box,
                    fromX,
                    fromY,
                    fromZ,
                    toX,
                    toY,
                    toZ,
                    bounds.radius(),
                    blockX,
                    blockY,
                    blockZ
            );
            if (hit.isPresent() && (best == null || hit.get().fraction() < best.fraction())) {
                best = hit.get();
            }
        }
        return Optional.ofNullable(best);
    }

    public static ImpactResult blocking(
            int blockX,
            int blockY,
            int blockZ,
            double impactX,
            double impactY,
            double impactZ,
            ProjectileHit.BlockFace face,
            double fraction
    ) {
        return new ImpactResult(blockX, blockY, blockZ, impactX, impactY, impactZ, face, fraction);
    }

    private static Optional<ImpactResult> raycastBox(
            BlockCollisionShape.Box box,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double radius,
            int blockX,
            int blockY,
            int blockZ
    ) {
        double minX = blockX + box.minX();
        double minY = blockY + box.minY();
        double minZ = blockZ + box.minZ();
        double maxX = blockX + box.maxX();
        double maxY = blockY + box.maxY();
        double maxZ = blockZ + box.maxZ();

        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        Clip clip = new Clip(0.0, 1.0, ProjectileHit.BlockFace.NONE);
        clip = clipAxis(fromX, dx, minX - radius, maxX + radius, clip, ProjectileHit.BlockFace.WEST, ProjectileHit.BlockFace.EAST);
        if (clip == null) {
            return Optional.empty();
        }
        clip = clipAxis(fromY, dy, minY - radius, maxY + radius, clip, ProjectileHit.BlockFace.DOWN, ProjectileHit.BlockFace.UP);
        if (clip == null) {
            return Optional.empty();
        }
        clip = clipAxis(fromZ, dz, minZ - radius, maxZ + radius, clip, ProjectileHit.BlockFace.NORTH, ProjectileHit.BlockFace.SOUTH);
        if (clip == null) {
            return Optional.empty();
        }

        double centerX = fromX + dx * clip.tMin();
        double centerY = fromY + dy * clip.tMin();
        double centerZ = fromZ + dz * clip.tMin();
        ProjectileHit.BlockFace face = clip.face();
        double impactX = contactCoordinate(centerX, minX, maxX, face.normalX());
        double impactY = contactCoordinate(centerY, minY, maxY, face.normalY());
        double impactZ = contactCoordinate(centerZ, minZ, maxZ, face.normalZ());
        return Optional.of(new ImpactResult(blockX, blockY, blockZ, impactX, impactY, impactZ, face, clip.tMin()));
    }

    private static Clip clipAxis(
            double origin,
            double direction,
            double min,
            double max,
            Clip clip,
            ProjectileHit.BlockFace minFace,
            ProjectileHit.BlockFace maxFace
    ) {
        if (Math.abs(direction) < EPSILON) {
            return origin >= min && origin <= max ? clip : null;
        }
        double inv = 1.0 / direction;
        double near = (min - origin) * inv;
        double far = (max - origin) * inv;
        ProjectileHit.BlockFace nearFace = minFace;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
            nearFace = maxFace;
        }

        double tMin = clip.tMin();
        ProjectileHit.BlockFace face = clip.face();
        if (near > tMin + EPSILON) {
            tMin = near;
            face = nearFace;
        }
        double tMax = Math.min(clip.tMax(), far);
        return tMin <= tMax + EPSILON ? new Clip(Math.max(0.0, tMin), Math.min(1.0, tMax), face) : null;
    }

    private static double contactCoordinate(double center, double min, double max, int normal) {
        if (normal < 0) {
            return min;
        }
        if (normal > 0) {
            return max;
        }
        return Math.max(min, Math.min(max, center));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private record Clip(double tMin, double tMax, ProjectileHit.BlockFace face) {
    }

    public record ImpactResult(
            int blockX,
            int blockY,
            int blockZ,
            double impactX,
            double impactY,
            double impactZ,
            int normalX,
            int normalY,
            int normalZ,
            ProjectileHit.BlockFace face,
            double fraction
    ) {
        public ImpactResult(
                int blockX,
                int blockY,
                int blockZ,
                double impactX,
                double impactY,
                double impactZ,
                ProjectileHit.BlockFace face,
                double fraction
        ) {
            this(
                    blockX,
                    blockY,
                    blockZ,
                    impactX,
                    impactY,
                    impactZ,
                    Objects.requireNonNull(face, "face").normalX(),
                    face.normalY(),
                    face.normalZ(),
                    face,
                    fraction
            );
        }

        public ImpactResult {
            face = Objects.requireNonNull(face, "face");
            if (!PhysicsNumericGuard.allFinite(impactX, impactY, impactZ, fraction) || fraction < 0.0 || fraction > 1.0) {
                throw new IllegalArgumentException("Invalid projectile impact result");
            }
            if (normalX != face.normalX() || normalY != face.normalY() || normalZ != face.normalZ()) {
                throw new IllegalArgumentException("Impact normal must match face");
            }
        }
    }
}
