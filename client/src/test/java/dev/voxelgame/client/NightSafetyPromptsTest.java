package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightSafetyPromptsTest {
    @Test
    void firstDuskWithoutCampfireWarnsOnce() {
        NightSafetyPrompts prompts = new NightSafetyPrompts();

        assertEquals(Optional.of("Dusk is coming. Build and light a campfire."), prompts.update(1, 17 * 60, false));
        assertTrue(prompts.update(1, 17 * 60 + 30, false).isEmpty());
    }

    @Test
    void firstNightWithoutCampfireWarnsOnce() {
        NightSafetyPrompts prompts = new NightSafetyPrompts();

        assertEquals(Optional.of("Night is dark. Stay near a lit campfire."), prompts.update(1, 19 * 60, false));
        assertTrue(prompts.update(1, 20 * 60, false).isEmpty());
    }

    @Test
    void activeCampfireAtNightShowsSafeSpotOnce() {
        NightSafetyPrompts prompts = new NightSafetyPrompts();

        assertEquals(Optional.of("Campfire glow makes this spot safer tonight."), prompts.update(1, 19 * 60, true));
        assertTrue(prompts.update(1, 20 * 60, true).isEmpty());
    }

    @Test
    void laterDaysDoNotReplayFirstNightGuidance() {
        NightSafetyPrompts prompts = new NightSafetyPrompts();

        assertTrue(prompts.update(2, 19 * 60, false).isEmpty());
    }

    @Test
    void resetAllowsGuidanceForNewSession() {
        NightSafetyPrompts prompts = new NightSafetyPrompts();

        prompts.update(1, 17 * 60, false);
        prompts.reset();

        assertEquals(Optional.of("Dusk is coming. Build and light a campfire."), prompts.update(1, 17 * 60, false));
    }
}
