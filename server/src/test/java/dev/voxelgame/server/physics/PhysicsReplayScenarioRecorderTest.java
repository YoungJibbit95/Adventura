package dev.voxelgame.server.physics;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.physics.PhysicsReplayCodec;
import dev.voxelgame.common.physics.PhysicsReplayFrame;
import dev.voxelgame.common.physics.ProjectileHit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class PhysicsReplayScenarioRecorderTest {
    @Test
    void projectileImpactScenarioRecordsServerWorldHitAndSamples() {
        PhysicsReplayScenarioRecorder.Result result = PhysicsReplayScenarioRecorder.record(
                new PhysicsReplayScenarioRecorder.Options(
                        PhysicsReplayScenarioRecorder.PROJECTILE_IMPACT_SCENARIO,
                        12345L,
                        8,
                        null,
                        false
                )
        );

        assertFalse(result.frames().isEmpty());
        assertFalse(result.terminalHits().isEmpty());
        assertEquals(ProjectileHit.Type.BLOCK, result.terminalHits().getFirst().type());

        PhysicsReplayFrame lastFrame = result.frames().getLast();
        PhysicsReplayFrame.Event hitEvent = lastFrame.events().stream()
                .filter(event -> "projectile_hit".equals(event.type()))
                .findFirst()
                .orElseThrow();
        assertEquals("BLOCK", hitEvent.data().get("hitType"));
        assertEquals("WEST", hitEvent.data().get("blockFace"));
        assertTrue(lastFrame.blockSamples().stream().anyMatch(sample -> sample.blockId() == Blocks.GARDEN_FENCE));
        assertEquals(1L, lastFrame.stats().get("terminalHits"));
        assertTrue(lastFrame.stats().get("worldSampleHash") > 0L);

        String jsonl = PhysicsReplayCodec.writeJsonl(result.frames());
        assertEquals(result.frames(), PhysicsReplayCodec.readJsonl(jsonl));
    }

    @Test
    void recordToFileWritesJsonlForGradleTask(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("projectile-impact.jsonl");
        PhysicsReplayScenarioRecorder.Result result = PhysicsReplayScenarioRecorder.recordToFile(
                new PhysicsReplayScenarioRecorder.Options(
                        PhysicsReplayScenarioRecorder.PROJECTILE_IMPACT_SCENARIO,
                        424242L,
                        8,
                        output,
                        false
                )
        );

        assertTrue(Files.isRegularFile(output));
        List<PhysicsReplayFrame> frames = PhysicsReplayCodec.readJsonl(Files.readString(output));
        assertEquals(result.frames(), frames);
        assertFalse(frames.getLast().events().isEmpty());
    }
}
