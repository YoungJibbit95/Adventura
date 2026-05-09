package dev.voxelgame.client.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameAudioTest {
    @Test
    void appliesMasterAndChannelVolumeToPlayedCue() {
        GameAudio audio = new GameAudio();
        audio.setVolumes(0.5, 1.0, 0.4, 0.8, 0.2);

        audio.play(AudioCue.STEP_GRASS);
        assertEquals(AudioCue.STEP_GRASS, audio.lastCue());
        assertEquals(0.4, audio.lastGain(), 0.001);

        audio.play(AudioCue.DAY_AMBIENCE);
        assertEquals(AudioCue.DAY_AMBIENCE, audio.lastCue());
        assertEquals(0.2, audio.lastGain(), 0.001);

        audio.play(AudioCue.INVENTORY_CLICK);
        assertEquals(AudioCue.INVENTORY_CLICK, audio.lastCue());
        assertEquals(0.1, audio.lastGain(), 0.001);
    }

    @Test
    void mutedCueDoesNotReplaceLastAudibleCue() {
        GameAudio audio = new GameAudio();
        audio.play(AudioCue.BLOCK_PLACE);

        audio.setVolumes(0.0, 1.0, 1.0, 1.0, 1.0);
        audio.play(AudioCue.STEP_STONE);

        assertEquals(AudioCue.BLOCK_PLACE, audio.lastCue());
        assertEquals(0.0, audio.lastGain(), 0.001);
    }

    @Test
    void nullCueIsIgnoredWithoutInventingAudio() {
        GameAudio audio = new GameAudio();

        audio.play(null);

        assertNull(audio.lastCue());
        assertEquals(0.0, audio.lastGain(), 0.001);
    }
}
