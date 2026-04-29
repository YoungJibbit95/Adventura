package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesher;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldMeshInvalidationTest {
    @Test
    void blockUpdatesMarkChunkAndFourNeighborsDirty() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        assertEquals(5, world.dirtyChunkCount());

        Set<ChunkPos> rebuilt = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 5)
                .stream()
                .map(ClientWorld.LayeredMeshBuild::pos)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                new ChunkPos(0, 0),
                new ChunkPos(1, 0),
                new ChunkPos(-1, 0),
                new ChunkPos(0, 1),
                new ChunkPos(0, -1)
        ), rebuilt);
        assertEquals(0, world.dirtyChunkCount());
    }

    @Test
    void dirtyMeshBuildsRespectPerFrameBudget() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        assertEquals(2, world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 2).size());
        assertEquals(3, world.dirtyChunkCount());
        assertEquals(3, world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 99).size());
        assertEquals(0, world.dirtyChunkCount());
    }

    @Test
    void priorityPositionBuildsNearestDirtyChunkFirst() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(3);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        world.applyBlock(new GamePacket.BlockUpdate(40, 80, 40, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        List<ClientWorld.LayeredMeshBuild> builds = world.buildDirtyLayeredMeshes(
                new ChunkMesher(),
                true,
                false,
                1,
                new Vector3f(8.0f, 80.0f, 8.0f)
        );

        assertEquals(List.of(new ChunkPos(0, 0)), builds.stream().map(ClientWorld.LayeredMeshBuild::pos).toList());
        assertTrue(world.dirtyChunkCount() > 0);
    }

    @Test
    void previewGenerationCanBeBudgetedAcrossFrames() {
        ClientWorld world = new ClientWorld(123L);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(2, world.loadedChunkCount());
        assertTrue(world.dirtyChunkCount() > 0);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(4, world.loadedChunkCount());
    }

    @Test
    void exposesLightLevelsForDebugOverlay() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.LANTERN));

        assertEquals(13, world.blockLightAt(8, 80, 8));
        assertEquals(Math.max(world.skyLightAt(8, 80, 8), 13), world.combinedLightAt(8, 80, 8));
    }

    @Test
    void transparentWaterToggleControlsTransparentMeshBuilds() {
        ClientWorld world = loadedCleanWorld();
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WATER));

        boolean hasTransparentMeshWhenDisabled = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 1)
                .getFirst()
                .transparentMesh()
                .indexCount() > 0;

        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WATER));
        boolean hasTransparentMesh = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 1)
                .getFirst()
                .transparentMesh()
                .indexCount() > 0;

        assertFalse(hasTransparentMeshWhenDisabled);
        assertTrue(hasTransparentMesh);
    }

    @Test
    void layeredBuildsKeepCutoutBlocksSeparateFromSolidTerrain() {
        ClientWorld world = loadedCleanWorld();
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WILD_GRASS));

        ClientWorld.LayeredMeshBuild build = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 1)
                .getFirst();

        assertTrue(build.opaqueMesh().indexCount() > 0);
        assertTrue(build.cutoutMesh().indexCount() > 0);
        assertEquals(0, build.transparentMesh().indexCount());
    }

    private static ClientWorld loadedCleanWorld() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(1);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);
        assertEquals(0, world.dirtyChunkCount());
        return world;
    }
}
