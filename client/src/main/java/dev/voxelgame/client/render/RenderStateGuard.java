package dev.voxelgame.client.render;

import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public final class RenderStateGuard {
    private RenderStateGuard() {
    }

    public static void apply(RenderPassPlan.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        if (entry.depthTest()) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthMask(entry.depthWrite());
        if (entry.blendingEnabled()) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else {
            glDisable(GL_BLEND);
        }
    }

    public static void restoreAfter(RenderPassPlan.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        if (!entry.depthTest()) {
            glEnable(GL_DEPTH_TEST);
        }
        if (!entry.depthWrite()) {
            glDepthMask(true);
        }
        if (entry.blendingEnabled()) {
            glDisable(GL_BLEND);
        }
    }

    public static int estimatedStateChanges(RenderPassPlan.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        int changes = 3; // depth-test, depth-write and blend enable/disable are explicit per pass.
        if (entry.blendingEnabled()) {
            changes++; // blend function is part of the pass contract when blending is active.
        }
        if (!entry.depthTest()) {
            changes++;
        }
        if (!entry.depthWrite()) {
            changes++;
        }
        if (entry.blendingEnabled()) {
            changes++;
        }
        return changes;
    }

    public record StateContract(
            boolean depthTest,
            boolean depthWrite,
            RenderPassPlan.BlendMode blendMode,
            int estimatedStateChanges
    ) {
        public StateContract {
            blendMode = blendMode == null ? RenderPassPlan.BlendMode.NONE : blendMode;
            estimatedStateChanges = Math.max(0, estimatedStateChanges);
        }
    }

    public static StateContract contractFor(RenderPassPlan.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        return new StateContract(
                entry.depthTest(),
                entry.depthWrite(),
                entry.blendMode(),
                estimatedStateChanges(entry)
        );
    }
}
