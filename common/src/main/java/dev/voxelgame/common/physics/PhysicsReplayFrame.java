package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record PhysicsReplayFrame(
        int schemaVersion,
        long tick,
        PhysicsConfigSnapshot config,
        Input input,
        PlayerState playerState,
        List<EntitySnapshot> entityStates,
        List<ProjectileState> projectileStates,
        List<BlockSample> blockSamples,
        List<Event> events,
        Map<String, Long> stats
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Comparator<EntitySnapshot> ENTITY_ORDER = Comparator
            .comparingLong(EntitySnapshot::entityId)
            .thenComparing(EntitySnapshot::typeKey);
    private static final Comparator<ProjectileState> PROJECTILE_ORDER = Comparator
            .comparingLong(ProjectileState::projectileId)
            .thenComparing(ProjectileState::typeKey);
    private static final Comparator<BlockSample> BLOCK_SAMPLE_ORDER = Comparator
            .comparingInt(BlockSample::x)
            .thenComparingInt(BlockSample::y)
            .thenComparingInt(BlockSample::z)
            .thenComparingInt(BlockSample::blockId);

    public PhysicsReplayFrame {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported physics replay schema version: " + schemaVersion);
        }
        if (tick < 0L) {
            throw new IllegalArgumentException("Physics replay tick must be non-negative");
        }
        config = Objects.requireNonNull(config, "config");
        input = input == null ? Input.idle() : input;
        entityStates = sortedCopy(entityStates, ENTITY_ORDER, "entityStates");
        projectileStates = sortedCopy(projectileStates, PROJECTILE_ORDER, "projectileStates");
        blockSamples = sortedCopy(blockSamples, BLOCK_SAMPLE_ORDER, "blockSamples");
        events = checkedCopy(events, "events");
        stats = checkedStats(stats);
    }

    public static Builder tick(long tick) {
        return new Builder(tick);
    }

    public static Builder fromContext(PhysicsStepContext context) {
        Objects.requireNonNull(context, "context");
        return tick(context.tick()).input(0L, PlayerInput.idle(), context.waterState());
    }

    private static <T> List<T> sortedCopy(List<T> values, Comparator<T> comparator, String label) {
        Objects.requireNonNull(values, label);
        ArrayList<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            copy.add(Objects.requireNonNull(value, label + " entry"));
        }
        copy.sort(comparator);
        return List.copyOf(copy);
    }

    private static <T> List<T> checkedCopy(List<T> values, String label) {
        Objects.requireNonNull(values, label);
        ArrayList<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            copy.add(Objects.requireNonNull(value, label + " entry"));
        }
        return List.copyOf(copy);
    }

    private static Map<String, Long> checkedStats(Map<String, Long> values) {
        Objects.requireNonNull(values, "stats");
        TreeMap<String, Long> sorted = new TreeMap<>();
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            String key = requireKey("stats key", entry.getKey());
            Long value = Objects.requireNonNull(entry.getValue(), "stats value");
            if (value < 0L) {
                throw new IllegalArgumentException("Physics replay stat must be non-negative: " + key);
            }
            sorted.put(key, value);
        }
        return Collections.unmodifiableMap(sorted);
    }

    private static String requireKey(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    public record Input(long sequence, PlayerInput playerInput, PlayerWaterState waterState) {
        public Input {
            if (sequence < 0L) {
                throw new IllegalArgumentException("Physics replay input sequence must be non-negative");
            }
            playerInput = playerInput == null ? PlayerInput.idle() : playerInput;
            waterState = waterState == null ? new PlayerWaterState(false, false, false) : waterState;
        }

        public static Input idle() {
            return new Input(0L, PlayerInput.idle(), new PlayerWaterState(false, false, false));
        }
    }

    public record BlockSample(int x, int y, int z, short blockId) {
        public BlockSample {
            if (blockId < 0) {
                throw new IllegalArgumentException("Physics replay block id must be non-negative");
            }
        }
    }

    public record Event(String type, Map<String, String> data) {
        public Event {
            type = requireKey("event type", type);
            Objects.requireNonNull(data, "data");
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = requireKey("event data key", entry.getKey());
                String value = Objects.requireNonNull(entry.getValue(), "event data value");
                sorted.put(key, value);
            }
            data = Collections.unmodifiableMap(sorted);
        }

        public static Event projectileHit(ProjectileHit hit) {
            Objects.requireNonNull(hit, "hit");
            ProjectileState state = hit.state();
            Map<String, String> data = new LinkedHashMap<>();
            data.put("projectileId", Long.toString(state.projectileId()));
            data.put("hitType", hit.type().name());
            data.put("blockX", Integer.toString(hit.blockX()));
            data.put("blockY", Integer.toString(hit.blockY()));
            data.put("blockZ", Integer.toString(hit.blockZ()));
            data.put("entityId", Long.toString(hit.entityId()));
            data.put("impactX", Double.toString(hit.impactX()));
            data.put("impactY", Double.toString(hit.impactY()));
            data.put("impactZ", Double.toString(hit.impactZ()));
            data.put("blockFace", hit.blockFace().name());
            return new Event("projectile_hit", data);
        }
    }

    public static final class Builder {
        private final long tick;
        private PhysicsConfigSnapshot config = PhysicsConfigSnapshot.current();
        private Input input = Input.idle();
        private PlayerState playerState;
        private final List<EntitySnapshot> entityStates = new ArrayList<>();
        private final List<ProjectileState> projectileStates = new ArrayList<>();
        private final List<BlockSample> blockSamples = new ArrayList<>();
        private final List<Event> events = new ArrayList<>();
        private final Map<String, Long> stats = new LinkedHashMap<>();

        private Builder(long tick) {
            this.tick = tick;
        }

        public Builder config(PhysicsConfigSnapshot config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        public Builder input(long sequence, PlayerInput playerInput, PlayerWaterState waterState) {
            this.input = new Input(sequence, playerInput, waterState);
            return this;
        }

        public Builder input(Input input) {
            this.input = Objects.requireNonNull(input, "input");
            return this;
        }

        public Builder playerState(PlayerState playerState) {
            this.playerState = playerState;
            return this;
        }

        public Builder entity(EntitySnapshot entityState) {
            entityStates.add(Objects.requireNonNull(entityState, "entityState"));
            return this;
        }

        public Builder projectile(ProjectileState projectileState) {
            projectileStates.add(Objects.requireNonNull(projectileState, "projectileState"));
            return this;
        }

        public Builder blockSample(int x, int y, int z, short blockId) {
            blockSamples.add(new BlockSample(x, y, z, blockId));
            return this;
        }

        public Builder event(Event event) {
            events.add(Objects.requireNonNull(event, "event"));
            return this;
        }

        public Builder projectileHit(ProjectileHit hit) {
            return event(Event.projectileHit(hit));
        }

        public Builder stat(String key, long value) {
            stats.put(requireKey("stat key", key), value);
            return this;
        }

        public PhysicsReplayFrame build() {
            return new PhysicsReplayFrame(
                    CURRENT_SCHEMA_VERSION,
                    tick,
                    config,
                    input,
                    playerState,
                    entityStates,
                    projectileStates,
                    blockSamples,
                    events,
                    stats
            );
        }
    }
}
