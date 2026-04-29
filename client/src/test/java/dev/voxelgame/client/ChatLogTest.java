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
}
