package dev.voxelgame.client.render.entity;

import dev.voxelgame.client.animation.AnimationClip;
import dev.voxelgame.client.animation.AnimationCurve;
import dev.voxelgame.client.animation.AnimationLoopMode;
import dev.voxelgame.client.animation.AnimationSample;
import dev.voxelgame.client.animation.FloatAnimationTrack;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;

public final class EntityAnimationLibrary {
    private final AnimationClip ambientIdle = ambientIdle();
    private final AnimationClip humanoidIdle = humanoidIdle();
    private final AnimationClip humanoidWalk = humanoidWalk();
    private final AnimationClip creatureWalk = creatureWalk();
    private final AnimationClip bunnyHop = bunnyHop();
    private final AnimationClip graze = graze();
    private final AnimationClip snailCrawl = snailCrawl();
    private final AnimationClip boarSniff = boarSniff();
    private final AnimationClip fireflyFloat = fireflyFloat();

    public EntityPose poseFor(EntitySnapshot snapshot, EntityModel model, double timeSeconds) {
        AnimationClip clip = clipFor(snapshot, model);
        if (clip == null) {
            return EntityPose.identity();
        }
        double sampleTime = timeSeconds * playbackSpeed(snapshot) + phaseOffset(snapshot);
        AnimationSample sample = clip.sample(sampleTime);
        return EntityPose.fromSample(sample, model);
    }

    AnimationClip clipFor(EntitySnapshot snapshot, EntityModel model) {
        String typeKey = snapshot.typeKey();
        if (ItemDropType.isTypeKey(typeKey)) {
            return ambientIdle;
        }
        if ("voxel:player".equals(typeKey)) {
            return moving(snapshot) ? humanoidWalk : humanoidIdle;
        }
        if ("voxel:firefly_swarm".equals(typeKey) || "voxel:mire_wisp".equals(typeKey)) {
            return fireflyFloat;
        }
        if ("voxel:forest_bunny".equals(typeKey) || "voxel:snow_hare".equals(typeKey)) {
            return bunnyHop;
        }
        if ("voxel:moss_snail".equals(typeKey)) {
            return snailCrawl;
        }
        if ("voxel:little_boar".equals(typeKey)) {
            return moving(snapshot) ? creatureWalk : boarSniff;
        }
        if (EntitySnapshot.STATE_GRAZE.equals(snapshot.stateKey())) {
            return graze;
        }
        if (moving(snapshot) && model.ambientBob()) {
            return creatureWalk;
        }
        return model.ambientBob() ? ambientIdle : null;
    }

    private static double playbackSpeed(EntitySnapshot snapshot) {
        double speedBoost = Math.min(0.75, horizontalSpeed(snapshot) * 0.18);
        double stateSpeed = switch (snapshot.stateKey()) {
            case EntitySnapshot.STATE_FLEE -> 1.35;
            case EntitySnapshot.STATE_FOLLOW, EntitySnapshot.STATE_WANDER -> 1.12;
            default -> 1.0;
        };
        return stateSpeed + speedBoost;
    }

    private static double horizontalSpeed(EntitySnapshot snapshot) {
        return Math.hypot(snapshot.velocityX(), snapshot.velocityZ());
    }

    private static boolean moving(EntitySnapshot snapshot) {
        return horizontalSpeed(snapshot) > 0.03
                || EntitySnapshot.STATE_FLEE.equals(snapshot.stateKey())
                || EntitySnapshot.STATE_FOLLOW.equals(snapshot.stateKey())
                || EntitySnapshot.STATE_WANDER.equals(snapshot.stateKey());
    }

    private static double phaseOffset(EntitySnapshot snapshot) {
        return Math.floorMod(snapshot.entityId(), 997L) * 0.013;
    }

