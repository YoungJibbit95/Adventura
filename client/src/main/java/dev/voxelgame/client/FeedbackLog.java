package dev.voxelgame.client;

import dev.voxelgame.client.animation.Easing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FeedbackLog {
    private static final int MAX_ENTRIES = 6;
    private static final double DEFAULT_DURATION_SECONDS = 3.4;
    private static final double FADE_SECONDS = 0.45;
    private static final double MIN_DURATION_SECONDS = 0.2;

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Double> repeatCooldowns = new HashMap<>();

    public synchronized void add(String message, double nowSeconds) {
        Kind kind = inferKind(message);
        add(message, nowSeconds, kind, kind.durationSeconds());
    }

    public synchronized void add(String message, double nowSeconds, double durationSeconds) {
        add(message, nowSeconds, inferKind(message), durationSeconds);
    }

    public synchronized void add(String message, double nowSeconds, Kind kind) {
        Kind safeKind = kind == null ? inferKind(message) : kind;
        add(message, nowSeconds, safeKind, safeKind.durationSeconds());
    }

    private void add(String message, double nowSeconds, Kind kind, double durationSeconds) {
        if (message == null || message.isBlank()) {
            return;
        }
        String normalizedMessage = message.strip();
        entries.removeIf(entry -> entry.expiresAtSeconds <= nowSeconds);
        repeatCooldowns.entrySet().removeIf(entry -> entry.getValue() <= nowSeconds);
        Double cooldownUntil = repeatCooldowns.get(normalizedMessage);
        if (cooldownUntil != null && cooldownUntil > nowSeconds) {
            return;
        }
        repeatCooldowns.put(normalizedMessage, nowSeconds + kind.repeatCooldownSeconds);

        double expiresAt = nowSeconds + Math.max(MIN_DURATION_SECONDS, durationSeconds);
        int duplicateIndex = -1;
        Entry primaryDuplicate = null;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (!entry.message.equals(normalizedMessage)) {
                continue;
            }
            if (primaryDuplicate == null) {
                duplicateIndex = i;
                primaryDuplicate = entry;
                expiresAt = Math.max(entry.expiresAtSeconds, expiresAt);
                continue;
            }
            primaryDuplicate = primaryDuplicate.absorb(entry);
            entries.remove(i);
            if (i < duplicateIndex) {
                duplicateIndex--;
            }
        }
        if (duplicateIndex >= 0) {
            entries.set(duplicateIndex, primaryDuplicate.refresh(expiresAt, kind));
            return;
        }
        entries.add(new Entry(normalizedMessage, kind, expiresAt));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(lowestPriorityEntryIndex());
        }
    }

    public synchronized List<String> visible(double nowSeconds) {
        return visibleEntries(nowSeconds).stream()
                .map(VisibleEntry::message)
                .toList();
    }

    public synchronized List<VisibleEntry> visibleEntries(double nowSeconds) {
        entries.removeIf(entry -> entry.expiresAtSeconds <= nowSeconds);
        List<VisibleEntry> visible = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            visible.add(new VisibleEntry(entry.render(), fadeAlpha(entry.expiresAtSeconds - nowSeconds), entry.kind));
        }
        return List.copyOf(visible);
    }

    public synchronized void clear() {
        entries.clear();
        repeatCooldowns.clear();
    }

    private int lowestPriorityEntryIndex() {
        int lowestIndex = 0;
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).kind.priority < entries.get(lowestIndex).kind.priority) {
                lowestIndex = i;
            }
        }
        return lowestIndex;
    }

    private static Kind inferKind(String message) {
        if (message == null || message.isBlank()) {
            return Kind.INFO;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("unlock")
                || lower.contains("recipes available")
                || lower.contains("new recipe")) {
            return Kind.UNLOCK;
        }
        if (lower.contains("lore")
                || lower.contains("journal")
                || lower.contains("discovered")
                || lower.contains("first supply")) {
            return Kind.DISCOVERY;
        }
        if (lower.contains("comfort")
                || lower.contains("cozy")
                || lower.contains("warmth")) {
            return Kind.COMFORT;
        }
        if (lower.contains("full")
                || lower.contains("need")
                || lower.contains("can't")
                || lower.contains("failed")
                || lower.contains("blocked")
                || lower.contains("too far")
                || lower.contains("missing")
                || lower.contains("unavailable")
                || lower.contains("died")) {
            return Kind.WARNING;
        }
        if (lower.contains("crafted")
                || lower.contains("gathered")
                || lower.contains("opened")
                || lower.contains("stored")
                || lower.contains("moved")
                || lower.contains("sorted")
                || lower.contains("connected")
                || lower.contains("loaded")
                || lower.contains("fueled")
                || lower.contains("lit")) {
            return Kind.SUCCESS;
        }
        return Kind.INFO;
    }

    private static float fadeAlpha(double remainingSeconds) {
        if (remainingSeconds >= FADE_SECONDS) {
            return 1.0f;
        }
        return Easing.smoothStep(remainingSeconds / FADE_SECONDS);
    }

    public enum Kind {
        INFO(10, 0.0, 0.35),
        SUCCESS(20, 0.35, 0.35),
        COMFORT(30, 1.1, 0.45),
        WARNING(40, 0.3, 0.35),
        DISCOVERY(50, 1.6, 0.20),
        UNLOCK(60, 1.8, 0.20);

        private final int priority;
        private final double durationBonusSeconds;
        private final double repeatCooldownSeconds;

        Kind(int priority, double durationBonusSeconds, double repeatCooldownSeconds) {
            this.priority = priority;
            this.durationBonusSeconds = durationBonusSeconds;
            this.repeatCooldownSeconds = repeatCooldownSeconds;
        }

        public int priority() {
            return priority;
        }

        private double durationSeconds() {
            return DEFAULT_DURATION_SECONDS + durationBonusSeconds;
        }
    }

    public record VisibleEntry(String message, float alpha, Kind kind) {
        public VisibleEntry(String message, float alpha) {
            this(message, alpha, Kind.INFO);
        }

        public VisibleEntry {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Visible feedback message is required");
            }
            if (alpha < 0.0f || alpha > 1.0f) {
                throw new IllegalArgumentException("Visible feedback alpha must be in 0..1");
            }
            if (kind == null) {
                throw new IllegalArgumentException("Visible feedback kind is required");
            }
        }
    }

    private record Entry(String message, Kind kind, int count, double expiresAtSeconds) {
        private Entry(String message, Kind kind, double expiresAtSeconds) {
            this(message, kind, 1, expiresAtSeconds);
        }

        private Entry refresh(double newExpiresAtSeconds, Kind newKind) {
            Kind strongestKind = newKind.priority > kind.priority ? newKind : kind;
            return new Entry(message, strongestKind, count + 1, Math.max(expiresAtSeconds, newExpiresAtSeconds));
        }

        private Entry absorb(Entry duplicate) {
            Kind strongestKind = duplicate.kind.priority > kind.priority ? duplicate.kind : kind;
            return new Entry(message, strongestKind, count + duplicate.count, Math.max(expiresAtSeconds, duplicate.expiresAtSeconds));
        }

        private String render() {
            return count <= 1 ? message : message + " x" + count;
        }
    }
}
