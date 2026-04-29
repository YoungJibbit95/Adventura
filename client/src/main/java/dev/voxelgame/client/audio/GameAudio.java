package dev.voxelgame.client.audio;

public final class GameAudio {
    private AudioCue lastCue;

    public void play(AudioCue cue) {
        lastCue = cue;
    }

    public AudioCue lastCue() {
        return lastCue;
    }
}
