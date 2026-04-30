package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSaveCodecTest {
    @Test
    void playerSaveRoundTripKeepsProgressButDoesNotPersistComfort() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short berries = items.requireByKey("voxel:berries").id();
        PlayerSave save = new PlayerSave(
                SaveMetadata.CURRENT_SAVE_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                "Ada",
                12.5,
                81.0,
                -3.25,
                45.0f,
                5.0f,
                List.of(new ItemStack(berries, 4)),
                2,
                new PlayerSave.SurvivalStats(17, 12, 8, 19),
                new PlayerSave.SpawnPoint(true, 1.5, 80.0, 1.5),
                "survival",
                List.of("voxel:herb_soup"),
                List.of("voxel:meadow"),
                List.of("found-camp"),
                "overworld"
        );

        Properties encoded = PlayerSaveCodec.encode(save, items);
        PlayerSave decoded = PlayerSaveCodec.decode(encoded, items);

        assertEquals(save.playerId(), decoded.playerId());
        assertEquals(save.x(), decoded.x(), 0.001);
        assertEquals(new ItemStack(berries, 4), decoded.inventory().getFirst());
        assertEquals(17, decoded.survival().health());
        assertEquals(List.of("voxel:herb_soup"), decoded.discoveredRecipes());
        assertFalse(encoded.containsKey("survival.comfort"));
    }

    @Test
    void playerSaveDecodeUsesAliasesAndDropsUnknownItems() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short berries = items.requireByKey("voxel:berries").id();
        Properties properties = new Properties();
        properties.setProperty("kind", "adventura-player");
        properties.setProperty("save.version", "1");
        properties.setProperty("player.id", "00000000-0000-0000-0000-000000000123");
        properties.setProperty("inventory.count", "2");
        properties.setProperty("inventory.0.item", "voxel:wild_berries");
        properties.setProperty("inventory.0.count", "3");
        properties.setProperty("inventory.0.damage", "0");
        properties.setProperty("inventory.1.item", "mod:missing_item");
        properties.setProperty("inventory.1.count", "5");
        properties.setProperty("inventory.1.damage", "0");

        PlayerSave decoded = PlayerSaveCodec.decode(properties, items);

        assertEquals(new ItemStack(berries, 3), decoded.inventory().get(0));
        assertTrue(decoded.inventory().get(1).isEmpty());
    }

    @Test
    void playerSaveDecodeRejectsMissingKindMarker() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("save.version", "1");

        assertThrows(IllegalArgumentException.class, () -> PlayerSaveCodec.decode(properties, items));
    }
}
