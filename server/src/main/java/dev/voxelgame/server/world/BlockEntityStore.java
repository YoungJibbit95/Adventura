package dev.voxelgame.server.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

public final class BlockEntityStore {
    private static final Comparator<EntrySnapshot> ENTRY_ORDER = Comparator
            .comparingInt(EntrySnapshot::x)
            .thenComparingInt(EntrySnapshot::y)
            .thenComparingInt(EntrySnapshot::z)
            .thenComparing(EntrySnapshot::typeKey);
    private static final Comparator<UnknownEntry> UNKNOWN_ORDER = Comparator
            .comparingInt(UnknownEntry::x)
            .thenComparingInt(UnknownEntry::y)
            .thenComparingInt(UnknownEntry::z)
            .thenComparing(UnknownEntry::typeKey);

    private final Map<Position, BlockEntityType> entries = new LinkedHashMap<>();
    private final List<UnknownEntry> unknownEntries = new ArrayList<>();

    public void clear() {
        entries.clear();
        unknownEntries.clear();
    }

    public Optional<BlockEntityType> sync(Position position, short blockId) {
        Objects.requireNonNull(position, "position");
        Optional<BlockEntityType> type = BlockEntityType.fromBlockId(blockId);
        if (type.isPresent()) {
            entries.put(position, type.get());
        } else {
            entries.remove(position);
        }
        return type;
    }

    public void ensure(Position position, BlockEntityType type) {
        entries.put(Objects.requireNonNull(position, "position"), Objects.requireNonNull(type, "type"));
    }

    public void remove(Position position) {
        entries.remove(Objects.requireNonNull(position, "position"));
    }

    public Optional<BlockEntityType> typeAt(Position position) {
        return Optional.ofNullable(entries.get(position));
    }

    public boolean has(Position position, BlockEntityType type) {
        return entries.get(position) == type;
    }

    public int knownCount() {
        return entries.size();
    }

    public int unknownCount() {
        return unknownEntries.size();
    }

    public Snapshot snapshot() {
        List<EntrySnapshot> known = entries.entrySet().stream()
                .map(entry -> new EntrySnapshot(
                        entry.getKey().x(),
                        entry.getKey().y(),
                        entry.getKey().z(),
                        entry.getValue().typeKey()
                ))
                .sorted(ENTRY_ORDER)
                .toList();
        List<UnknownEntry> unknown = unknownEntries.stream()
                .sorted(UNKNOWN_ORDER)
                .toList();
        return new Snapshot(known, unknown);
    }

    public void loadSnapshot(Snapshot snapshot) {
        clear();
        if (snapshot == null) {
            return;
        }
        for (EntrySnapshot entry : snapshot.entries()) {
            Position position = new Position(entry.x(), entry.y(), entry.z());
            BlockEntityType.fromTypeKey(entry.typeKey())
                    .ifPresentOrElse(
                            type -> entries.put(position, type),
                            () -> unknownEntries.add(new UnknownEntry(entry.x(), entry.y(), entry.z(), entry.typeKey(), ""))
                    );
        }
        unknownEntries.addAll(snapshot.unknownEntries());
    }

    public void pruneInvalid(BiPredicate<Position, BlockEntityType> valid) {
        Objects.requireNonNull(valid, "valid");
        entries.entrySet().removeIf(entry -> !valid.test(entry.getKey(), entry.getValue()));
    }

    public record Position(int x, int y, int z) {
    }

    public record EntrySnapshot(int x, int y, int z, String typeKey) {
        public EntrySnapshot {
            typeKey = Objects.requireNonNull(typeKey, "typeKey");
        }
    }

    public record UnknownEntry(int x, int y, int z, String typeKey, String payload) {
        public UnknownEntry {
            typeKey = Objects.requireNonNull(typeKey, "typeKey");
            payload = payload == null ? "" : payload;
        }
    }

    public record Snapshot(List<EntrySnapshot> entries, List<UnknownEntry> unknownEntries) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of());

        public Snapshot {
            entries = entries == null ? List.of() : List.copyOf(entries);
            unknownEntries = unknownEntries == null ? List.of() : List.copyOf(unknownEntries);
        }
    }
}
