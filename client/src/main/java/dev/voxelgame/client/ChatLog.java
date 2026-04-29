package dev.voxelgame.client;

import java.util.ArrayList;
import java.util.List;

public final class ChatLog {
    private static final int MAX_ENTRIES = 80;
    private final List<String> entries = new ArrayList<>();

    public synchronized void add(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        entries.add(message);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized List<String> recent(int count) {
        int from = Math.max(0, entries.size() - count);
        return List.copyOf(entries.subList(from, entries.size()));
    }
}
