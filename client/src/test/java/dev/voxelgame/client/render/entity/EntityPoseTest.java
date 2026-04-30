package dev.voxelgame.client.render.entity;

import dev.voxelgame.client.animation.AnimationSample;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityPoseTest {
    @Test
    void buildsPartPoseFromAnimationChannels() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:forest_bunny");
        AnimationSample sample = new AnimationSample(Map.of(
                EntityAnimationChannels.ROOT_Y, 0.12f,
                EntityAnimationChannels.rotationZ("left_ear"), 0.25f,
                EntityAnimationChannels.scaleY("tail"), 1.2f
        ));

        EntityPose pose = EntityPose.fromSample(sample, model);

        assertEquals(0.12f, pose.rootYOffset(), 0.001f);
        assertEquals(0.25f, pose.part("left_ear").rotationZ(), 0.001f);
        assertEquals(1.2f, pose.part("tail").scaleY(), 0.001f);
        assertSame(EntityPartPose.IDENTITY, pose.part("missing"));
    }

    @Test
    void emptySampleReturnsIdentityPose() {
        EntityModel model = new EntityModelRegistry().modelFor("voxel:player");

        assertSame(EntityPose.identity(), EntityPose.fromSample(AnimationSample.empty(), model));
    }

    @Test
    void rejectsInvalidScale() {
        assertThrows(IllegalArgumentException.class, () -> new EntityPartPose(
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f
        ));
    }
}
