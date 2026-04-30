package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEntityStoreTest {
    @Test
    void syncCreatesUpdatesAndDeletesKnownBlockEntities() {
        BlockEntityStore store = new BlockEntityStore();
        BlockEntityStore.Position position = new BlockEntityStore.Position(1, 80, 2);

        store.sync(position, Blocks.STORAGE_CRATE);

        assertEquals(BlockEntityType.STORAGE_CRATE, store.typeAt(position).orElseThrow());

        store.sync(position, Blocks.AIR);

        assertTrue(store.typeAt(position).isEmpty());
    }

    @Test
    void loadSnapshotKeepsUnknownEntriesInactive() {
        BlockEntityStore store = new BlockEntityStore();

        store.loadSnapshot(new BlockEntityStore.Snapshot(
                List.of(
                        new BlockEntityStore.EntrySnapshot(1, 80, 2, BlockEntityType.CAMPFIRE.typeKey()),
                        new BlockEntityStore.EntrySnapshot(2, 80, 2, "mod:future_station")
                ),
                List.of(new BlockEntityStore.UnknownEntry(3, 80, 2, "mod:future_storage", "payload"))
        ));

        assertEquals(1, store.knownCount());
        assertEquals(2, store.unknownCount());
        assertEquals(BlockEntityType.CAMPFIRE, store.typeAt(new BlockEntityStore.Position(1, 80, 2)).orElseThrow());
        assertTrue(store.typeAt(new BlockEntityStore.Position(2, 80, 2)).isEmpty());
    }
}
