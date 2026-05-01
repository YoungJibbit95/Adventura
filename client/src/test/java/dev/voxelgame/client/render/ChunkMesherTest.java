package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkMesherTest {
    @Test
    void isolatedCubeBuildsSixFullFaces() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);

        ChunkMesh mesh = new ChunkMesher().buildTerrainMesh(meshWorld.world(), meshWorld.chunk(), false);

        assertEquals(24, mesh.vertexCount());
        assertEquals(36, mesh.indexCount());
        assertEquals(12, mesh.triangleCount());
        assertBounds(mesh, 0.0f, 1.0f, 64.0f, 65.0f, 0.0f, 1.0f);
    }

    @Test
    void adjacentCubesCullSharedFaceAndKeepIntegerBoundaries() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(1, 64, 0, Blocks.STONE);

        ChunkMesh mesh = new ChunkMesher().buildTerrainMesh(meshWorld.world(), meshWorld.chunk(), false);

        assertEquals(24, mesh.vertexCount());
        assertEquals(36, mesh.indexCount());
        assertBounds(mesh, 0.0f, 2.0f, 64.0f, 65.0f, 0.0f, 1.0f);
        assertIntegerVertexPositions(mesh);
        assertHasTiledUv(mesh, 2.0f);
    }

    @Test
    void visibleSolidLayerUsesGreedyMeshWhenAmbientOcclusionIsOff() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(1, 64, 0, Blocks.STONE);

        ChunkMesh mesh = new ChunkMesher().buildVisibleFaceMesh(
                meshWorld.world(),
                meshWorld.chunk(),
                BlockRenderLayer.SOLID,
                false
        );

        assertEquals(24, mesh.vertexCount());
        assertEquals(36, mesh.indexCount());
        assertBounds(mesh, 0.0f, 2.0f, 64.0f, 65.0f, 0.0f, 1.0f);
        assertHasTiledUv(mesh, 2.0f);
    }

    @Test
    void ambientOcclusionPathUsesGreedyMeshWhenMergeInputsMatch() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(1, 64, 0, Blocks.STONE);

        ChunkMesher mesher = new ChunkMesher();
        ChunkMesh mesh = mesher.buildTerrainMesh(meshWorld.world(), meshWorld.chunk(), true);

        assertEquals(24, mesh.vertexCount());
        assertEquals(36, mesh.indexCount());
        assertEquals(true, mesher.lastBuildStats().greedyMeshing());
    }

    @Test
    void greedyMeshingCanBeDisabledForVisualComparison() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(1, 64, 0, Blocks.STONE);

        ChunkMesher mesher = new ChunkMesher();
        mesher.setGreedyMeshingEnabled(false);
        ChunkMesh mesh = mesher.buildTerrainMesh(meshWorld.world(), meshWorld.chunk(), true);
        ChunkMesher.MeshBuildStats stats = mesher.lastBuildStats();

        assertEquals(40, mesh.vertexCount());
        assertEquals(60, mesh.indexCount());
        assertEquals(false, stats.greedyMeshing());
        assertEquals(mesh.vertexCount(), stats.vertices());
        assertEquals(mesh.indexCount(), stats.indices());
        assertEquals(mesh.estimatedBytes(), stats.outputBytes());
        assertEquals(0L, stats.temporaryBufferGrowthBytes());
        assertEquals(true, stats.retainedBufferBytes() >= stats.outputBytes());
    }

    @Test
    void visibleFaceMeshingCanSplitSolidCutoutAndTransparentLayers() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(2, 64, 0, Blocks.WILD_GRASS);
        meshWorld.world().setBlockId(4, 64, 0, Blocks.WATER);

        ChunkMesher mesher = new ChunkMesher();
        ChunkMesh solid = mesher.buildVisibleFaceMesh(meshWorld.world(), meshWorld.chunk(), BlockRenderLayer.SOLID, false);
        ChunkMesh cutout = mesher.buildVisibleFaceMesh(meshWorld.world(), meshWorld.chunk(), BlockRenderLayer.CUTOUT, false);
        ChunkMesh transparent = mesher.buildVisibleFaceMesh(meshWorld.world(), meshWorld.chunk(), BlockRenderLayer.TRANSLUCENT, false);

        assertEquals(24, solid.vertexCount());
        assertEquals(8, cutout.vertexCount());
        assertEquals(24, transparent.vertexCount());
    }

    @Test
    void sectionLayerMeshRestrictsOutputToRequestedVerticalSection() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setBlockId(0, 96, 0, Blocks.STONE);

        ChunkMesh sectionMesh = new ChunkMesher().buildSectionLayerMesh(
                meshWorld.world(),
                meshWorld.chunk(),
                4,
                BlockRenderLayer.SOLID,
                false
        );

        assertEquals(24, sectionMesh.vertexCount());
        assertBounds(sectionMesh, 0.0f, 1.0f, 64.0f, 65.0f, 0.0f, 1.0f);
    }

    @Test
    void verticesCarrySeparateSkyAndBlockLightForDebugViews() {
        MeshWorld meshWorld = meshWorld();
        meshWorld.world().setBlockId(0, 64, 0, Blocks.STONE);
        meshWorld.world().setSkyLight(0, 65, 0, 15);
        meshWorld.world().setBlockLight(0, 65, 0, 6);

        ChunkMesh mesh = new ChunkMesher().buildTerrainMesh(meshWorld.world(), meshWorld.chunk(), false);

        assertTopFaceLight(mesh, 252.0f / 255.0f, 1.0f, 0.4f);
    }

    private static MeshWorld meshWorld() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        InMemoryWorld world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        return new MeshWorld(world, chunk);
    }

    private static void assertBounds(ChunkMesh mesh, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        float actualMinX = Float.POSITIVE_INFINITY;
        float actualMinY = Float.POSITIVE_INFINITY;
        float actualMinZ = Float.POSITIVE_INFINITY;
        float actualMaxX = Float.NEGATIVE_INFINITY;
        float actualMaxY = Float.NEGATIVE_INFINITY;
        float actualMaxZ = Float.NEGATIVE_INFINITY;
        float[] vertices = mesh.vertices();
        for (int offset = 0; offset < vertices.length; offset += ChunkMesher.FLOATS_PER_VERTEX) {
            actualMinX = Math.min(actualMinX, vertices[offset]);
            actualMinY = Math.min(actualMinY, vertices[offset + 1]);
            actualMinZ = Math.min(actualMinZ, vertices[offset + 2]);
            actualMaxX = Math.max(actualMaxX, vertices[offset]);
            actualMaxY = Math.max(actualMaxY, vertices[offset + 1]);
            actualMaxZ = Math.max(actualMaxZ, vertices[offset + 2]);
        }
        assertEquals(minX, actualMinX, 0.0001f);
        assertEquals(maxX, actualMaxX, 0.0001f);
        assertEquals(minY, actualMinY, 0.0001f);
        assertEquals(maxY, actualMaxY, 0.0001f);
        assertEquals(minZ, actualMinZ, 0.0001f);
        assertEquals(maxZ, actualMaxZ, 0.0001f);
        assertEquals(minX, mesh.bounds().minX(), 0.0001f);
        assertEquals(maxX, mesh.bounds().maxX(), 0.0001f);
        assertEquals(minY, mesh.bounds().minY(), 0.0001f);
        assertEquals(maxY, mesh.bounds().maxY(), 0.0001f);
        assertEquals(minZ, mesh.bounds().minZ(), 0.0001f);
        assertEquals(maxZ, mesh.bounds().maxZ(), 0.0001f);
    }

    private static void assertIntegerVertexPositions(ChunkMesh mesh) {
        float[] vertices = mesh.vertices();
        for (int offset = 0; offset < vertices.length; offset += ChunkMesher.FLOATS_PER_VERTEX) {
            assertEquals((float) Math.round(vertices[offset]), vertices[offset], 0.0001f);
            assertEquals((float) Math.round(vertices[offset + 1]), vertices[offset + 1], 0.0001f);
            assertEquals((float) Math.round(vertices[offset + 2]), vertices[offset + 2], 0.0001f);
        }
    }

    private static void assertHasTiledUv(ChunkMesh mesh, float value) {
        float[] vertices = mesh.vertices();
        for (int offset = 0; offset < vertices.length; offset += ChunkMesher.FLOATS_PER_VERTEX) {
            if (Math.abs(vertices[offset + ChunkMesher.FACE_UV_OFFSET] - value) < 0.0001f
                    || Math.abs(vertices[offset + ChunkMesher.FACE_UV_OFFSET + 1] - value) < 0.0001f) {
                return;
            }
        }
        throw new AssertionError("Expected merged mesh to contain tiled UV coordinate " + value);
    }

    private static void assertTopFaceLight(ChunkMesh mesh, float combined, float sky, float block) {
        float[] vertices = mesh.vertices();
        for (int offset = 0; offset < vertices.length; offset += ChunkMesher.FLOATS_PER_VERTEX) {
            float normalY = vertices[offset + ChunkMesher.NORMAL_OFFSET + 1];
            if (Math.abs(normalY - 1.0f) < 0.0001f) {
                assertEquals(combined, vertices[offset + ChunkMesher.LIGHT_OFFSET], 0.0001f);
                assertEquals(sky, vertices[offset + ChunkMesher.SKY_LIGHT_OFFSET], 0.0001f);
                assertEquals(block, vertices[offset + ChunkMesher.BLOCK_LIGHT_OFFSET], 0.0001f);
                return;
            }
        }
        throw new AssertionError("Expected mesh to contain a top face");
    }

    private record MeshWorld(InMemoryWorld world, Chunk chunk) {
    }
}
