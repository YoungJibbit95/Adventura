package dev.voxelgame.client.viewmodel;

import java.util.Objects;

public record LoadingScreenViewModel(
        Phase phase,
        String title,
        String detail,
        double progress,
        boolean indeterminate,
        boolean cancelAvailable,
        boolean error
) {
    public LoadingScreenViewModel {
        Objects.requireNonNull(phase, "phase");
        title = requireText(title, "title");
        detail = detail == null ? "" : detail.strip();
        if (!Double.isFinite(progress) || progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress must be in [0, 1]");
        }
        if (error && phase != Phase.ERROR) {
            throw new IllegalArgumentException("error view model must use ERROR phase");
        }
    }

    public static LoadingScreenViewModel boot() {
        return new LoadingScreenViewModel(Phase.BOOT, "Starting Adventura", "Preparing engine", 0.05, true, false, false);
    }

    public static LoadingScreenViewModel loadingWorld(double progress) {
        return new LoadingScreenViewModel(Phase.LOADING_WORLD, "Loading World", "Generating nearby terrain", clamp(progress), false, false, false);
    }

    public static LoadingScreenViewModel joiningServer(String endpoint) {
        String target = endpoint == null || endpoint.isBlank() ? "server" : endpoint.strip();
        return new LoadingScreenViewModel(Phase.JOINING_SERVER, "Joining Server", "Connecting to " + target, 0.15, true, true, false);
    }

    public static LoadingScreenViewModel streamingSpawn(int loadedChunks, int requiredChunks) {
        int required = Math.max(1, requiredChunks);
        int loaded = Math.max(0, Math.min(loadedChunks, required));
        double progress = loaded / (double) required;
        return new LoadingScreenViewModel(
                Phase.STREAMING_SPAWN,
                "Streaming Spawn",
                loaded + " / " + required + " chunks ready",
                progress,
                false,
                false,
                false
        );
    }

    public static LoadingScreenViewModel error(String message) {
        String detail = message == null || message.isBlank() ? "Unknown loading error" : message.strip();
        return new LoadingScreenViewModel(Phase.ERROR, "Could Not Load", detail, 1.0, false, true, true);
    }

    public boolean complete() {
        return !error && !indeterminate && progress >= 1.0;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public enum Phase {
        BOOT,
        LOADING_WORLD,
        JOINING_SERVER,
        STREAMING_SPAWN,
        ERROR
    }
}
