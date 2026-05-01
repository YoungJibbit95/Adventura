package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ProjectilePhysics {
    private ProjectilePhysics() {
    }

    @FunctionalInterface
    public interface BlockCollisionQuery {
        boolean collides(double x, double y, double z, ProjectileBounds bounds);
    }

    @FunctionalInterface
    public interface WaterQuery {
        boolean inWater(double x, double y, double z);
    }

    public static ProjectileHit step(
            ProjectileState state,
            double deltaSeconds,
            ProjectilePhysicsConfig config,
            BlockCollisionQuery blockCollision,
            WaterQuery waterQuery,
            Collection<EntitySnapshot> targets
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(blockCollision, "blockCollision");
        Objects.requireNonNull(waterQuery, "waterQuery");
        Objects.requireNonNull(targets, "targets");
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Projectile delta must be finite and non-negative");
        }
        if (state.ageTicks() >= config.maxLifetimeTicks()) {
            return ProjectileHit.expired(state);
        }
        if (deltaSeconds == 0.0) {
            return ProjectileHit.miss(state);
        }

        double x = state.x();
        double y = state.y();
        double z = state.z();
        double velocityX = state.velocityX();
        double velocityY = state.velocityY();
        double velocityZ = state.velocityZ();
        double maxDelta = Math.max(Math.abs(velocityX * deltaSeconds), Math.max(Math.abs(velocityY * deltaSeconds), Math.abs(velocityZ * deltaSeconds)));
        int steps = Math.max(1, (int) Math.ceil(maxDelta / config.maxStep()));
        double stepSeconds = deltaSeconds / steps;
        for (int i = 0; i < steps; i++) {
            if (waterQuery.inWater(x, y, z)) {
                velocityX *= config.waterDrag();
                velocityY *= config.waterDrag();
                velocityZ *= config.waterDrag();
            }
            velocityY -= config.gravity() * stepSeconds;
            double nextX = x + velocityX * stepSeconds;
            double nextY = y + velocityY * stepSeconds;
            double nextZ = z + velocityZ * stepSeconds;
            Optional<EntitySnapshot> hitEntity = firstEntityHit(
                    state.projectileId(),
                    state.ownerPlayerId(),
                    x,
                    y,
                    z,
                    nextX,
                    nextY,
                    nextZ,
                    config.bounds().radius(),
                    targets
            );
            ProjectileState sample = new ProjectileState(
                    state.projectileId(),
                    state.ownerPlayerId(),
                    state.typeKey(),
                    nextX,
                    nextY,
                    nextZ,
                    velocityX,
                    velocityY,
                    velocityZ,
                    state.ageTicks() + 1
            );
            if (hitEntity.isPresent()) {
                return ProjectileHit.entity(sample, hitEntity.get().entityId());
            }
            if (blockCollision.collides(nextX, nextY, nextZ, config.bounds())) {
                ProjectileHit.BlockFace face = blockFaceForVelocity(velocityX, velocityY, velocityZ);
                return ProjectileHit.block(
                        sample,
                        floor(nextX + travelOffset(velocityX, config.bounds().radius())),
                        floor(nextY + travelOffset(velocityY, config.bounds().radius())),
                        floor(nextZ + travelOffset(velocityZ, config.bounds().radius())),
                        nextX,
                        nextY,
                        nextZ,
                        face
                );
            }
            x = nextX;
            y = nextY;
            z = nextZ;
        }

        ProjectileState next = new ProjectileState(
                state.projectileId(),
                state.ownerPlayerId(),
                state.typeKey(),
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ,
                state.ageTicks() + 1
        );
        return next.ageTicks() >= config.maxLifetimeTicks()
                ? ProjectileHit.expired(next)
                : ProjectileHit.miss(next);
    }

    public static Optional<EntitySnapshot> firstEntityHit(
            long projectileId,
            UUID ownerPlayerId,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double radius,
            Collection<EntitySnapshot> targets
    ) {
        EntitySnapshot best = null;
        double bestT = Double.POSITIVE_INFINITY;
        for (EntitySnapshot target : targets) {
            if (target.entityId() == projectileId || isOwnerPlayer(ownerPlayerId, target)) {
                continue;
            }
            EntityBounds bounds = EntityBounds.forType(target.typeKey());
            double baseY = EntityBounds.baseY(target);
            double t = segmentAabbIntersection(
                    fromX,
                    fromY,
                    fromZ,
                    toX,
                    toY,
                    toZ,
                    bounds.minX(target.x()) - radius,
                    bounds.minY(baseY) - radius,
                    bounds.minZ(target.z()) - radius,
                    bounds.maxX(target.x()) + radius,
                    bounds.maxY(baseY) + radius,
                    bounds.maxZ(target.z()) + radius
            );
            if (t >= 0.0 && t < bestT) {
                bestT = t;
                best = target;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isOwnerPlayer(UUID ownerPlayerId, EntitySnapshot target) {
        return ownerPlayerId != null && ownerPlayerId.equals(target.ownerPlayerId());
    }

    private static double segmentAabbIntersection(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double tMin = 0.0;
        double tMax = 1.0;
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double[] result = clipAxis(fromX, dx, minX, maxX, tMin, tMax);
        if (result == null) {
            return -1.0;
        }
        tMin = result[0];
        tMax = result[1];
        result = clipAxis(fromY, dy, minY, maxY, tMin, tMax);
        if (result == null) {
            return -1.0;
        }
        tMin = result[0];
        tMax = result[1];
        result = clipAxis(fromZ, dz, minZ, maxZ, tMin, tMax);
        return result == null ? -1.0 : result[0];
    }

    private static double[] clipAxis(double origin, double direction, double min, double max, double tMin, double tMax) {
        if (Math.abs(direction) < 0.0000001) {
            return origin >= min && origin <= max ? new double[]{tMin, tMax} : null;
        }
        double inv = 1.0 / direction;
        double near = (min - origin) * inv;
        double far = (max - origin) * inv;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }
        tMin = Math.max(tMin, near);
        tMax = Math.min(tMax, far);
        return tMin <= tMax ? new double[]{tMin, tMax} : null;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double travelOffset(double velocity, double radius) {
        if (Math.abs(velocity) < 0.0000001) {
            return 0.0;
        }
        return Math.copySign(radius, velocity);
    }

    private static ProjectileHit.BlockFace blockFaceForVelocity(double velocityX, double velocityY, double velocityZ) {
        double absX = Math.abs(velocityX);
        double absY = Math.abs(velocityY);
        double absZ = Math.abs(velocityZ);
        if (absX >= absY && absX >= absZ && absX > 0.0000001) {
            return velocityX > 0.0 ? ProjectileHit.BlockFace.WEST : ProjectileHit.BlockFace.EAST;
        }
        if (absY >= absZ && absY > 0.0000001) {
            return velocityY > 0.0 ? ProjectileHit.BlockFace.DOWN : ProjectileHit.BlockFace.UP;
        }
        if (absZ > 0.0000001) {
            return velocityZ > 0.0 ? ProjectileHit.BlockFace.NORTH : ProjectileHit.BlockFace.SOUTH;
        }
        return ProjectileHit.BlockFace.NONE;
    }
}
