package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackLogTest {
    @Test
    void visibleReturnsUnexpiredMessagesOnly() {
        FeedbackLog log = new FeedbackLog();

        log.add("One", 1.0, 1.0);
        log.add("Two", 1.5, 3.0);

        assertEquals(List.of("Two"), log.visible(2.2));
    }

    @Test
    void addKeepsMostRecentEntries() {
        FeedbackLog log = new FeedbackLog();

        for (int i = 0; i < 8; i++) {
            log.add("Message " + i, 0.0, 10.0);
        }

        assertEquals(List.of("Message 2", "Message 3", "Message 4", "Message 5", "Message 6", "Message 7"), log.visible(1.0));
    }

    @Test
    void duplicateActiveMessagesAreMergedAndDurationIsRefreshed() {
        FeedbackLog log = new FeedbackLog();

        log.add("Inventory Full", 10.0, 1.0);
        log.add("Inventory Full", 10.4, 2.0);

        assertEquals(List.of("Inventory Full x2"), log.visible(10.8));
        assertEquals(List.of("Inventory Full x2"), log.visible(12.0));
        assertEquals(List.of(), log.visible(12.5));
    }

    @Test
    void visibleEntriesFadeNearExpiration() {
        FeedbackLog log = new FeedbackLog();

        log.add("Almost gone", 10.0, 1.0);

        assertEquals(1.0f, log.visibleEntries(10.2).getFirst().alpha(), 0.001f);
        assertEquals(0.5f, log.visibleEntries(10.775).getFirst().alpha(), 0.001f);
        assertTrue(log.visibleEntries(10.95).getFirst().alpha() < 0.1f);
        assertEquals(List.of(), log.visibleEntries(11.0));
    }

    @Test
    void addPurgesExpiredEntriesBeforeApplyingCapacityLimit() {
        FeedbackLog log = new FeedbackLog();

        for (int i = 0; i < 6; i++) {
            log.add("Expired " + i, 0.0, 0.5);
        }
        log.add("Fresh", 1.0, 2.0);

        assertEquals(List.of("Fresh"), log.visible(1.1));
    }


    @Test
    void importantMessageDetectionIsLocaleStable() {
        FeedbackLog log = new FeedbackLog();
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            log.add("Item unlocked", 5.0);
            assertEquals(List.of("Item unlocked"), log.visible(9.0));
        } finally {
            Locale.setDefault(previous);
        }
        assertEquals(List.of(), log.visible(10.3));
    }
}
