package dev.voxelgame.client.render;

enum TerrainPass implements RenderPass {
    OPAQUE(RenderPassPlan.require(RenderPassPlan.TERRAIN_OPAQUE)),
    CUTOUT(RenderPassPlan.require(RenderPassPlan.TERRAIN_CUTOUT)),
    TRANSLUCENT(RenderPassPlan.require(RenderPassPlan.TERRAIN_TRANSLUCENT));

    private final RenderPassPlan.Entry entry;

    TerrainPass(RenderPassPlan.Entry entry) {
        this.entry = entry;
    }

    @Override
    public String passName() {
        return entry.passName();
    }

    @Override
    public void begin(RenderContext context) {
        RenderStateGuard.apply(entry);
    }

    @Override
    public void end(RenderContext context) {
        RenderStateGuard.restoreAfter(entry);
    }

    RenderPassPlan.Entry entry() {
        return entry;
    }

    int estimatedStateChanges() {
        return RenderStateGuard.estimatedStateChanges(entry);
    }
}
