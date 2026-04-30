package dev.voxelgame.client.animation;

public enum AnimationLoopMode {
    ONCE {
        @Override
        double localTime(double elapsedSeconds, double durationSeconds) {
            return Math.min(durationSeconds, elapsedSeconds);
        }
    },
    LOOP {
        @Override
        double localTime(double elapsedSeconds, double durationSeconds) {
            return elapsedSeconds % durationSeconds;
        }
    },
    PING_PONG {
        @Override
        double localTime(double elapsedSeconds, double durationSeconds) {
            double cycle = durationSeconds * 2.0;
            double time = elapsedSeconds % cycle;
            return time <= durationSeconds ? time : cycle - time;
        }
    };

    abstract double localTime(double elapsedSeconds, double durationSeconds);
}
