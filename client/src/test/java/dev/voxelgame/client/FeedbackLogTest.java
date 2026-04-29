package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
