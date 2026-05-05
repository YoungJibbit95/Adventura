package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public static CompletableFuture<SaveQueue.SaveResult> saveQueued(
            SaveQueue saveQueue,
            Path directory,
            PlayerSave save
    ) {
        Objects.requireNonNull(saveQueue, "saveQueue");
        Objects.requireNonNull(save, "save");
        Path path = pathFor(directory, save.playerId());
        return saveQueue.enqueue(saveKey(path), () -> {
            save(directory, save);
            return Files.exists(path) ? Files.size(path) : 0L;
        });
    }

    public static Path pathFor(Path directory, UUID playerId) {
        return directory.resolve(playerId + ".properties");
    }

    private static String saveKey(Path path) {
        return "player:" + path.toAbsolutePath().normalize();
    }
}
