package dev.voxelgame.client;

import java.util.ArrayList;
import java.util.List;

public final class FeedbackLog {
    private static final int MAX_ENTRIES = 6;
    private static final double DEFAULT_DURATION_SECONDS = 3.4;

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void add(String message, double nowSeconds) {
        add(message, nowSeconds, DEFAULT_DURATION_SECONDS);
    }

    public synchronized void add(String message, double nowSeconds, double durationSeconds) {
        if (message == null || message.isBlank()) {
            return;
        }
        entries.add(new Entry(message, nowSeconds + Math.max(0.2, durationSeconds)));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    public synchronized List<String> visible(double nowSeconds) {
        entries.removeIf(entry -> entry.expiresAtSeconds <= nowSeconds);
        List<String> visible = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            visible.add(entry.message);
        }
        return List.copyOf(visible);
    }

    public synchronized void clear() {
        entries.clear();
    }

    private record Entry(String message, double expiresAtSeconds) {
    }
}
