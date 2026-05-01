package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class PlayerSaveStore {
    private static final Registry<ItemType> ITEMS = Items.createDefaultRegistry();

    private PlayerSaveStore() {
    }

    public static Optional<PlayerSave> load(Path directory, UUID playerId) throws IOException {
        Path path = pathFor(directory, playerId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(PlayerSaveCodec.read(path, ITEMS));
    }

    public static void save(Path directory, PlayerSave save) throws IOException {
        Path path = pathFor(directory, save.playerId());
        if (Files.exists(path)) {
            Properties existing = PlayerSaveCodec.readProperties(path);
            SaveBackup.createBeforeMigrationIfNeeded(path, existing);
        }
        PlayerSaveCodec.write(path, save, ITEMS);
    }

    public static Path pathFor(Path directory, UUID playerId) {
        return directory.resolve(playerId + ".properties");
    }
}
