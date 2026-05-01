package dev.voxelgame.client.render;

import java.util.Objects;

public record RenderPassStats(
        String passName,
        int renderedMeshes,
        int culledMeshes,
        int culledChunkPositions,
        int drawCalls,
        int triangles,
        int culledByDistance,
        int culledByBounds,
        int stateChanges
) {
    public RenderPassStats(String passName, int renderedMeshes, int culledMeshes, int culledChunkPositions, int drawCalls, int triangles, int culledByDistance, int culledByBounds) {
        this(passName, renderedMeshes, culledMeshes, culledChunkPositions, drawCalls, triangles, culledByDistance, culledByBounds, 0);
    }

    public RenderPassStats(String passName, int renderedMeshes, int culledMeshes, int culledChunkPositions, int drawCalls, int triangles) {
        this(passName, renderedMeshes, culledMeshes, culledChunkPositions, drawCalls, triangles, 0, culledMeshes, 0);
    }

    public RenderPassStats {
        Objects.requireNonNull(passName, "passName");
        renderedMeshes = Math.max(0, renderedMeshes);
        culledMeshes = Math.max(0, culledMeshes);
        culledChunkPositions = Math.max(0, culledChunkPositions);
        drawCalls = Math.max(0, drawCalls);
        triangles = Math.max(0, triangles);
        culledByDistance = Math.max(0, culledByDistance);
        culledByBounds = Math.max(0, culledByBounds);
        stateChanges = Math.max(0, stateChanges);
    }

    public static RenderPassStats empty(String passName) {
        return new RenderPassStats(passName, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
