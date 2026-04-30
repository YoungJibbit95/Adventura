package dev.voxelgame.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ChatLog {
    private static final int MAX_ENTRIES = 80;
    private final Deque<String> entries = new ArrayDeque<>();

    public synchronized void add(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        entries.add(message);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollFirst();
        }
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized List<String> recent(int count) {
        if (count <= 0 || entries.isEmpty()) {
            return List.of();
        }
        int keep = Math.min(count, entries.size());
        int skip = entries.size() - keep;
        List<String> recent = new ArrayList<>(keep);
        int index = 0;
        for (String entry : entries) {
            if (index++ >= skip) {
                recent.add(entry);
            }
        }
        return List.copyOf(recent);
    }
}
