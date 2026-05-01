package dev.voxelgame.client.audio;

public final class GameAudio {
    private AudioCue lastCue;
    private boolean underwater;

    public void play(AudioCue cue) {
        lastCue = cue;
    }

    public AudioCue lastCue() {
        return lastCue;
    }

    public void setUnderwater(boolean underwater) {
        this.underwater = underwater;
    }

    public boolean underwater() {
        return underwater;
    }
}
