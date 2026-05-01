package dev.voxelgame.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionStatusCardTest {
    @Test
    void fitsStationStatusLinesInsideViewport() {
        InteractionStatusCard card = InteractionStatusCard.from(
                "CAMPFIRE ACTIVE",
                "BURN 12:34  HELD FUEL +3:00",
                "COOKING A VERY LONG RECIPE NAME WITH TOO MANY WORDS 88%",
                1.7f,
                360,
                0.88f
        );

        assertTrue(card.width() <= 360.0f);
        assertTrue(card.height() > 0.0f);
        assertTrue(card.progressVisible());
        assertFalse(card.extra().isBlank());
    }

    @Test
    void hidesProgressWhenNoStationProgressIsActive() {
        InteractionStatusCard card = InteractionStatusCard.from(
                "FORGE READY",
                "ORE, FUEL AND HEAT REQUIRED",
                "PRESS E TO FORGE",
                1.0f,
                1280,
                0.0f
        );

        assertFalse(card.progressVisible());
        assertTrue(card.height() >= 60.0f);
    }
}
