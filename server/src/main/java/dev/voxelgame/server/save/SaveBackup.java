package dev.voxelgame.server.save;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;

final class SaveBackup {
    private SaveBackup() {
    }

    static Optional<Path> createBeforeMigrationIfNeeded(Path path, Properties properties) throws IOException {
        int version = WorldSaveCodec.intValue(properties, "save.version", SaveMetadata.CURRENT_SAVE_VERSION);
        if (version >= SaveMetadata.CURRENT_SAVE_VERSION || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        Path backup = migrationBackupPath(path, version);
        if (Files.exists(backup)) {
            return Optional.of(backup);
        }
        Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
        return Optional.of(backup);
    }

    static Path migrationBackupPath(Path path, int fromVersion) {
        String fileName = path.getFileName().toString();
        return path.resolveSibling(fileName + ".v" + Math.max(0, fromVersion) + ".bak");
    }
}
