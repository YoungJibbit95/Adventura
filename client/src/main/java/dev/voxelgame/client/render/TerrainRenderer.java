package dev.voxelgame.client.render;

import dev.voxelgame.common.world.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TerrainRenderer {
    private final RenderPassExecutor executor;

    TerrainRenderer(RenderPassExecutor executor) {
        this.executor = executor == null ? new RenderPassExecutor() : executor;
    }

    Result render(
            RenderContext context,
            Map<ChunkPos, GpuChunkMesh> opaqueMeshes,
            Map<ChunkPos, GpuChunkMesh> cutoutMeshes,
            Map<ChunkPos, GpuChunkMesh> transparentMeshes
    ) {
        Set<ChunkPos> culledPositions = new HashSet<>();
        RenderPassStats opaquePass = renderPass(context, TerrainPass.OPAQUE, opaqueMeshes, culledPositions);
        RenderPassStats cutoutPass = renderPass(context, TerrainPass.CUTOUT, cutoutMeshes, culledPositions);
        RenderPassStats transparentPass = renderPass(context, TerrainPass.TRANSLUCENT, transparentMeshes, culledPositions);
        return new Result(opaquePass, cutoutPass, transparentPass, culledPositions.size());
    }

    private RenderPassStats renderPass(
            RenderContext context,
            TerrainPass pass,
            Map<ChunkPos, GpuChunkMesh> meshes,
            Set<ChunkPos> culledPositions
    ) {
        return executor.execute(context, pass, () -> {
            int renderedMeshes = 0;
            int culledMeshes = 0;
            int drawCalls = 0;
            int triangles = 0;
            int culledByDistance = 0;
            int culledByBounds = 0;
            int loadedParts = 0;
            int renderedParts = 0;
            int culledParts = 0;
            Set<ChunkPos> passCulledPositions = new HashSet<>();
            Iterable<ChunkPos> positions = pass == TerrainPass.TRANSLUCENT
                    ? VisibilityCollector.transparentRenderOrderByMeshBounds(meshes, context.cameraPosition())
                    : meshes.keySet();
            for (ChunkPos pos : positions) {
                GpuChunkMesh mesh = meshes.get(pos);
                if (mesh == null) {
                    continue;
                }
                int meshParts = mesh.partCount();
                loadedParts += meshParts;
                VisibilityCollector.Culling culling = VisibilityCollector.meshCulling(pos, mesh, context);
                if (culling != VisibilityCollector.Culling.VISIBLE) {
                    culledMeshes++;
                    culledParts += meshParts;
                    if (culling == VisibilityCollector.Culling.DISTANCE) {
                        culledByDistance++;
                    } else {
                        culledByBounds++;
                    }
                    culledPositions.add(pos);
                    passCulledPositions.add(pos);
                    continue;
                }
                if (!mesh.parts().isEmpty()) {
                    List<ChunkMesh.SectionPart> visibleParts = VisibilityCollector.visibleParts(mesh, context);
                    if (visibleParts.isEmpty()) {
                        culledMeshes++;
                        culledParts += meshParts;
                        culledByBounds++;
                        culledPositions.add(pos);
                        passCulledPositions.add(pos);
                        continue;
                    }
                    mesh.drawParts(visibleParts);
                    renderedMeshes++;
                    renderedParts += visibleParts.size();
                    culledParts += Math.max(0, meshParts - visibleParts.size());
                    drawCalls += visibleParts.size();
                    for (ChunkMesh.SectionPart part : visibleParts) {
                        triangles += part.triangleCount();
                    }
                    continue;
                }
                mesh.draw();
                renderedMeshes++;
                drawCalls++;
                triangles += mesh.triangleCount();
            }
            return new RenderPassStats(
                    pass.passName(),
                    renderedMeshes,
                    culledMeshes,
                    passCulledPositions.size(),
                    drawCalls,
                    triangles,
                    culledByDistance,
                    culledByBounds,
                    pass.estimatedStateChanges(),
                    loadedParts,
                    renderedParts,
                    culledParts
            );
        });
    }

    record Result(
            RenderPassStats opaquePass,
            RenderPassStats cutoutPass,
            RenderPassStats transparentPass,
            int culledChunkPositions
    ) {
    }
}