    private static AnimationClip ambientIdle() {
        return AnimationClip.named("entity-ambient-idle")
                .duration(1.6)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.4, 0.055f, AnimationCurve.SMOOTH_STEP)
                        .key(0.8, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(1.2, 0.024f, AnimationCurve.SMOOTH_STEP)
                        .key(1.6, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("head"))
                        .key(0.0, -0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(0.8, 0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(1.6, -0.025f)
                        .build())
                .build();
    }

    private static AnimationClip humanoidIdle() {
        return AnimationClip.named("entity-humanoid-idle")
                .duration(1.8)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.9, 0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(1.8, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("head"))
                        .key(0.0, -0.018f, AnimationCurve.SMOOTH_STEP)
                        .key(0.9, 0.018f, AnimationCurve.SMOOTH_STEP)
                        .key(1.8, -0.018f)
                        .build())
                .build();
    }

    private static AnimationClip humanoidWalk() {
        return AnimationClip.named("entity-humanoid-walk")
                .duration(0.72)
                .loopMode(AnimationLoopMode.LOOP)
                .track(swing("left_leg", 0.36f, false, 0.72))
                .track(swing("right_leg", 0.36f, true, 0.72))
                .track(swing("left_arm", 0.30f, true, 0.72))
                .track(swing("right_arm", 0.30f, false, 0.72))
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.18, 0.035f, AnimationCurve.SMOOTH_STEP)
                        .key(0.36, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.54, 0.035f, AnimationCurve.SMOOTH_STEP)
                        .key(0.72, 0.0f)
                        .build())
                .build();
    }

    private static AnimationClip creatureWalk() {
        return AnimationClip.named("entity-creature-walk")
                .duration(0.88)
                .loopMode(AnimationLoopMode.LOOP)
                .track(swing("front_left_leg", 0.28f, false, 0.88))
                .track(swing("front_right_leg", 0.28f, true, 0.88))
                .track(swing("back_left_leg", 0.24f, true, 0.88))
                .track(swing("back_right_leg", 0.24f, false, 0.88))
                .track(swing("left_front_leg", 0.22f, false, 0.88))
                .track(swing("right_front_leg", 0.22f, true, 0.88))
                .track(swing("left_back_leg", 0.18f, true, 0.88))
                .track(swing("right_back_leg", 0.18f, false, 0.88))
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.22, 0.035f, AnimationCurve.SMOOTH_STEP)
                        .key(0.44, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.66, 0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(0.88, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("head"))
                        .key(0.0, -0.035f, AnimationCurve.SMOOTH_STEP)
                        .key(0.44, 0.045f, AnimationCurve.SMOOTH_STEP)
                        .key(0.88, -0.035f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("tail"))
                        .key(0.0, -0.10f, AnimationCurve.SMOOTH_STEP)
                        .key(0.44, 0.10f, AnimationCurve.SMOOTH_STEP)
                        .key(0.88, -0.10f)
                        .build())
                .build();
    }

    private static AnimationClip bunnyHop() {
        return AnimationClip.named("entity-bunny-hop")
                .duration(0.92)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(0.24, 0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(0.58, 0.045f, AnimationCurve.SMOOTH_STEP)
                        .key(0.92, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("left_ear"))
                        .key(0.0, -0.08f, AnimationCurve.SMOOTH_STEP)
                        .key(0.46, 0.10f, AnimationCurve.SMOOTH_STEP)
                        .key(0.92, -0.08f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("right_ear"))
                        .key(0.0, 0.08f, AnimationCurve.SMOOTH_STEP)
                        .key(0.46, -0.10f, AnimationCurve.SMOOTH_STEP)
                        .key(0.92, 0.08f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("front_paws"))
                        .key(0.0, 0.10f, AnimationCurve.SMOOTH_STEP)
                        .key(0.42, -0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(0.92, 0.10f)
                        .build())
                .build();
    }

    private static AnimationClip graze() {
        return AnimationClip.named("entity-graze")
                .duration(2.4)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("neck"))
                        .key(0.0, 0.18f, AnimationCurve.SMOOTH_STEP)
                        .key(0.72, 0.62f, AnimationCurve.SMOOTH_STEP)
                        .key(1.7, 0.62f, AnimationCurve.SMOOTH_STEP)
                        .key(2.4, 0.18f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.y("head"))
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.72, -0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(1.7, -0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(2.4, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("tail"))
                        .key(0.0, -0.08f, AnimationCurve.SMOOTH_STEP)
                        .key(1.2, 0.08f, AnimationCurve.SMOOTH_STEP)
                        .key(2.4, -0.08f)
                        .build())
                .build();
    }

    private static AnimationClip snailCrawl() {
        return AnimationClip.named("entity-snail-crawl")
                .duration(2.8)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.y("shell"))
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(1.4, 0.026f, AnimationCurve.SMOOTH_STEP)
                        .key(2.8, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.z("head"))
                        .key(0.0, -0.035f, AnimationCurve.SMOOTH_STEP)
                        .key(1.4, 0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(2.8, -0.035f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("left_feeler"))
                        .key(0.0, -0.05f, AnimationCurve.SMOOTH_STEP)
                        .key(1.4, 0.04f, AnimationCurve.SMOOTH_STEP)
                        .key(2.8, -0.05f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("right_feeler"))
                        .key(0.0, 0.04f, AnimationCurve.SMOOTH_STEP)
                        .key(1.4, -0.05f, AnimationCurve.SMOOTH_STEP)
                        .key(2.8, 0.04f)
                        .build())
                .build();
    }

    private static AnimationClip boarSniff() {
        return AnimationClip.named("entity-boar-sniff")
                .duration(1.35)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationX("head"))
                        .key(0.0, -0.09f, AnimationCurve.SMOOTH_STEP)
                        .key(0.55, 0.08f, AnimationCurve.SMOOTH_STEP)
                        .key(1.35, -0.09f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.y("snout"))
                        .key(0.0, -0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(0.55, 0.025f, AnimationCurve.SMOOTH_STEP)
                        .key(1.35, -0.025f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("left_ear"))
                        .key(0.0, -0.05f, AnimationCurve.SMOOTH_STEP)
                        .key(0.67, 0.03f, AnimationCurve.SMOOTH_STEP)
                        .key(1.35, -0.05f)
                        .build())
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationZ("right_ear"))
                        .key(0.0, 0.05f, AnimationCurve.SMOOTH_STEP)
                        .key(0.67, -0.03f, AnimationCurve.SMOOTH_STEP)
                        .key(1.35, 0.05f)
                        .build())
                .build();
    }

    private static AnimationClip fireflyFloat() {
        return AnimationClip.named("entity-firefly-float")
                .duration(1.8)
                .loopMode(AnimationLoopMode.LOOP)
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.ROOT_Y)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.9, 0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(1.8, 0.0f)
                        .build())
                .track(partScale("glow", EntityAnimationChannels::scaleX))
                .track(partScale("glow", EntityAnimationChannels::scaleY))
                .track(partScale("glow", EntityAnimationChannels::scaleZ))
                .track(FloatAnimationTrack.channel(EntityAnimationChannels.rotationY("core"))
                        .key(0.0, -0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(0.9, 0.16f, AnimationCurve.SMOOTH_STEP)
                        .key(1.8, -0.16f)
                        .build())
                .build();
    }

    private static FloatAnimationTrack swing(String partName, float amplitude, boolean inverted, double duration) {
        float start = inverted ? amplitude : -amplitude;
        float middle = -start;
        return FloatAnimationTrack.channel(EntityAnimationChannels.rotationX(partName))
                .key(0.0, start, AnimationCurve.SMOOTH_STEP)
                .key(duration * 0.5, middle, AnimationCurve.SMOOTH_STEP)
                .key(duration, start)
                .build();
    }

    private static FloatAnimationTrack partScale(String partName, Channel channel) {
        return FloatAnimationTrack.channel(channel.name(partName))
                .key(0.0, 1.0f, AnimationCurve.SMOOTH_STEP)
                .key(0.9, 1.16f, AnimationCurve.SMOOTH_STEP)
                .key(1.8, 1.0f)
                .build();
    }

    @FunctionalInterface
    private interface Channel {
        String name(String partName);
    }
}
