package dev.voxelgame.common.block;

import dev.voxelgame.common.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockDropRegistryTest {
    @Test
    void everyBlockDropResolvesToRegisteredItem() {
        var blocks = Blocks.createDefaultRegistry();
        var items = Items.createDefaultRegistry();

        for (BlockType block : blocks.values()) {
            String drop = block.dropItemKey();
            if (drop != null && !drop.isBlank()) {
                assertTrue(items.findByKey(drop).isPresent(), block.key() + " drops missing item " + drop);
            }
        }
    }
}
