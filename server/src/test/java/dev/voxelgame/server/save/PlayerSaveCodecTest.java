package dev.voxelgame.server.save;

import dev.voxelgame.common.gameplay.status.StatusEffectSaveState;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

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
                List.of("voxel:first_supply", "voxel:first_food"),
                List.of("voxel:goal_first_camp"),
                List.of(
                        new StatusEffectSaveState("voxel:rested", 120.0, 1, 0.0),
                        new StatusEffectSaveState("voxel:chilled", 12.5, 2, 0.5)
                ),
                List.of(new PlayerSave.CreatureFriendshipState("voxel:cozy_sheep", 2, 1, 4L, 97_200L)),
                "overworld"
        );

        Properties encoded = PlayerSaveCodec.encode(save, items);
        PlayerSave decoded = PlayerSaveCodec.decode(encoded, items);

        assertEquals(save.playerId(), decoded.playerId());
        assertEquals(save.x(), decoded.x(), 0.001);
        assertEquals(new ItemStack(berries, 4), decoded.inventory().getFirst());
        assertEquals(17, decoded.survival().health());
        assertEquals(List.of("voxel:herb_soup"), decoded.discoveredRecipes());
        assertEquals(List.of("voxel:first_supply", "voxel:first_food"), decoded.achievedMilestones());
        assertEquals(List.of("voxel:goal_first_camp"), decoded.completedGoals());
        assertEquals(save.statusEffects(), decoded.statusEffects());
        assertEquals(save.creatureFriendships(), decoded.creatureFriendships());
        assertEquals("voxel:chilled", encoded.getProperty("status.effect.0.effectKey"));
        assertEquals("voxel:cozy_sheep", encoded.getProperty("creature.friendship.0.entity"));
        assertFalse(encoded.containsKey("survival.comfort"));
    }

    @Test
    void playerSaveWriteUsesAtomicRenameAndCleansTemporaryFile(@TempDir Path tempDir) throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        PlayerSave save = new PlayerSave(
                SaveMetadata.CURRENT_SAVE_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000456"),
                "Grace",
                1.0,
                82.0,
                3.0,
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
        );
        Path savePath = tempDir.resolve("players").resolve(save.playerId() + ".properties");

        PlayerSaveCodec.write(savePath, save, items);

        assertEquals(save.playerId(), PlayerSaveCodec.read(savePath, items).playerId());
        assertFalse(hasTempSaveFile(savePath.getParent()));
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
    void playerSaveDecodeRejectsUnknownStatusEffectKey() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("kind", "adventura-player");
        properties.setProperty("save.version", "1");
        properties.setProperty("player.id", "00000000-0000-0000-0000-000000000123");
        properties.setProperty("inventory.count", "0");
        properties.setProperty("status.effect.count", "1");
        properties.setProperty("status.effect.0.effectKey", "mod:unknown_status");
        properties.setProperty("status.effect.0.remainingSeconds", "4.0");
        properties.setProperty("status.effect.0.intensity", "1");
        properties.setProperty("status.effect.0.tickProgressSeconds", "0.0");

        assertThrows(IllegalArgumentException.class, () -> PlayerSaveCodec.decode(properties, items));
    }

    private static boolean hasTempSaveFile(Path directory) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp"));
        }
    }

    @Test
    void playerSaveDecodeRejectsMissingKindMarker() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Properties properties = new Properties();
        properties.setProperty("save.version", "1");

        assertThrows(IllegalArgumentException.class, () -> PlayerSaveCodec.decode(properties, items));
    }
}
