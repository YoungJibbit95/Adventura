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
        entries.removeIf(entry -> entry.expiresAtSeconds <= nowSeconds);
        double expiresAt = nowSeconds + Math.max(0.2, durationSeconds);
        int duplicateIndex = -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (!entry.message.equals(message)) {
                continue;
            }
            if (duplicateIndex == -1) {
                duplicateIndex = i;
                expiresAt = Math.max(entry.expiresAtSeconds, expiresAt);
                continue;
            }
            entries.remove(i);
        }
        if (duplicateIndex >= 0) {
            entries.set(duplicateIndex, new Entry(message, expiresAt));
            return;
        }
        entries.add(new Entry(message, expiresAt));
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
