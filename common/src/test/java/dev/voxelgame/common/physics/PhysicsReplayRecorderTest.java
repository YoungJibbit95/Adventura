package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.world.DimensionSettings;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class PhysicsReplayRecorderTest {
    @Test
    void jsonlRoundTripKeepsStableOrderingAndAllCoreSections() {
        ProjectileState projectile = new ProjectileState(
                9L,
                null,
                "voxel:arrow_projectile",
                1.5,
                66.0,
                2.5,
                5.0,
                0.0,
                0.0,
                3
        );
        ProjectileHit hit = ProjectileHit.block(
                projectile,
                4,
                64,
                2,
                4.0,
                65.25,
                2.5,
                ProjectileHit.BlockFace.EAST
        );
        PhysicsReplayFrame frame = PhysicsReplayFrame.tick(12L)
                .config(new PhysicsConfigSnapshot(1, "player-test", "projectile-test"))
                .input(
                        7L,
                        new PlayerInput(0.5f, 0.25f, true, false, true),
                        new PlayerWaterState(false, true, false)
                )
                .playerState(new PlayerState(1.0, 65.0, 2.0, 0.1f, 0.0f, 0.2f, true, false, 0.0f, 0.1f, 0.0f))
                .entity(new EntitySnapshot(5L, "voxel:boar", null, 7.0, 64.0, 8.0, 0.0f, 0.0f, 20, EntitySnapshot.STATE_FLEE, 0.2, 0.0, 0.1))
                .entity(new EntitySnapshot(2L, "voxel:cozy_sheep", null, 3.0, 64.0, 4.0, 90.0f, 0.0f, 10, EntitySnapshot.STATE_WANDER, 0.0, 0.0, 0.1))
                .projectile(projectile)
                .blockSample(6, 65, 2, (short) 3)
                .blockSample(4, 64, 2, (short) 1)
                .event(new PhysicsReplayFrame.Event("custom_debug", Map.of("zeta", "last", "alpha", "first")))
                .projectileHit(hit)
                .stat("projectileHits", 1L)
                .stat("collisionShapeSections", 1L)
                .build();

        String jsonl = PhysicsReplayCodec.writeJsonl(List.of(frame));
        assertTrue(jsonl.startsWith("{\"schemaVersion\":1,\"tick\":12,\"config\""));
        assertTrue(jsonl.indexOf("\"entityId\":2") < jsonl.indexOf("\"entityId\":5"));
        assertTrue(jsonl.indexOf("\"x\":4,\"y\":64,\"z\":2,\"blockId\":1") < jsonl.indexOf("\"x\":6,\"y\":65,\"z\":2,\"blockId\":3"));
        assertTrue(jsonl.indexOf("\"alpha\":\"first\"") < jsonl.indexOf("\"zeta\":\"last\""));

        List<PhysicsReplayFrame> decoded = PhysicsReplayCodec.readJsonl(jsonl);

        assertEquals(List.of(frame), decoded);
        assertEquals(2L, decoded.getFirst().entityStates().getFirst().entityId());
        assertEquals("projectile_hit", decoded.getFirst().events().get(1).type());
    }

    @Test
    void recorderRejectsNonMonotonicTicks() {
        PhysicsReplayRecorder recorder = PhysicsReplayRecorder.create();
        recorder.record(PhysicsReplayFrame.tick(3L).build());

        assertThrows(IllegalArgumentException.class, () -> recorder.record(PhysicsReplayFrame.tick(3L).build()));
        assertThrows(IllegalArgumentException.class, () -> recorder.record(PhysicsReplayFrame.tick(2L).build()));
    }

    @Test
    void recordStepCapturesContextAndOutputEvents() {
        PhysicsReplayRecorder recorder = PhysicsReplayRecorder.create();
        PhysicsStepContext context = PhysicsStepContext.survival(
                0.05,
                8L,
                DimensionSettings.OVERWORLD,
                new PlayerWaterState(true, true, false)
        );

        PhysicsReplayFrame frame = recorder.recordStep(context, builder -> builder
                .input(21L, new PlayerInput(1.0f, 0.0f, false, false, true), context.waterState())
                .playerState(new PlayerState(2.0, 66.0, 3.0, 0.4f, 0.0f, 0.0f, true, false, 0.0f))
                .stat("acceptedInputs", 1L));

        assertFalse(recorder.isEmpty());
        assertEquals(8L, frame.tick());
        assertEquals(21L, frame.input().sequence());
        assertTrue(frame.input().waterState().feetInWater());
        assertEquals(1L, frame.stats().get("acceptedInputs"));
    }

    @Test
    void loadsJsonlGoldenResource() throws IOException {
        List<PhysicsReplayFrame> frames = PhysicsReplayCodec.readJsonl(load("physics-replays/projectile_impact_v1.jsonl"));

        assertEquals(1, frames.size());
        PhysicsReplayFrame frame = frames.getFirst();
        assertEquals(42L, frame.tick());
        assertEquals("player-test", frame.config().playerFingerprint());
        assertEquals(7L, frame.input().sequence());
        assertEquals(1, frame.blockSamples().size());
        assertEquals((short) 1, frame.blockSamples().getFirst().blockId());
        assertEquals("projectile_hit", frame.events().getFirst().type());
        assertEquals("EAST", frame.events().getFirst().data().get("blockFace"));
        assertEquals(1L, frame.stats().get("projectileHits"));
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        String json = PhysicsReplayCodec.writeFrame(PhysicsReplayFrame.tick(1L).build())
                .replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":99");

        assertThrows(IllegalArgumentException.class, () -> PhysicsReplayCodec.readFrame(json));
    }

    private static String load(String resource) throws IOException {
        try (InputStream input = PhysicsReplayRecorderTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing replay resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
