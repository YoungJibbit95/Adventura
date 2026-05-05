package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockRenderLayer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisibilityCollectorTest {
    @Test
    void cullsSectionPartsIndependentlyByBounds() {
        RenderContext context = new RenderContext(
                new Matrix4f(),
                new Matrix4f(),
                new ClientWorld(123L),
                new Vector3f(0.0f, 0.0f, 0.0f),
                RenderSettings.defaults(4),
                0.0,
                new FrustumIntersection(new Matrix4f())
        );
        ChunkMesh.SectionPart inside = new ChunkMesh.SectionPart(
                0,
                BlockRenderLayer.SOLID,
                0,
                6,
                new ChunkMesh.Bounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)
        );
        ChunkMesh.SectionPart outside = new ChunkMesh.SectionPart(
                1,
                BlockRenderLayer.SOLID,
                6,
                6,
                new ChunkMesh.Bounds(3.0f, 3.0f, 3.0f, 4.0f, 4.0f, 4.0f)
        );

        assertEquals(VisibilityCollector.Culling.VISIBLE, VisibilityCollector.partCulling(inside, context));
        assertEquals(VisibilityCollector.Culling.BOUNDS, VisibilityCollector.partCulling(outside, context));
    }
}
