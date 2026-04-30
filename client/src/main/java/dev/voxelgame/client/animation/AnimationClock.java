package dev.voxelgame.client.animation;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class AnimationClock {
    private static final double NANOS_TO_SECONDS = 1.0 / 1_000_000_000.0;

    private final LongSupplier nanoTime;
    private long lastNanos;
    private double elapsedSeconds;

    public AnimationClock() {
        this(System::nanoTime);
    }

    public AnimationClock(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.lastNanos = nanoTime.getAsLong();
    }

    public double tick() {
        long now = nanoTime.getAsLong();
        if (now <= lastNanos) {
            return 0.0;
        }
        double deltaSeconds = (now - lastNanos) * NANOS_TO_SECONDS;
        lastNanos = now;
        elapsedSeconds += deltaSeconds;
        return deltaSeconds;
    }

    public double elapsedSeconds() {
        return elapsedSeconds;
    }

    public void reset() {
        lastNanos = nanoTime.getAsLong();
        elapsedSeconds = 0.0;
    }
}
