package dev.voxelgame.client.audio;

public final class GameAudio {
    private AudioCue lastCue;
    private double lastGain = 1.0;
    private double masterVolume = 1.0;
    private double musicVolume = 0.8;
    private double ambienceVolume = 0.85;
    private double sfxVolume = 1.0;
    private double uiVolume = 0.9;
    private boolean underwater;

    public void play(AudioCue cue) {
        lastGain = volumeFor(cue);
        if (lastGain <= 0.0) {
            return;
        }
        lastCue = cue;
    }

    public AudioCue lastCue() {
        return lastCue;
    }

    public double lastGain() {
        return lastGain;
    }

    public void setVolumes(double masterVolume, double musicVolume, double ambienceVolume, double sfxVolume, double uiVolume) {
        this.masterVolume = clamp01(masterVolume);
        this.musicVolume = clamp01(musicVolume);
        this.ambienceVolume = clamp01(ambienceVolume);
        this.sfxVolume = clamp01(sfxVolume);
        this.uiVolume = clamp01(uiVolume);
    }

    public void setUnderwater(boolean underwater) {
        this.underwater = underwater;
    }

    public boolean underwater() {
        return underwater;
    }

    private double volumeFor(AudioCue cue) {
        if (cue == null) {
            return 0.0;
        }
        return masterVolume * switch (channelFor(cue)) {
            case MUSIC -> musicVolume;
            case AMBIENCE -> ambienceVolume;
            case UI -> uiVolume;
            case SFX -> sfxVolume;
        };
    }

    private static AudioChannel channelFor(AudioCue cue) {
        return switch (cue) {
            case CAMPFIRE_CRACKLE, DAY_AMBIENCE, NIGHT_AMBIENCE, MEADOW_BIRDS, CAVE_DRIP, THUNDER -> AudioChannel.AMBIENCE;
            case INVENTORY_CLICK -> AudioChannel.UI;
            default -> AudioChannel.SFX;
        };
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private enum AudioChannel {
        MUSIC,
        AMBIENCE,
        SFX,
        UI
    }
}
