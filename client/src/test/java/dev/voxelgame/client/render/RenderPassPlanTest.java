package dev.voxelgame.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderPassPlanTest {
    @Test
    void alphaPlanKeepsStablePassOrder() {
        List<String> names = RenderPassPlan.alphaPlan().stream()
                .map(RenderPassPlan.Entry::passName)
                .toList();

        assertEquals(List.of(
                RenderPassPlan.SKY_CLEAR,
                RenderPassPlan.TERRAIN_OPAQUE,
                RenderPassPlan.TERRAIN_CUTOUT,
                RenderPassPlan.ENTITIES_OPAQUE,
                RenderPassPlan.TERRAIN_TRANSLUCENT,
                RenderPassPlan.PARTICLES,
                RenderPassPlan.OUTLINES_OVERLAYS,
                RenderPassPlan.HUD_UI,
                RenderPassPlan.DEBUG
        ), names);
    }

    @Test
    void transparentTerrainRunsAfterOpaqueEntitiesAndBeforeParticles() {
        assertTrue(RenderPassPlan.orderOf(RenderPassPlan.TERRAIN_OPAQUE) < RenderPassPlan.orderOf(RenderPassPlan.ENTITIES_OPAQUE));
        assertTrue(RenderPassPlan.orderOf(RenderPassPlan.ENTITIES_OPAQUE) < RenderPassPlan.orderOf(RenderPassPlan.TERRAIN_TRANSLUCENT));
        assertTrue(RenderPassPlan.orderOf(RenderPassPlan.TERRAIN_TRANSLUCENT) < RenderPassPlan.orderOf(RenderPassPlan.PARTICLES));
    }

    @Test
    void glStateContractIsExplicitForTerrainAndHudPasses() {
        RenderPassPlan.Entry opaque = entry(RenderPassPlan.TERRAIN_OPAQUE);
        RenderPassPlan.Entry transparent = entry(RenderPassPlan.TERRAIN_TRANSLUCENT);
        RenderPassPlan.Entry hud = entry(RenderPassPlan.HUD_UI);

        assertTrue(opaque.depthTest());
        assertTrue(opaque.depthWrite());
        assertFalse(opaque.blendingEnabled());
        assertTrue(transparent.depthTest());
        assertFalse(transparent.depthWrite());
        assertEquals(RenderPassPlan.BlendMode.ALPHA, transparent.blendMode());
        assertFalse(hud.depthTest());
        assertTrue(hud.blendingEnabled());
    }

    private static RenderPassPlan.Entry entry(String passName) {
        return RenderPassPlan.alphaPlan().stream()
                .filter(entry -> entry.passName().equals(passName))
                .findFirst()
                .orElseThrow();
    }
}
