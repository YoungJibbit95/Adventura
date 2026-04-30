package dev.voxelgame.client.animation;

public enum AnimationCurve {
    LINEAR {
        @Override
        public float apply(double progress) {
            return Easing.linear(progress);
        }
    },
    SMOOTH_STEP {
        @Override
        public float apply(double progress) {
            return Easing.smoothStep(progress);
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float apply(double progress) {
            return Easing.easeOutCubic(progress);
        }
    };

    public abstract float apply(double progress);
}
