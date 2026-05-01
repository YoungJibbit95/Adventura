package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class EntityPhysics {
    private static final double REST_VELOCITY = 0.01;

    private EntityPhysics() {
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canMove(EntitySnapshot current, EntitySnapshot candidate);
    }

    public static EntitySnapshot withPosition(EntitySnapshot snapshot, double x, double y, double z) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new EntitySnapshot(
                snapshot.entityId(),
                snapshot.typeKey(),
                snapshot.ownerPlayerId(),
                x,
                y,
                z,
                snapshot.yaw(),
                snapshot.pitch(),
                snapshot.health(),
                snapshot.stateKey(),
                snapshot.velocityX(),
                snapshot.velocityY(),
                snapshot.velocityZ()
        );
    }

    public static EntitySnapshot withVelocity(EntitySnapshot snapshot, double velocityX, double velocityY, double velocityZ) {
        return snapshot.withVelocity(velocityX, velocityY, velocityZ);
    }

    public static EntitySnapshot applyImpulseMotion(EntitySnapshot current, EntitySnapshot candidate, EntityPhysicsProfile profile) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(candidate, "candidate");
        profile = profile == null ? EntityPhysicsProfile.forType(current.typeKey()) : profile;
        if (Math.abs(current.velocityX()) <= REST_VELOCITY
                && Math.abs(current.velocityY()) <= REST_VELOCITY
                && Math.abs(current.velocityZ()) <= REST_VELOCITY) {
            return candidate.withVelocity(0.0, 0.0, 0.0);
        }

        double nextX = candidate.x() + current.velocityX();
        double nextY = candidate.y() + current.velocityY();
        double nextZ = candidate.z() + current.velocityZ();
        double nextVelocityX = damp(current.velocityX(), profile.knockbackFriction());
        double nextVelocityY = Math.max(current.velocityY() - profile.knockbackGravity(), -profile.maxFallSpeed());
        double nextVelocityZ = damp(current.velocityZ(), profile.knockbackFriction());
        return new EntitySnapshot(
                candidate.entityId(),
                candidate.typeKey(),
                candidate.ownerPlayerId(),
                nextX,
                nextY,
                nextZ,
                candidate.yaw(),
                candidate.pitch(),
                candidate.health(),
                candidate.stateKey(),
                nextVelocityX,
                damp(nextVelocityY, profile.knockbackFriction()),
                nextVelocityZ
        );
    }

    public static EntitySnapshot applySeparation(EntitySnapshot current, EntitySnapshot candidate, Collection<EntitySnapshot> neighbors) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(candidate, "candidate");
        if (neighbors == null || neighbors.isEmpty()) {
            return candidate;
        }
        EntityPhysicsProfile profile = EntityPhysicsProfile.forType(candidate.typeKey());
        double dx = 0.0;
        double dz = 0.0;
        for (EntitySnapshot neighbor : neighbors) {
            Separation separation = separationFrom(candidate, neighbor, profile.separationPadding());
            dx += separation.x();
            dz += separation.z();
        }
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.0001) {
            return candidate;
        }
        EntityBounds bounds = EntityBounds.forType(candidate.typeKey());
        double step = Math.max(profile.separationStep(), Math.max(bounds.width(), bounds.depth()) + profile.separationPadding());
        if (length > step && step > 0.0) {
            dx = dx / length * step;
            dz = dz / length * step;
        }
        return withPosition(candidate, candidate.x() + dx, candidate.y(), candidate.z() + dz);
    }

    public static boolean overlaps(EntitySnapshot first, EntitySnapshot second, double padding) {
        if (first == null || second == null || first.entityId() == second.entityId()) {
            return false;
        }
        EntityBounds firstBounds = EntityBounds.forType(first.typeKey());
        EntityBounds secondBounds = EntityBounds.forType(second.typeKey());
        double firstBaseY = EntityBounds.baseY(first);
        double secondBaseY = EntityBounds.baseY(second);
        return firstBounds.minX(first.x()) < secondBounds.maxX(second.x()) + padding
                && firstBounds.maxX(first.x()) > secondBounds.minX(second.x()) - padding
                && firstBounds.minY(firstBaseY) < secondBounds.maxY(secondBaseY) + padding
                && firstBounds.maxY(firstBaseY) > secondBounds.minY(secondBaseY) - padding
                && firstBounds.minZ(first.z()) < secondBounds.maxZ(second.z()) + padding
                && firstBounds.maxZ(first.z()) > secondBounds.minZ(second.z()) - padding;
    }

    public static boolean overlapsAny(EntitySnapshot candidate, Collection<EntitySnapshot> neighbors, double padding) {
        if (neighbors == null || neighbors.isEmpty()) {
            return false;
        }
        for (EntitySnapshot neighbor : neighbors) {
            if (overlaps(candidate, neighbor, padding)) {
                return true;
            }
        }
        return false;
    }

    public static MoveResult sweepWithSlide(EntitySnapshot current, EntitySnapshot candidate, MovementValidator validator) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(validator, "validator");
        if (validator.canMove(current, candidate)) {
            return new MoveResult(candidate, false, false, false);
        }

        EntitySnapshot xOnly = slideCandidate(current, candidate, true, false, false);
        if (movedOnAnyAxis(current, xOnly) && validator.canMove(current, xOnly)) {
            return new MoveResult(xOnly, false, true, false);
        }
        EntitySnapshot zOnly = slideCandidate(current, candidate, false, false, true);
        if (movedOnAnyAxis(current, zOnly) && validator.canMove(current, zOnly)) {
            return new MoveResult(zOnly, false, true, false);
        }
        EntitySnapshot yOnly = slideCandidate(current, candidate, false, true, false);
        if (movedOnAnyAxis(current, yOnly) && validator.canMove(current, yOnly)) {
            return new MoveResult(yOnly, false, true, false);
        }

        EntityPhysicsProfile profile = EntityPhysicsProfile.forType(current.typeKey());
        for (EntitySnapshot recovery : recoveryCandidates(current, candidate, profile.recoveryStep())) {
            if (validator.canMove(current, recovery)) {
                return new MoveResult(recovery, false, false, true);
            }
        }

        return new MoveResult(blockedAtCurrent(current, candidate), true, false, false);
    }

    private static Separation separationFrom(EntitySnapshot candidate, EntitySnapshot neighbor, double padding) {
        if (neighbor == null || candidate.entityId() == neighbor.entityId()) {
            return Separation.NONE;
        }
        EntityBounds firstBounds = EntityBounds.forType(candidate.typeKey());
        EntityBounds secondBounds = EntityBounds.forType(neighbor.typeKey());
        double firstBaseY = EntityBounds.baseY(candidate);
        double secondBaseY = EntityBounds.baseY(neighbor);
        double overlapY = Math.min(firstBounds.maxY(firstBaseY), secondBounds.maxY(secondBaseY) + padding)
                - Math.max(firstBounds.minY(firstBaseY), secondBounds.minY(secondBaseY) - padding);
        if (overlapY <= 0.0) {
            return Separation.NONE;
        }

        double overlapX = Math.min(firstBounds.maxX(candidate.x()), secondBounds.maxX(neighbor.x()) + padding)
                - Math.max(firstBounds.minX(candidate.x()), secondBounds.minX(neighbor.x()) - padding);
        double overlapZ = Math.min(firstBounds.maxZ(candidate.z()), secondBounds.maxZ(neighbor.z()) + padding)
                - Math.max(firstBounds.minZ(candidate.z()), secondBounds.minZ(neighbor.z()) - padding);
        if (overlapX <= 0.0 || overlapZ <= 0.0) {
            return Separation.NONE;
        }

        double dx = candidate.x() - neighbor.x();
        double dz = candidate.z() - neighbor.z();
        if (Math.abs(dx) <= 0.0001 && Math.abs(dz) <= 0.0001) {
            double angle = deterministicAngle(candidate.entityId(), neighbor.entityId());
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        }
        if (overlapX < overlapZ) {
            return new Separation(Math.signum(dx == 0.0 ? 1.0 : dx) * overlapX, 0.0);
        }
        return new Separation(0.0, Math.signum(dz == 0.0 ? 1.0 : dz) * overlapZ);
    }

    private static EntitySnapshot slideCandidate(EntitySnapshot current, EntitySnapshot candidate, boolean x, boolean y, boolean z) {
        return new EntitySnapshot(
                candidate.entityId(),
                candidate.typeKey(),
                candidate.ownerPlayerId(),
                x ? candidate.x() : current.x(),
                y ? candidate.y() : current.y(),
                z ? candidate.z() : current.z(),
                candidate.yaw(),
                candidate.pitch(),
                candidate.health(),
                candidate.stateKey(),
                x ? candidate.velocityX() : 0.0,
                y ? candidate.velocityY() : 0.0,
                z ? candidate.velocityZ() : 0.0
        );
    }

    private static List<EntitySnapshot> recoveryCandidates(EntitySnapshot current, EntitySnapshot candidate, double step) {
        if (step <= 0.0) {
            return List.of();
        }
        return List.of(
                recoveryCandidate(current, candidate, step, 0.0),
                recoveryCandidate(current, candidate, -step, 0.0),
                recoveryCandidate(current, candidate, 0.0, step),
                recoveryCandidate(current, candidate, 0.0, -step),
                recoveryCandidate(current, candidate, step, step),
                recoveryCandidate(current, candidate, -step, step),
                recoveryCandidate(current, candidate, step, -step),
                recoveryCandidate(current, candidate, -step, -step)
        );
    }

    private static EntitySnapshot recoveryCandidate(EntitySnapshot current, EntitySnapshot candidate, double dx, double dz) {
        return new EntitySnapshot(
                candidate.entityId(),
                candidate.typeKey(),
                candidate.ownerPlayerId(),
                current.x() + dx,
                current.y(),
                current.z() + dz,
                candidate.yaw(),
                candidate.pitch(),
                candidate.health(),
                candidate.stateKey(),
                0.0,
                0.0,
                0.0
        );
    }

    private static EntitySnapshot blockedAtCurrent(EntitySnapshot current, EntitySnapshot candidate) {
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                current.x(),
                current.y(),
                current.z(),
                candidate.yaw(),
                candidate.pitch(),
                candidate.health(),
                candidate.stateKey(),
                0.0,
                0.0,
                0.0
        );
    }

    private static boolean movedOnAnyAxis(EntitySnapshot current, EntitySnapshot candidate) {
        return Math.abs(candidate.x() - current.x()) > 0.0001
                || Math.abs(candidate.y() - current.y()) > 0.0001
                || Math.abs(candidate.z() - current.z()) > 0.0001;
    }

    private static double damp(double value, double friction) {
        double next = value * friction;
        return Math.abs(next) <= REST_VELOCITY ? 0.0 : next;
    }

    private static double deterministicAngle(long firstId, long secondId) {
        long mixed = firstId * 0x9E3779B97F4A7C15L ^ secondId * 0xC2B2AE3D27D4EB4FL;
        long positive = mixed & Long.MAX_VALUE;
        return (positive % 628_319L) / 100_000.0;
    }

    public record MoveResult(EntitySnapshot snapshot, boolean blocked, boolean slid, boolean recovered) {
    }

    private record Separation(double x, double z) {
        private static final Separation NONE = new Separation(0.0, 0.0);
    }
}
