package dev.voxelgame.client.audio;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioCueRulesTest {
    @Test
    void stepCueMatchesCoarseMaterialFamily() {
        var blocks = Blocks.createDefaultRegistry();

        assertEquals(AudioCue.STEP_GRASS, AudioCueRules.stepCueFor(blocks.requireById(Blocks.GRASS)));
        assertEquals(AudioCue.STEP_STONE, AudioCueRules.stepCueFor(blocks.requireById(Blocks.STONE)));
        assertEquals(AudioCue.STEP_WOOD, AudioCueRules.stepCueFor(blocks.requireById(Blocks.SKYROOT_PLANKS)));
    }

    @Test
    void breakCueMatchesCoarseMaterialFamily() {
        var blocks = Blocks.createDefaultRegistry();

        assertEquals(AudioCue.BLOCK_BREAK, AudioCueRules.breakCueFor(blocks.requireById(Blocks.WILD_GRASS)));
        assertEquals(AudioCue.BREAK_STONE, AudioCueRules.breakCueFor(blocks.requireById(Blocks.GLOW_CRYSTAL_NODE)));
        assertEquals(AudioCue.BREAK_WOOD, AudioCueRules.breakCueFor(blocks.requireById(Blocks.STORAGE_CRATE)));
    }
}
