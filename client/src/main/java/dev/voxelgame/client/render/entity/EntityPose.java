package dev.voxelgame.client.render.entity;

import dev.voxelgame.client.animation.AnimationSample;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class EntityPose {
    private static final EntityPose IDENTITY = new EntityPose(0.0f, Map.of());

    private final float rootYOffset;
    private final Map<String, EntityPartPose> parts;

    private EntityPose(float rootYOffset, Map<String, EntityPartPose> parts) {
        this.rootYOffset = rootYOffset;
        this.parts = Map.copyOf(parts);
    }

    static EntityPose identity() {
        return IDENTITY;
    }

    static Builder builder(float rootYOffset) {
        return new Builder(rootYOffset);
    }

    static EntityPose fromSample(AnimationSample sample, EntityModel model) {
        float rootY = sample.value(EntityAnimationChannels.ROOT_Y, 0.0f);
        Map<String, EntityPartPose> parts = new LinkedHashMap<>();
        for (EntityModelPart part : model.parts()) {
            EntityPartPose pose = new EntityPartPose(
                    sample.value(EntityAnimationChannels.x(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.y(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.z(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.rotationX(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.rotationY(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.rotationZ(part.name()), 0.0f),
                    sample.value(EntityAnimationChannels.scaleX(part.name()), 1.0f),
                    sample.value(EntityAnimationChannels.scaleY(part.name()), 1.0f),
                    sample.value(EntityAnimationChannels.scaleZ(part.name()), 1.0f));
            if (!pose.isIdentity()) {
                parts.put(part.name(), pose);
            }
        }
        if (rootY == 0.0f && parts.isEmpty()) {
            return identity();
        }
        return new EntityPose(rootY, parts);
    }

    float rootYOffset() {
        return rootYOffset;
    }

    EntityPartPose part(String name) {
        return parts.getOrDefault(name, EntityPartPose.IDENTITY);
    }

    static final class Builder {
        private final float rootYOffset;
        private final Map<String, EntityPartPose> parts = new HashMap<>();

        private Builder(float rootYOffset) {
            this.rootYOffset = rootYOffset;
        }

        Builder add(String partName, EntityPartPose pose) {
            if (partName == null || partName.isBlank()) {
                throw new IllegalArgumentException("Entity pose part name must not be blank");
            }
            parts.merge(partName, pose, EntityPartPose::add);
            return this;
        }

        EntityPose build() {
            if (rootYOffset == 0.0f && parts.isEmpty()) {
                return identity();
            }
            return new EntityPose(rootYOffset, parts);
        }
    }
}
