package dev.voxelgame.common.engine;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineJobModelTest {
    @Test
    void definesStableJobKeysForEngineSchedulers() {
        assertEquals(EngineJobType.CHUNK_GENERATE, EngineJobType.findByKey("chunk.generate").orElseThrow());
        assertEquals(EngineJobType.CHUNK_LIGHT, EngineJobType.findByKey("CHUNK.LIGHT").orElseThrow());
        assertEquals(EngineJobType.CHUNK_MESH, EngineJobType.findByKey(" chunk.mesh ").orElseThrow());
        assertEquals(EngineJobType.SAVE_WRITE, EngineJobType.findByKey("save.write").orElseThrow());
        assertEquals(EngineJobType.NET_ENCODE, EngineJobType.findByKey("net.encode").orElseThrow());
        assertTrue(EngineJobType.findByKey("chunk.unknown").isEmpty());
    }

    @Test
    void playerActionPriorityWinsBeforeVisiblePreviewAndBackground() {
        List<EngineJobPriority> priorities = Arrays.stream(EngineJobPriority.values())
                .sorted(Comparator.comparingInt(EngineJobPriority::sortOrder))
                .toList();

        assertEquals(List.of(
                EngineJobPriority.PLAYER_ACTION,
                EngineJobPriority.VISIBLE_CHUNK,
                EngineJobPriority.PREVIEW,
                EngineJobPriority.BACKGROUND
        ), priorities);
        assertTrue(EngineJobPriority.PLAYER_ACTION.outranks(EngineJobPriority.PREVIEW));
        assertTrue(EngineJobPriority.VISIBLE_CHUNK.outranks(EngineJobPriority.BACKGROUND));
    }
}
