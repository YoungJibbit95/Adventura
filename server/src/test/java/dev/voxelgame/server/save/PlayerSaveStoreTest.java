package dev.voxelgame.server.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSaveStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void saveCreatesBackupBeforeOverwritingOlderSaveVersion() throws Exception {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000321");
        Path savePath = PlayerSaveStore.pathFor(tempDir, playerId);
        Properties oldSave = new Properties();
        oldSave.setProperty("kind", "adventura-player");
        oldSave.setProperty("save.version", "0");
        oldSave.setProperty("player.id", playerId.toString());
        oldSave.setProperty("player.name", "Old Ada");
        writeProperties(savePath, oldSave);

        PlayerSaveStore.save(tempDir, new PlayerSave(
                SaveMetadata.CURRENT_SAVE_VERSION,
                playerId,
                "Ada",
                1.0,
                80.0,
                2.0,
                0.0f,
                0.0f,
                List.of(),
                0,
                PlayerSave.SurvivalStats.defaults(),
                PlayerSave.SpawnPoint.empty(),
                "survival",
                List.of(),
                List.of(),
                List.of(),
                "overworld"
        ));

        Path backupPath = SaveBackup.migrationBackupPath(savePath, 0);
        assertTrue(Files.exists(backupPath));
        assertEquals("0", readProperties(backupPath).getProperty("save.version"));
        assertEquals("Old Ada", readProperties(backupPath).getProperty("player.name"));
        assertEquals(Integer.toString(SaveMetadata.CURRENT_SAVE_VERSION), readProperties(savePath).getProperty("save.version"));
        assertEquals("Ada", readProperties(savePath).getProperty("player.name"));
    }

    private static void writeProperties(Path path, Properties properties) throws Exception {
        try (var output = Files.newOutputStream(path)) {
            properties.store(output, "test save");
        }
    }

    private static Properties readProperties(Path path) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }
}
