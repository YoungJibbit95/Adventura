package dev.voxelgame.client.hud;

import dev.voxelgame.client.InteractionHint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionHudCardTest {
    @Test
    void keepsLongInteractionTextInsideViewportWidth() {
        InteractionHint hint = new InteractionHint(
                "Very long block name with decorative words",
                "Right click with a very specific contextual action",
                "This detail line is intentionally long so the card has to clamp it",
                InteractionHint.Tone.READY,
                0.42f,
                java.util.List.of("requires station", "long chip label")
        );

        InteractionHudCard card = InteractionHudCard.from(hint, 2.0f, 360);

        assertTrue(card.width() <= 360.0f);
        assertTrue(card.height() > 0.0f);
        assertTrue(card.progressVisible());
        assertFalse(card.title().isBlank());
        assertFalse(card.action().isBlank());
    }

    @Test
    void hidesProgressWhenHintHasNoProgress() {
        InteractionHint hint = new InteractionHint("Crate", "Right click open", "Shift-click moves stacks", InteractionHint.Tone.READY);

        InteractionHudCard card = InteractionHudCard.from(hint, 1.0f, 1280);

        assertFalse(card.progressVisible());
        assertTrue(card.height() > 40.0f);
    }
}
