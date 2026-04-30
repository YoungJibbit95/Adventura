package dev.voxelgame.server.save;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.world.ServerWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSaveStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void worldSaveRoundTripRestoresDiffsStorageCampfireAndLootState() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory player = new Inventory(36);
        ServerWorld source = new ServerWorld(987L);
        source.setDayTimeTicks(14_250L);
        source.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        source.setBlock(3, 80, 2, Blocks.CAMPFIRE);
        source.setBlock(4, 80, 2, Blocks.STONE);
        source.setBlock(5, 80, 2, Blocks.DIRT);
        source.setBlock(5, 80, 2, Blocks.AIR);
        player.add(dirt, 11, items);
        source.transferStorageStack(2, 80, 2, player, items, false, 0).orElseThrow();
        source.fuelCampfire(3, 80, 2, 10.0, 25.0).orElseThrow();

        Path savePath = tempDir.resolve("world.properties");
        WorldSaveStore.saveWorld(savePath, source, 15.0);

        ServerWorld loaded = WorldSaveStore.loadWorld(savePath, 100.0);

        assertEquals(987L, loaded.seed());
        assertEquals(14_250L, loaded.dayTimeTicks());
        assertEquals(Blocks.STONE, loaded.blockAt(4, 80, 2).orElseThrow().id());
        assertEquals(Blocks.AIR, loaded.blockAt(5, 80, 2).orElseThrow().id());
        assertEquals(new ItemStack(dirt, 11), loaded.openStorageCrate(2, 80, 2).orElseThrow().getFirst());
        assertEquals(20.0, loaded.campfireFuelSecondsRemaining(3, 80, 2, 100.0).orElseThrow(), 0.001);
    }

    @Test
    void worldSaveDecodeFallsBackForUnknownBlocksAndItems() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("kind", "adventura-world");
        properties.setProperty("save.version", "1");
        properties.setProperty("world.seed", "42");
        properties.setProperty("block.count", "1");
        properties.setProperty("block.0.x", "1");
        properties.setProperty("block.0.y", "80");
        properties.setProperty("block.0.z", "1");
        properties.setProperty("block.0.key", "mod:unknown_block");
        properties.setProperty("storage.count", "1");
        properties.setProperty("storage.0.x", "2");
        properties.setProperty("storage.0.y", "80");
        properties.setProperty("storage.0.z", "2");
        properties.setProperty("storage.0.slot.count", Integer.toString(ServerWorld.STORAGE_CRATE_SLOTS));
        properties.setProperty("storage.0.slot.0.item", "mod:unknown_item");
        properties.setProperty("storage.0.slot.0.count", "9");
        properties.setProperty("storage.0.slot.0.damage", "0");

        WorldSave decoded = WorldSaveCodec.decode(properties, items);
        ServerWorld loaded = WorldSaveStore.loadWorld(decoded, 0.0);

        assertEquals(Blocks.AIR, loaded.blockAt(1, 80, 1).orElseThrow().id());
        assertTrue(decoded.blockEntities().storageCrates().getFirst().slots().getFirst().isEmpty());
    }

    @Test
    void worldSaveKeepsUnknownBlockEntitiesInertInFileModel() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("kind", "adventura-world");
        properties.setProperty("save.version", "1");
        properties.setProperty("world.seed", "42");
        properties.setProperty("be.count", "1");
        properties.setProperty("be.0.x", "2");
        properties.setProperty("be.0.y", "80");
        properties.setProperty("be.0.z", "2");
        properties.setProperty("be.0.type", "mod:future_station");

        WorldSave decoded = WorldSaveCodec.decode(properties, items);

        assertFalse(decoded.blockEntities().blockEntities().unknownEntries().isEmpty());
    }

    @Test
    void worldSaveDecodeRejectsMissingKindMarker() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("save.version", "1");
        properties.setProperty("world.seed", "42");

        assertThrows(IllegalArgumentException.class, () -> WorldSaveCodec.decode(properties, items));
    }
}
