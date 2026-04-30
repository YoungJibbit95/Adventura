package dev.voxelgame.client;

import dev.voxelgame.common.block.ToolType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyGameMilestonesTest {
    @Test
    void firstCollectedItemGetsSupplyPromptOnlyOnce() {
        EarlyGameMilestones milestones = new EarlyGameMilestones();

        assertEquals(Optional.of("First supply gathered: berries"), milestones.collectItem("voxel:berries", "berries"));
        assertTrue(milestones.collectItem("voxel:twig", "twig").isEmpty());
    }

    @Test
    void recipeUnlockMessageDistinguishesFirstUnlock() {
        EarlyGameMilestones milestones = new EarlyGameMilestones();

        assertEquals("First recipe unlocked: Build Campfire", milestones.unlockRecipe("Build Campfire"));
        assertEquals("Recipe unlocked: Craft Stone Knife", milestones.unlockRecipe("Craft Stone Knife"));
    }

    @Test
    void craftingPromptsForCampfireAndFirstTool() {
        EarlyGameMilestones milestones = new EarlyGameMilestones();

        assertEquals(
                Optional.of("Campfire crafted. Place it before nightfall."),
                milestones.craftItem("voxel:campfire", "campfire", ToolType.NONE)
        );
        assertEquals(
                Optional.of("First tool crafted: stone knife"),
                milestones.craftItem("voxel:stone_knife", "stone knife", ToolType.KNIFE)
        );
        assertTrue(milestones.craftItem("voxel:stone_axe", "stone axe", ToolType.AXE).isEmpty());
    }

    @Test
    void placementAndCampfireLightingPromptsAreOneShot() {
        EarlyGameMilestones milestones = new EarlyGameMilestones();

        assertEquals(Optional.of("Campfire built. Add fuel to make a safe camp."), milestones.placeBlock("voxel:campfire"));
        assertTrue(milestones.placeBlock("voxel:campfire").isEmpty());
        assertEquals(Optional.of("Campfire lit. Stay nearby for warmth and comfort."), milestones.lightCampfire());
        assertTrue(milestones.lightCampfire().isEmpty());
    }

    @Test
    void resetAllowsMilestonesForNewSession() {
        EarlyGameMilestones milestones = new EarlyGameMilestones();

        milestones.collectItem("voxel:berries", "berries");
        milestones.reset();

        assertEquals(Optional.of("First supply gathered: twig"), milestones.collectItem("voxel:twig", "twig"));
    }
}
