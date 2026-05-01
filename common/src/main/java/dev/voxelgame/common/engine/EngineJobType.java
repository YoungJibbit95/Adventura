package dev.voxelgame.common.engine;

import java.util.Locale;
import java.util.Optional;

public enum EngineJobType {
    CHUNK_GENERATE("chunk.generate", "GEN"),
    CHUNK_LIGHT("chunk.light", "LGT"),
    CHUNK_MESH("chunk.mesh", "MSH"),
    SAVE_WRITE("save.write", "SAV"),
    NET_ENCODE("net.encode", "NET");

    private final String key;
    private final String debugLabel;

    EngineJobType(String key, String debugLabel) {
        this.key = key;
        this.debugLabel = debugLabel;
    }

    public String key() {
        return key;
    }

    public String debugLabel() {
        return debugLabel;
    }

    public static Optional<EngineJobType> findByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (EngineJobType type : values()) {
            if (type.key.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
