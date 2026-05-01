package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PhysicsReplayCodec {
    public static final String FORMAT = "adventura.physics-replay.jsonl";

    private PhysicsReplayCodec() {
    }

    public static String writeJsonl(Collection<PhysicsReplayFrame> frames) {
        Objects.requireNonNull(frames, "frames");
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (PhysicsReplayFrame frame : frames) {
            if (!first) {
                builder.append('\n');
            }
            appendFrame(builder, Objects.requireNonNull(frame, "frame"));
            first = false;
        }
        return builder.toString();
    }

    public static List<PhysicsReplayFrame> readJsonl(String jsonl) {
        Objects.requireNonNull(jsonl, "jsonl");
        ArrayList<PhysicsReplayFrame> frames = new ArrayList<>();
        String[] lines = jsonl.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                frames.add(readFrame(line));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid physics replay JSONL at line " + (index + 1) + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(frames);
    }

    public static String writeFrame(PhysicsReplayFrame frame) {
        StringBuilder builder = new StringBuilder();
        appendFrame(builder, Objects.requireNonNull(frame, "frame"));
        return builder.toString();
    }

    public static PhysicsReplayFrame readFrame(String json) {
        return frameFrom(PhysicsReplayJson.parseObject(json));
    }

    private static void appendFrame(StringBuilder builder, PhysicsReplayFrame frame) {
        builder.append('{');
        appendName(builder, "schemaVersion");
        appendInt(builder, frame.schemaVersion());
        builder.append(',');
        appendName(builder, "tick");
        appendLong(builder, frame.tick());
        builder.append(',');
        appendName(builder, "config");
        appendConfig(builder, frame.config());
        builder.append(',');
        appendName(builder, "input");
        appendInput(builder, frame.input());
        builder.append(',');
        appendName(builder, "playerState");
        appendPlayerState(builder, frame.playerState());
        builder.append(',');
        appendName(builder, "entityStates");
        appendEntities(builder, frame.entityStates());
        builder.append(',');
        appendName(builder, "projectileStates");
        appendProjectiles(builder, frame.projectileStates());
        builder.append(',');
        appendName(builder, "blockSamples");
        appendBlockSamples(builder, frame.blockSamples());
        builder.append(',');
        appendName(builder, "events");
        appendEvents(builder, frame.events());
        builder.append(',');
        appendName(builder, "stats");
        appendLongMap(builder, frame.stats());
        builder.append('}');
    }

    private static void appendConfig(StringBuilder builder, PhysicsConfigSnapshot config) {
        builder.append('{');
        appendName(builder, "schemaVersion");
        appendInt(builder, config.schemaVersion());
        builder.append(',');
        appendName(builder, "playerFingerprint");
        appendString(builder, config.playerFingerprint());
        builder.append(',');
        appendName(builder, "projectileFingerprint");
        appendString(builder, config.projectileFingerprint());
        builder.append('}');
    }

    private static void appendInput(StringBuilder builder, PhysicsReplayFrame.Input input) {
        PlayerInput playerInput = input.playerInput();
        PlayerWaterState waterState = input.waterState();
        builder.append('{');
        appendName(builder, "sequence");
        appendLong(builder, input.sequence());
        builder.append(',');
        appendName(builder, "player");
        builder.append('{');
        appendName(builder, "moveX");
        appendFloat(builder, playerInput.moveX());
        builder.append(',');
        appendName(builder, "moveZ");
        appendFloat(builder, playerInput.moveZ());
        builder.append(',');
        appendName(builder, "jump");
        appendBoolean(builder, playerInput.jump());
        builder.append(',');
        appendName(builder, "descend");
        appendBoolean(builder, playerInput.descend());
        builder.append(',');
        appendName(builder, "sprint");
        appendBoolean(builder, playerInput.sprint());
        builder.append('}');
        builder.append(',');
        appendName(builder, "water");
        builder.append('{');
        appendName(builder, "feetInWater");
        appendBoolean(builder, waterState.feetInWater());
        builder.append(',');
        appendName(builder, "bodyInWater");
        appendBoolean(builder, waterState.bodyInWater());
        builder.append(',');
        appendName(builder, "headUnderwater");
        appendBoolean(builder, waterState.headUnderwater());
        builder.append('}');
        builder.append('}');
    }

    private static void appendPlayerState(StringBuilder builder, PlayerState state) {
        if (state == null) {
            builder.append("null");
            return;
        }
        builder.append('{');
        appendName(builder, "x");
        appendDouble(builder, state.x());
        builder.append(',');
        appendName(builder, "y");
        appendDouble(builder, state.y());
        builder.append(',');
        appendName(builder, "z");
        appendDouble(builder, state.z());
        builder.append(',');
        appendName(builder, "velocityX");
        appendFloat(builder, state.velocityX());
        builder.append(',');
        appendName(builder, "velocityY");
        appendFloat(builder, state.velocityY());
        builder.append(',');
        appendName(builder, "velocityZ");
        appendFloat(builder, state.velocityZ());
        builder.append(',');
        appendName(builder, "onGround");
        appendBoolean(builder, state.onGround());
        builder.append(',');
        appendName(builder, "underwater");
        appendBoolean(builder, state.underwater());
        builder.append(',');
        appendName(builder, "fallImpactSpeed");
        appendFloat(builder, state.fallImpactSpeed());
        builder.append(',');
        appendName(builder, "coyoteTimeSeconds");
        appendFloat(builder, state.coyoteTimeSeconds());
        builder.append(',');
        appendName(builder, "jumpBufferSeconds");
        appendFloat(builder, state.jumpBufferSeconds());
        builder.append('}');
    }

    private static void appendEntities(StringBuilder builder, List<EntitySnapshot> entities) {
        builder.append('[');
        for (int index = 0; index < entities.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendEntity(builder, entities.get(index));
        }
        builder.append(']');
    }

    private static void appendEntity(StringBuilder builder, EntitySnapshot state) {
        builder.append('{');
        appendName(builder, "entityId");
        appendLong(builder, state.entityId());
        builder.append(',');
        appendName(builder, "typeKey");
        appendString(builder, state.typeKey());
        builder.append(',');
        appendName(builder, "ownerPlayerId");
        appendUuid(builder, state.ownerPlayerId());
        builder.append(',');
        appendName(builder, "x");
        appendDouble(builder, state.x());
        builder.append(',');
        appendName(builder, "y");
        appendDouble(builder, state.y());
        builder.append(',');
        appendName(builder, "z");
        appendDouble(builder, state.z());
        builder.append(',');
        appendName(builder, "yaw");
        appendFloat(builder, state.yaw());
        builder.append(',');
        appendName(builder, "pitch");
        appendFloat(builder, state.pitch());
        builder.append(',');
        appendName(builder, "health");
        appendInt(builder, state.health());
        builder.append(',');
        appendName(builder, "stateKey");
        appendString(builder, state.stateKey());
        builder.append(',');
        appendName(builder, "velocityX");
        appendDouble(builder, state.velocityX());
        builder.append(',');
        appendName(builder, "velocityY");
        appendDouble(builder, state.velocityY());
        builder.append(',');
        appendName(builder, "velocityZ");
        appendDouble(builder, state.velocityZ());
        builder.append('}');
    }

    private static void appendProjectiles(StringBuilder builder, List<ProjectileState> projectiles) {
        builder.append('[');
        for (int index = 0; index < projectiles.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendProjectile(builder, projectiles.get(index));
        }
        builder.append(']');
    }

    private static void appendProjectile(StringBuilder builder, ProjectileState state) {
        builder.append('{');
        appendName(builder, "projectileId");
        appendLong(builder, state.projectileId());
        builder.append(',');
        appendName(builder, "ownerPlayerId");
        appendUuid(builder, state.ownerPlayerId());
        builder.append(',');
        appendName(builder, "typeKey");
        appendString(builder, state.typeKey());
        builder.append(',');
        appendName(builder, "x");
        appendDouble(builder, state.x());
        builder.append(',');
        appendName(builder, "y");
        appendDouble(builder, state.y());
        builder.append(',');
        appendName(builder, "z");
        appendDouble(builder, state.z());
        builder.append(',');
        appendName(builder, "velocityX");
        appendDouble(builder, state.velocityX());
        builder.append(',');
        appendName(builder, "velocityY");
        appendDouble(builder, state.velocityY());
        builder.append(',');
        appendName(builder, "velocityZ");
        appendDouble(builder, state.velocityZ());
        builder.append(',');
        appendName(builder, "ageTicks");
        appendInt(builder, state.ageTicks());
        builder.append('}');
    }

    private static void appendBlockSamples(StringBuilder builder, List<PhysicsReplayFrame.BlockSample> blockSamples) {
        builder.append('[');
        for (int index = 0; index < blockSamples.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            PhysicsReplayFrame.BlockSample sample = blockSamples.get(index);
            builder.append('{');
            appendName(builder, "x");
            appendInt(builder, sample.x());
            builder.append(',');
            appendName(builder, "y");
            appendInt(builder, sample.y());
            builder.append(',');
            appendName(builder, "z");
            appendInt(builder, sample.z());
            builder.append(',');
            appendName(builder, "blockId");
            appendInt(builder, sample.blockId());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendEvents(StringBuilder builder, List<PhysicsReplayFrame.Event> events) {
        builder.append('[');
        for (int index = 0; index < events.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            PhysicsReplayFrame.Event event = events.get(index);
            builder.append('{');
            appendName(builder, "type");
            appendString(builder, event.type());
            builder.append(',');
            appendName(builder, "data");
            appendStringMap(builder, event.data());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendStringMap(StringBuilder builder, Map<String, String> values) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            appendName(builder, entry.getKey());
            appendString(builder, entry.getValue());
            first = false;
        }
        builder.append('}');
    }

    private static void appendLongMap(StringBuilder builder, Map<String, Long> values) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            appendName(builder, entry.getKey());
            appendLong(builder, entry.getValue());
            first = false;
        }
        builder.append('}');
    }

    private static PhysicsReplayFrame frameFrom(Map<String, Object> object) {
        int schemaVersion = intValue(object, "schemaVersion");
        long tick = longValue(object, "tick");
        PhysicsConfigSnapshot config = configFrom(objectValue(object, "config"));
        PhysicsReplayFrame.Input input = inputFrom(objectValue(object, "input"));
        PlayerState playerState = playerStateFrom(nullableObjectValue(object, "playerState"));
        List<EntitySnapshot> entities = entityListFrom(arrayValue(object, "entityStates"));
        List<ProjectileState> projectiles = projectileListFrom(arrayValue(object, "projectileStates"));
        List<PhysicsReplayFrame.BlockSample> blocks = blockSamplesFrom(arrayValue(object, "blockSamples"));
        List<PhysicsReplayFrame.Event> events = eventsFrom(arrayValue(object, "events"));
        Map<String, Long> stats = longMapFrom(objectValue(object, "stats"), "stats");
        return new PhysicsReplayFrame(schemaVersion, tick, config, input, playerState, entities, projectiles, blocks, events, stats);
    }

    private static PhysicsConfigSnapshot configFrom(Map<String, Object> object) {
        return new PhysicsConfigSnapshot(
                intValue(object, "schemaVersion"),
                stringValue(object, "playerFingerprint"),
                stringValue(object, "projectileFingerprint")
        );
    }

    private static PhysicsReplayFrame.Input inputFrom(Map<String, Object> object) {
        Map<String, Object> player = objectValue(object, "player");
        Map<String, Object> water = objectValue(object, "water");
        return new PhysicsReplayFrame.Input(
                longValue(object, "sequence"),
                new PlayerInput(
                        floatValue(player, "moveX"),
                        floatValue(player, "moveZ"),
                        booleanValue(player, "jump"),
                        booleanValue(player, "descend"),
                        booleanValue(player, "sprint")
                ),
                new PlayerWaterState(
                        booleanValue(water, "feetInWater"),
                        booleanValue(water, "bodyInWater"),
                        booleanValue(water, "headUnderwater")
                )
        );
    }

    private static PlayerState playerStateFrom(Map<String, Object> object) {
        if (object == null) {
            return null;
        }
        return new PlayerState(
                doubleValue(object, "x"),
                doubleValue(object, "y"),
                doubleValue(object, "z"),
                floatValue(object, "velocityX"),
                floatValue(object, "velocityY"),
                floatValue(object, "velocityZ"),
                booleanValue(object, "onGround"),
                booleanValue(object, "underwater"),
                floatValue(object, "fallImpactSpeed"),
                floatValue(object, "coyoteTimeSeconds"),
                floatValue(object, "jumpBufferSeconds")
        );
    }

    private static List<EntitySnapshot> entityListFrom(List<Object> values) {
        ArrayList<EntitySnapshot> entities = new ArrayList<>(values.size());
        for (Object value : values) {
            Map<String, Object> object = castObject(value, "entityStates entry");
            entities.add(new EntitySnapshot(
                    longValue(object, "entityId"),
                    stringValue(object, "typeKey"),
                    uuidValue(object, "ownerPlayerId"),
                    doubleValue(object, "x"),
                    doubleValue(object, "y"),
                    doubleValue(object, "z"),
                    floatValue(object, "yaw"),
                    floatValue(object, "pitch"),
                    intValue(object, "health"),
                    stringValue(object, "stateKey"),
                    doubleValue(object, "velocityX"),
                    doubleValue(object, "velocityY"),
                    doubleValue(object, "velocityZ")
            ));
        }
        return entities;
    }

    private static List<ProjectileState> projectileListFrom(List<Object> values) {
        ArrayList<ProjectileState> projectiles = new ArrayList<>(values.size());
        for (Object value : values) {
            Map<String, Object> object = castObject(value, "projectileStates entry");
            projectiles.add(new ProjectileState(
                    longValue(object, "projectileId"),
                    uuidValue(object, "ownerPlayerId"),
                    stringValue(object, "typeKey"),
                    doubleValue(object, "x"),
                    doubleValue(object, "y"),
                    doubleValue(object, "z"),
                    doubleValue(object, "velocityX"),
                    doubleValue(object, "velocityY"),
                    doubleValue(object, "velocityZ"),
                    intValue(object, "ageTicks")
            ));
        }
        return projectiles;
    }

    private static List<PhysicsReplayFrame.BlockSample> blockSamplesFrom(List<Object> values) {
        ArrayList<PhysicsReplayFrame.BlockSample> blocks = new ArrayList<>(values.size());
        for (Object value : values) {
            Map<String, Object> object = castObject(value, "blockSamples entry");
            blocks.add(new PhysicsReplayFrame.BlockSample(
                    intValue(object, "x"),
                    intValue(object, "y"),
                    intValue(object, "z"),
                    shortValue(object, "blockId")
            ));
        }
        return blocks;
    }

    private static List<PhysicsReplayFrame.Event> eventsFrom(List<Object> values) {
        ArrayList<PhysicsReplayFrame.Event> events = new ArrayList<>(values.size());
        for (Object value : values) {
            Map<String, Object> object = castObject(value, "events entry");
            events.add(new PhysicsReplayFrame.Event(
                    stringValue(object, "type"),
                    stringMapFrom(objectValue(object, "data"), "event data")
            ));
        }
        return events;
    }

    private static Map<String, String> stringMapFrom(Map<String, Object> object, String label) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            if (!(entry.getValue() instanceof String value)) {
                throw new IllegalArgumentException(label + " value must be a string: " + entry.getKey());
            }
            values.put(entry.getKey(), value);
        }
        return values;
    }

    private static Map<String, Long> longMapFrom(Map<String, Object> object, String label) {
        LinkedHashMap<String, Long> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            values.put(entry.getKey(), integralValue(entry.getValue(), label + "." + entry.getKey()));
        }
        return values;
    }

    private static Map<String, Object> objectValue(Map<String, Object> object, String key) {
        return castObject(required(object, key), key);
    }

    private static Map<String, Object> nullableObjectValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (value == null) {
            return null;
        }
        return castObject(value, key);
    }

    private static List<Object> arrayValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(key + " must be an array");
        }
        return List.copyOf(raw);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castObject(Object value, String label) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        return (Map<String, Object>) raw;
    }

    private static String stringValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return string;
    }

    private static UUID uuidValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(key + " must be a UUID string or null");
        }
        return UUID.fromString(string);
    }

    private static boolean booleanValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return bool;
    }

    private static int intValue(Map<String, Object> object, String key) {
        long value = integralValue(required(object, key), key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " is outside int range");
        }
        return (int) value;
    }

    private static short shortValue(Map<String, Object> object, String key) {
        long value = integralValue(required(object, key), key);
        if (value < 0L || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException(key + " is outside block id range");
        }
        return (short) value;
    }

    private static long longValue(Map<String, Object> object, String key) {
        return integralValue(required(object, key), key);
    }

    private static long integralValue(Object value, String label) {
        if (value instanceof Long number) {
            return number;
        }
        if (value instanceof Integer number) {
            return number.longValue();
        }
        throw new IllegalArgumentException(label + " must be an integer");
    }

    private static double doubleValue(Map<String, Object> object, String key) {
        Object value = required(object, key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        double result = number.doubleValue();
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException(key + " must be finite");
        }
        return result;
    }

    private static float floatValue(Map<String, Object> object, String key) {
        double value = doubleValue(object, key);
        if (value < -Float.MAX_VALUE || value > Float.MAX_VALUE) {
            throw new IllegalArgumentException(key + " is outside float range");
        }
        return (float) value;
    }

    private static Object required(Map<String, Object> object, String key) {
        if (!object.containsKey(key)) {
            throw new IllegalArgumentException("Missing physics replay field: " + key);
        }
        return object.get(key);
    }

    private static void appendName(StringBuilder builder, String name) {
        appendString(builder, name);
        builder.append(':');
    }

    private static void appendString(StringBuilder builder, String value) {
        PhysicsReplayJson.appendString(builder, value);
    }

    private static void appendUuid(StringBuilder builder, UUID value) {
        if (value == null) {
            builder.append("null");
        } else {
            appendString(builder, value.toString());
        }
    }

    private static void appendBoolean(StringBuilder builder, boolean value) {
        builder.append(value);
    }

    private static void appendInt(StringBuilder builder, int value) {
        builder.append(value);
    }

    private static void appendLong(StringBuilder builder, long value) {
        builder.append(value);
    }

    private static void appendFloat(StringBuilder builder, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Physics replay float must be finite");
        }
        builder.append(Float.toString(value));
    }

    private static void appendDouble(StringBuilder builder, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Physics replay double must be finite");
        }
        builder.append(Double.toString(value));
    }
}
