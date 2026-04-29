package dev.voxelgame.client.render.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRendererTest {
    @Test
    void entityFrustumTestUsesEntityBounds() {
        Matrix4f projectionView = new Matrix4f()
                .perspective((float) Math.toRadians(70.0), 16.0f / 9.0f, 0.05f, 128.0f)
                .mul(new Matrix4f().lookAt(0.0f, 2.0f, 4.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f));
        FrustumIntersection frustum = new FrustumIntersection(projectionView);

        assertTrue(EntityRenderer.insideFrustum(frustum, new EntitySnapshot(1L, "voxel:cozy_sheep", null, 0.0, 1.0, 0.0, 0.0f, 0.0f, 10)));
        assertFalse(EntityRenderer.insideFrustum(frustum, new EntitySnapshot(2L, "voxel:cozy_sheep", null, 500.0, 1.0, 500.0, 0.0f, 0.0f, 10)));
    }
}
