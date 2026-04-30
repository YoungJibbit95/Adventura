package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLogTest {
    @Test
    void storesRecentMessagesAndClears() {
        ChatLog log = new ChatLog();

        log.add("hello");
        log.add("world");

        assertEquals(2, log.recent(10).size());
        assertEquals("world", log.recent(1).getFirst());

        log.clear();
        assertTrue(log.recent(10).isEmpty());
    }

    @Test
    void keepsOnlyMostRecentEntriesAndHandlesNonPositiveRequests() {
        ChatLog log = new ChatLog();
        for (int i = 1; i <= 100; i++) {
            log.add("msg-" + i);
        }

        assertEquals(80, log.recent(200).size());
        assertEquals("msg-21", log.recent(200).getFirst());
        assertEquals("msg-100", log.recent(1).getFirst());
        assertTrue(log.recent(0).isEmpty());
        assertTrue(log.recent(-5).isEmpty());
    }
}
