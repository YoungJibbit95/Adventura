package dev.voxelgame.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComfortHudInfoTest {
    @Test
    void mapsComfortValuesToCozyLevels() {
        assertEquals(ComfortHudInfo.Level.NONE, ComfortHudInfo.levelFor(0));
        assertEquals(ComfortHudInfo.Level.LOW, ComfortHudInfo.levelFor(1));
        assertEquals(ComfortHudInfo.Level.COZY, ComfortHudInfo.levelFor(5));
        assertEquals(ComfortHudInfo.Level.WARM, ComfortHudInfo.levelFor(8));
        assertEquals(ComfortHudInfo.Level.RESTFUL, ComfortHudInfo.levelFor(12));
        assertEquals(ComfortHudInfo.Level.HOMEY, ComfortHudInfo.levelFor(16));
    }

    @Test
    void summarizesEffectsAndNearbySources() {
        ComfortHudInfo info = ComfortHudInfo.of(8, List.of(
                new ComfortHudInfo.Source("Campfire", 5),
                new ComfortHudInfo.Source("Rug", 3)
        ));

        assertEquals("WARM +8", info.statLabel().toUpperCase());
        assertTrue(info.detailLine().contains("stamina +"));
        assertTrue(info.detailLine().contains("Campfire +5"));
        assertTrue(info.detailLine().contains("Rug +3"));
    }

    @Test
    void describesFriendlyAnimalSourcesWithoutFakeComfortValue() {
        ComfortHudInfo info = ComfortHudInfo.of(5, List.of(
                new ComfortHudInfo.Source("Friendly Sheep", 0)
        ));

        assertTrue(info.detailLine().contains("Friendly Sheep"));
        assertTrue(!info.detailLine().contains("Friendly Sheep +0"));
    }
}
