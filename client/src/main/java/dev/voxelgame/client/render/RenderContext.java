package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public record RenderContext(
        Matrix4f projection,
        Matrix4f view,
        ClientWorld world,
        Vector3f cameraPosition,
        RenderSettings settings,
        double timeSeconds,
        FrustumIntersection frustum
) {
    public RenderContext {
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(cameraPosition, "cameraPosition");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(frustum, "frustum");
    }
}
