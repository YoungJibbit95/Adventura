package dev.voxelgame.client.audio;

import dev.voxelgame.common.block.BlockType;

public final class AudioCueRules {
    private AudioCueRules() {
    }

    public static AudioCue stepCueFor(BlockType block) {
        String key = block.key();
        if (isWood(key)) {
            return AudioCue.STEP_WOOD;
        }
        if (isStoneLike(key)) {
            return AudioCue.STEP_STONE;
        }
        return AudioCue.STEP_GRASS;
    }

    public static AudioCue breakCueFor(BlockType block) {
        String key = block.key();
        if (isWood(key)) {
            return AudioCue.BREAK_WOOD;
        }
        if (isStoneLike(key)) {
            return AudioCue.BREAK_STONE;
        }
        return AudioCue.BLOCK_BREAK;
    }

    private static boolean isWood(String key) {
        return key.contains("log")
                || key.contains("wood")
                || key.contains("planks")
                || key.contains("crate")
                || key.contains("chair")
                || key.contains("table")
                || key.contains("fence")
                || key.contains("stump");
    }

    private static boolean isStoneLike(String key) {
        return key.contains("stone")
                || key.contains("ore")
                || key.contains("crystal")
                || key.contains("gravel")
                || key.contains("clay")
                || key.contains("sand")
                || key.contains("snow")
                || key.contains("ice");
    }
}
