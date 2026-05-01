package dev.voxelgame.client.render;

import java.util.Objects;

public final class RenderPassExecutor {
    public RenderPassStats execute(RenderContext context, RenderPass pass, PassBody body) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(body, "body");
        pass.begin(context);
        try {
            return body.run();
        } finally {
            pass.end(context);
        }
    }

    @FunctionalInterface
    public interface PassBody {
        RenderPassStats run();
    }
}
