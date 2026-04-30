package dev.voxelgame.client.render.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAnimationLibraryTest {
    private final EntityAnimationLibrary animations = new EntityAnimationLibrary();
    private final EntityModelRegistry models = new EntityModelRegistry();

    @Test
    void walkingCreatureAlternatesLegsFromVelocity() {
        EntityModel model = models.modelFor("voxel:cozy_sheep");
        EntitySnapshot snapshot = new EntitySnapshot(
                0L, "voxel:cozy_sheep", null,
                0.0, 80.0, 0.0,
                0.0f, 0.0f, 10,
                EntitySnapshot.STATE_WANDER,
                1.2, 0.0, 0.0);

        EntityPose pose = animations.poseFor(snapshot, model, 0.08);

        float left = pose.part("front_left_leg").rotationX();
        float right = pose.part("front_right_leg").rotationX();
        assertTrue(Math.abs(left) > 0.05f);
        assertEquals(-Math.signum(left), Math.signum(right), 0.001f);
        assertTrue(pose.rootYOffset() > 0.0f);
    }

    @Test
    void grazingStateLowersHeadAndNeck() {
        EntityModel model = models.modelFor("voxel:forest_grazer");
        EntitySnapshot snapshot = new EntitySnapshot(
                0L, "voxel:forest_grazer", null,
                0.0, 80.0, 0.0,
                0.0f, 0.0f, 10,
                EntitySnapshot.STATE_GRAZE);

        EntityPose pose = animations.poseFor(snapshot, model, 0.9);

        assertTrue(pose.part("head").offsetY() < -0.10f);
        assertTrue(pose.part("neck").rotationX() > 0.45f);
    }

    @Test
    void bunnyFleeUsesFasterHopPlayback() {
        EntityModel model = models.modelFor("voxel:forest_bunny");
        EntitySnapshot idle = new EntitySnapshot(
                0L, "voxel:forest_bunny", null,
                0.0, 80.0, 0.0,
                0.0f, 0.0f, 10,
                EntitySnapshot.STATE_IDLE);
        EntitySnapshot fleeing = new EntitySnapshot(
                0L, "voxel:forest_bunny", null,
                0.0, 80.0, 0.0,
                0.0f, 0.0f, 10,
                EntitySnapshot.STATE_FLEE);

        EntityPose idlePose = animations.poseFor(idle, model, 0.24);
        EntityPose fleePose = animations.poseFor(fleeing, model, 0.24);

        assertTrue(Math.abs(fleePose.part("left_ear").rotationZ()) != Math.abs(idlePose.part("left_ear").rotationZ()));
        assertTrue(fleePose.rootYOffset() != idlePose.rootYOffset());
    }

    @Test
    void fireflyGlowPulseScalesGlowPart() {
        EntityModel model = models.modelFor("voxel:firefly_swarm");
        EntitySnapshot snapshot = new EntitySnapshot(
                0L, "voxel:firefly_swarm", null,
                0.0, 80.0, 0.0,
                0.0f, 0.0f, 4);

        EntityPose pose = animations.poseFor(snapshot, model, 0.9);

        assertTrue(pose.rootYOffset() > 0.10f);
        assertTrue(pose.part("glow").scaleX() > 1.10f);
        assertTrue(pose.part("glow").scaleY() > 1.10f);
    }
}
