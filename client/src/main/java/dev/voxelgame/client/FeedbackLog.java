package dev.voxelgame.client;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

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
        String lower = message.toLowerCase(Locale.ROOT);
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
