package dev.voxelgame.client;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class FeedbackLog {
    private static final int MAX_ENTRIES = 6;
    private static final double DEFAULT_DURATION_SECONDS = 3.4;

    private final Deque<Entry> entries = new ArrayDeque<>();

    public synchronized void add(String message, double nowSeconds) {
        double duration = isImportantMessage(message) ? DEFAULT_DURATION_SECONDS + 1.8 : DEFAULT_DURATION_SECONDS;
        add(message, nowSeconds, duration);
    }

    public synchronized void add(String message, double nowSeconds, double durationSeconds) {
        if (message == null || message.isBlank()) {
            return;
        }
        double expiresAt = nowSeconds + Math.max(0.2, durationSeconds);
        Entry existing = null;
        for (Entry entry : entries) {
            if (entry.expiresAtSeconds <= nowSeconds) {
                continue;
            }
            if (entry.message.equals(message)) {
                existing = entry;
            }
        }
        if (existing != null) {
            entries.remove(existing);
            entries.addLast(existing.refresh(expiresAt));
            return;
        }
        entries.add(new Entry(message, expiresAt));
        while (entries.size() > MAX_ENTRIES) {
            entries.pollFirst();
        }
    }

    public synchronized List<String> visible(double nowSeconds) {
        entries.removeIf(entry -> entry.expiresAtSeconds <= nowSeconds);
        List<String> visible = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            visible.add(entry.render());
        }
        return List.copyOf(visible);
    }

    public synchronized void clear() {
        entries.clear();
    }

    private static boolean isImportantMessage(String message) {
        String lower = message.toLowerCase();
        return lower.contains("unlock")
                || lower.contains("discovered")
                || lower.contains("lore")
                || lower.contains("comfort level");
    }

    private record Entry(String message, int count, double expiresAtSeconds) {
        private Entry(String message, double expiresAtSeconds) {
            this(message, 1, expiresAtSeconds);
        }

        private Entry refresh(double newExpiresAtSeconds) {
            return new Entry(message, count + 1, Math.max(expiresAtSeconds, newExpiresAtSeconds));
        }

        private String render() {
            return count <= 1 ? message : message + " x" + count;
        }
    }
}
