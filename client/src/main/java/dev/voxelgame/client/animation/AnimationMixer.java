package dev.voxelgame.client.animation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnimationMixer {
    private final Map<String, AnimationPlayback> layers = new LinkedHashMap<>();

    public void play(String layer, AnimationClip clip, double nowSeconds) {
        layer(layer).play(clip, nowSeconds);
    }

    public void stop(String layer) {
        AnimationPlayback playback = layers.get(layer);
        if (playback != null) {
            playback.stop();
        }
    }

    public boolean active(String layer, double nowSeconds) {
        AnimationPlayback playback = layers.get(layer);
        return playback != null && playback.active(nowSeconds);
    }

    public AnimationSample sample(double nowSeconds) {
        Map<String, Float> values = new LinkedHashMap<>();
        Iterator<Map.Entry<String, AnimationPlayback>> iterator = layers.entrySet().iterator();
        while (iterator.hasNext()) {
            AnimationPlayback playback = iterator.next().getValue();
            if (!playback.active(nowSeconds)) {
                iterator.remove();
                continue;
            }
            values.putAll(playback.sample(nowSeconds).values());
        }
        return new AnimationSample(values);
    }

    private AnimationPlayback layer(String layer) {
        if (layer == null || layer.isBlank()) {
            throw new IllegalArgumentException("Animation layer must not be blank");
        }
        return layers.computeIfAbsent(layer, ignored -> new AnimationPlayback());
    }
}
