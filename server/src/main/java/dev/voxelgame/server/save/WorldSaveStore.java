package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.world.ServerWorld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldSaveStore {
    private static final Registry<ItemType> ITEMS = Items.createDefaultRegistry();

    private WorldSaveStore() {
    }

    public static WorldSave snapshot(ServerWorld world, double nowSeconds, long createdAtEpochMillis, long lastLoadedAtEpochMillis) {
        return new WorldSave(
                SaveMetadata.current(world.seed(), createdAtEpochMillis, lastLoadedAtEpochMillis, world.dayTimeTicks()),
                world.saveBlockDiffs(),
                world.saveBlockEntities(nowSeconds)
        );
    }

    public static void saveWorld(Path path, ServerWorld world, double nowSeconds) throws IOException {
        long now = System.currentTimeMillis();
        long createdAt = now;
        if (Files.exists(path)) {
            createdAt = WorldSaveCodec.read(path, ITEMS).metadata().createdAtEpochMillis();
        }
        WorldSaveCodec.write(path, snapshot(world, nowSeconds, createdAt, now), ITEMS);
    }

    public static ServerWorld loadWorld(Path path, double nowSeconds) throws IOException {
        return loadWorld(WorldSaveCodec.read(path, ITEMS), nowSeconds);
    }

    public static ServerWorld loadWorld(WorldSave save, double nowSeconds) {
        ServerWorld world = new ServerWorld(save.metadata().worldSeed());
        world.setDayTimeTicks(save.metadata().dayTimeTicks());
        world.loadBlockDiffs(save.blockChanges());
        world.loadBlockEntities(save.blockEntities(), nowSeconds);
        return world;
    }
}
