package dev.voxelgame.client.animation;

import java.util.Map;

public final class AnimationSample {
    private static final AnimationSample EMPTY = new AnimationSample(Map.of());

    private final Map<String, Float> values;

    public AnimationSample(Map<String, Float> values) {
        this.values = Map.copyOf(values);
    }

    public static AnimationSample empty() {
        return EMPTY;
    }

    public float value(String channel, float fallback) {
        return values.getOrDefault(channel, fallback);
    }

    public Map<String, Float> values() {
        return values;
    }
}
