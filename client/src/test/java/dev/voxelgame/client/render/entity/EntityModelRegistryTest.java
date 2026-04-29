package dev.voxelgame.client.render.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityModelRegistryTest {
    @Test
    void playerModelUsesHumanoidPartsWithoutAmbientBob() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:player");

        assertEquals("voxel:player", model.key());
        assertEquals(6, model.partCount());
        assertFalse(model.ambientBob());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("head")));
    }

    @Test
    void ambientCreatureModelUsesBodyHeadAndLegs() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:cozy_sheep");

        assertEquals(6, model.partCount());
        assertTrue(model.ambientBob());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("body")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("front_left_leg")));
    }

    @Test
    void wispModelHasEmissiveParts() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:mire_wisp");

        assertEquals(2, model.partCount());
        assertTrue(model.parts().stream().allMatch(EntityModelPart::emissive));
    }

    @Test
    void bunnyModelHasRotatedEarsAndTail() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:forest_bunny");

        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("tail")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("left_ear") && part.rotationX() != 0.0f));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("right_ear") && part.rotationZ() != 0.0f));
    }

    @Test
    void snailModelHasShellAndFeelers() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:moss_snail");

        assertEquals(5, model.partCount());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("shell")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("left_feeler") && part.rotationX() < 0.0f));
    }

    @Test
    void boarModelHasSnoutTusksAndEars() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:little_boar");

        assertEquals(11, model.partCount());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("snout")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("left_tusk") && part.rotationZ() < 0.0f));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("right_ear") && part.rotationZ() > 0.0f));
    }

    @Test
    void crawlerModelHasLowBodyAndSideLegs() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:dune_crawler");

        assertEquals(7, model.partCount());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("low_body")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("carapace")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("left_front_leg") && part.rotationZ() > 0.0f));
    }

    @Test
    void grazerModelHasNeckHeadAndTail() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:forest_grazer");

        assertEquals(10, model.partCount());
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("neck") && part.rotationX() < 0.0f));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("head")));
        assertTrue(model.parts().stream().anyMatch(part -> part.name().equals("tail")));
    }
}
