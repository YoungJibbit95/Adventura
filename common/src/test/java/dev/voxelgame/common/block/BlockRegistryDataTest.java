package dev.voxelgame.common.block;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRegistryDataTest {
    @Test
    void defaultBlocksHaveUniqueIdsKeysAndDisplayableNames() {
        var blocks = Blocks.createDefaultRegistry();
        Set<Short> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (BlockType block : blocks.values()) {
            assertTrue(ids.add(block.id()), "Duplicate block id " + block.id());
            assertTrue(keys.add(block.key()), "Duplicate block key " + block.key());
            assertFalse(block.key().isBlank(), "Blank block key");
            assertFalse(displayName(block.key()).isBlank(), "Blank block display name " + block.key());
        }
    }

    @Test
    void oreProgressionBlocksRequirePickaxeAndToolLevel() {
        for (BlockType block : Blocks.createDefaultRegistry().values()) {
            if (!isProgressionOre(block)) {
                continue;
            }
            assertEquals(ToolType.PICKAXE, block.preferredTool(), block.key());
            assertTrue(block.requiredToolLevel() >= 1, block.key());
        }
    }

    private static boolean isProgressionOre(BlockType block) {
        return block.key().endsWith("_ore") || block.id() == Blocks.GLOW_CRYSTAL_NODE;
    }

    private static String displayName(String key) {
        int colon = key.indexOf(':');
        return (colon >= 0 ? key.substring(colon + 1) : key).replace('_', ' ').trim();
    }
}
