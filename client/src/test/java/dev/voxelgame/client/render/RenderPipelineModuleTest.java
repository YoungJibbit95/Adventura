package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderPipelineModuleTest {
    @Test
    void renderPassExecutorAlwaysEndsPasses() {
        RenderPassExecutor executor = new RenderPassExecutor();
        CountingPass pass = new CountingPass("test.pass");
        RenderContext context = context();

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(context, pass, () -> {
                    throw new IllegalStateException("boom");
                })
        );

        assertEquals("boom", thrown.getMessage());
        assertEquals(1, pass.begins);
        assertEquals(1, pass.ends);
    }

    @Test
    void renderStateContractsComeFromRuntimePassPlan() {
        RenderPassPlan.Entry opaque = RenderPassPlan.require(RenderPassPlan.TERRAIN_OPAQUE);
        RenderPassPlan.Entry transparent = RenderPassPlan.require(RenderPassPlan.TERRAIN_TRANSLUCENT);

        RenderStateGuard.StateContract opaqueContract = RenderStateGuard.contractFor(opaque);
        RenderStateGuard.StateContract transparentContract = RenderStateGuard.contractFor(transparent);

        assertTrue(opaqueContract.depthTest());
        assertTrue(opaqueContract.depthWrite());
        assertEquals(RenderPassPlan.BlendMode.NONE, opaqueContract.blendMode());
        assertTrue(transparentContract.depthTest());
        assertEquals(RenderPassPlan.BlendMode.ALPHA, transparentContract.blendMode());
        assertTrue(transparentContract.estimatedStateChanges() > opaqueContract.estimatedStateChanges());
        assertEquals(RenderPassPlan.TERRAIN_TRANSLUCENT, TerrainPass.TRANSLUCENT.passName());
    }

    @Test
    void renderPassStatsClampsStateChangeCounts() {
        RenderPassStats stats = new RenderPassStats("terrain.test", 1, 2, 1, 3, 4, 1, 1, -5);

        assertEquals(0, stats.stateChanges());
    }

    @Test
    void renderContextCopiesMutableCameraAndMatrices() {
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();
        Vector3f camera = new Vector3f(1.0f, 2.0f, 3.0f);

        RenderContext context = new RenderContext(
                projection,
                view,
                new ClientWorld(1L),
                camera,
                RenderSettings.defaults(4),
                0.0,
                new FrustumIntersection(new Matrix4f(projection).mul(view))
        );

        projection.m00(2.0f);
        view.m11(3.0f);
        camera.set(9.0f, 9.0f, 9.0f);

        assertEquals(1.0f, context.projection().m00(), 0.0001f);
        assertEquals(1.0f, context.view().m11(), 0.0001f);
        assertEquals(1.0f, context.cameraPosition().x, 0.0001f);
    }

    private static RenderContext context() {
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();
        return new RenderContext(
                projection,
                view,
                new ClientWorld(1L),
                new Vector3f(),
                RenderSettings.defaults(2),
                0.0,
                new FrustumIntersection(new Matrix4f(projection).mul(view))
        );
    }

    private static final class CountingPass implements RenderPass {
        private final String passName;
        private int begins;
        private int ends;

        private CountingPass(String passName) {
            this.passName = passName;
        }

        @Override
        public String passName() {
            return passName;
        }

        @Override
        public void begin(RenderContext context) {
            begins++;
        }

        @Override
        public void end(RenderContext context) {
            ends++;
        }
    }
}
