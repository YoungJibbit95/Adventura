package dev.voxelgame.client.render;

public interface RenderPass {
    String passName();

    void begin(RenderContext context);

    void end(RenderContext context);
}
