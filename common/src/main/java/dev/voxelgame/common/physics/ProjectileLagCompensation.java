package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.Objects;

public final class ProjectileLagCompensation {
    public static final double MAX_REWIND_SECONDS = 0.18;

    private ProjectileLagCompensation() {
    }

    public static RewindWindow evaluate(double requestedSeconds) {
        if (!Double.isFinite(requestedSeconds) || requestedSeconds <= 0.0) {
            return new RewindWindow(0.0, 0.0, false);
        }
        double applied = Math.min(requestedSeconds, MAX_REWIND_SECONDS);
        return new RewindWindow(requestedSeconds, applied, applied < requestedSeconds);
    }

    public static EntitySnapshot rewindLinear(EntitySnapshot snapshot, double seconds) {
        Objects.requireNonNull(snapshot, "snapshot");
        RewindWindow window = evaluate(seconds);
        if (window.appliedSeconds() == 0.0) {
            return snapshot;
        }
        return new EntitySnapshot(
                snapshot.entityId(),
                snapshot.typeKey(),
                snapshot.ownerPlayerId(),
                snapshot.x() - snapshot.velocityX() * window.appliedSeconds(),
                snapshot.y() - snapshot.velocityY() * window.appliedSeconds(),
                snapshot.z() - snapshot.velocityZ() * window.appliedSeconds(),
                snapshot.yaw(),
                snapshot.pitch(),
                snapshot.health(),
                snapshot.stateKey(),
                snapshot.velocityX(),
                snapshot.velocityY(),
                snapshot.velocityZ()
        );
    }

    public record RewindWindow(double requestedSeconds, double appliedSeconds, boolean clipped) {
        public RewindWindow {
            if (!Double.isFinite(requestedSeconds) || !Double.isFinite(appliedSeconds)
                    || requestedSeconds < 0.0 || appliedSeconds < 0.0) {
                throw new IllegalArgumentException("Rewind window must be finite and non-negative");
            }
        }
    }
}
