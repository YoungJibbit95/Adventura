package dev.voxelgame.client.render;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkBorderRendererTest {
    @Test
    void selectsChunkGridAroundCamera() {
        List<ChunkPos> chunks = ChunkBorderRenderer.chunkPositionsAround(new Vector3f(20.0f, 80.0f, -2.0f), 1);

        assertEquals(9, chunks.size());
        assertTrue(chunks.contains(new ChunkPos(1, -1)));
        assertTrue(chunks.contains(new ChunkPos(0, -2)));
        assertTrue(chunks.contains(new ChunkPos(2, 0)));
    }

    @Test
    void buildsTwelveEdgesPerChunk() {
        float[] vertices = ChunkBorderRenderer.borderVertices(
                List.of(new ChunkPos(1, -1)),
                new DimensionSettings(0, 32)
        );

        assertEquals(12 * 2 * 3, vertices.length);
        assertEquals(16.0f, vertices[0]);
        assertEquals(0.0f, vertices[1]);
        assertEquals(-16.0f, vertices[2]);
        assertEquals(32.0f, vertices[3]);
        assertEquals(0.0f, vertices[4]);
        assertEquals(-16.0f, vertices[5]);
    }

    @Test
    void buildsTwelveEdgesPerMeshBound() {
        float[] vertices = ChunkBorderRenderer.meshBoundsVertices(List.of(
                new ChunkMesh.Bounds(1.0f, 2.0f, 3.0f, 5.0f, 7.0f, 11.0f)
        ));

        assertEquals(12 * 2 * 3, vertices.length);
        assertEquals(1.0f, vertices[0]);
        assertEquals(2.0f, vertices[1]);
        assertEquals(3.0f, vertices[2]);
        assertEquals(5.0f, vertices[3]);
        assertEquals(2.0f, vertices[4]);
        assertEquals(3.0f, vertices[5]);
    }
}
