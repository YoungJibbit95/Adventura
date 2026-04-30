package dev.voxelgame.client.render.entity;

import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.ItemDropType;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityModelRegistry {
    private final Map<String, EntityModel> models = new ConcurrentHashMap<>();

    public EntityModel modelFor(String typeKey) {
        return models.computeIfAbsent(typeKey, EntityModelRegistry::createModel);
    }

    public int cachedModelCount() {
        return models.size();
    }

    public static Vector3f colorFor(String typeKey, EntityModelPart.ColorRole role) {
        return switch (role) {
            case BASE -> baseColor(typeKey);
            case HEAD -> headColor(typeKey);
            case DETAIL -> detailColor(typeKey);
            case DARK -> darkColor(typeKey);
        };
    }

    private static EntityModel createModel(String typeKey) {
        if (ItemDropType.isTypeKey(typeKey)) {
            return itemDrop(typeKey);
        }
        if ("voxel:player".equals(typeKey)) {
            return humanoid(typeKey);
        }
        if ("voxel:firefly_swarm".equals(typeKey) || "voxel:mire_wisp".equals(typeKey)) {
            return wisp(typeKey);
        }
        if ("voxel:forest_bunny".equals(typeKey) || "voxel:snow_hare".equals(typeKey)) {
            return bunny(typeKey);
        }
        if ("voxel:moss_snail".equals(typeKey)) {
            return snail(typeKey);
        }
        if ("voxel:little_boar".equals(typeKey)) {
            return boar(typeKey);
        }
        if ("voxel:dune_crawler".equals(typeKey)) {
            return crawler(typeKey);
        }
        if ("voxel:forest_grazer".equals(typeKey) || "voxel:meadow_grazer".equals(typeKey)) {
            return grazer(typeKey);
        }
        return creature(typeKey, EntityBounds.forType(typeKey));
    }

    private static EntityModel humanoid(String typeKey) {
        return new EntityModel(typeKey, List.of(
                part("left_leg", -0.14f, 0.0f, 0.0f, 0.20f, 0.64f, 0.22f, EntityModelPart.ColorRole.DARK),
                part("right_leg", 0.14f, 0.0f, 0.0f, 0.20f, 0.64f, 0.22f, EntityModelPart.ColorRole.DARK),
                part("body", 0.0f, 0.62f, 0.0f, 0.50f, 0.78f, 0.30f, EntityModelPart.ColorRole.BASE),
                part("left_arm", -0.40f, 0.68f, 0.0f, 0.16f, 0.70f, 0.18f, EntityModelPart.ColorRole.HEAD),
                part("right_arm", 0.40f, 0.68f, 0.0f, 0.16f, 0.70f, 0.18f, EntityModelPart.ColorRole.HEAD),
                part("head", 0.0f, 1.42f, 0.0f, 0.44f, 0.44f, 0.44f, EntityModelPart.ColorRole.HEAD)
        ), false);
    }

    private static EntityModel creature(String typeKey, EntityBounds bounds) {
        float bodyWidth = bounds.width() * 0.82f;
        float bodyHeight = bounds.height() * 0.62f;
        float bodyDepth = bounds.depth() * 0.78f;
        float legHeight = Math.max(0.10f, bounds.height() * 0.34f);
        float legWidth = Math.max(0.07f, bounds.width() * 0.18f);
        float legDepth = Math.max(0.07f, bounds.depth() * 0.16f);
        return new EntityModel(typeKey, List.of(
                part("body", 0.0f, legHeight, 0.0f, bodyWidth, bodyHeight, bodyDepth, EntityModelPart.ColorRole.BASE),
                part("head", 0.0f, legHeight + bodyHeight * 0.34f, -bounds.depth() * 0.42f, bounds.width() * 0.44f, bounds.height() * 0.42f, bounds.depth() * 0.36f, EntityModelPart.ColorRole.HEAD),
                part("front_left_leg", -bounds.width() * 0.24f, 0.0f, -bounds.depth() * 0.22f, legWidth, legHeight, legDepth, EntityModelPart.ColorRole.BASE),
                part("front_right_leg", bounds.width() * 0.24f, 0.0f, -bounds.depth() * 0.22f, legWidth, legHeight, legDepth, EntityModelPart.ColorRole.BASE),
                part("back_left_leg", -bounds.width() * 0.24f, 0.0f, bounds.depth() * 0.22f, legWidth, legHeight, legDepth, EntityModelPart.ColorRole.BASE),
                part("back_right_leg", bounds.width() * 0.24f, 0.0f, bounds.depth() * 0.22f, legWidth, legHeight, legDepth, EntityModelPart.ColorRole.BASE)
        ), true);
    }

    private static EntityModel bunny(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        float bodyWidth = bounds.width() * 0.78f;
        float bodyHeight = bounds.height() * 0.46f;
        float bodyDepth = bounds.depth() * 0.78f;
        return new EntityModel(typeKey, List.of(
                part("body", 0.0f, 0.08f, 0.04f, bodyWidth, bodyHeight, bodyDepth, EntityModelPart.ColorRole.BASE),
                part("head", 0.0f, 0.26f, -bounds.depth() * 0.34f, bounds.width() * 0.52f, bounds.height() * 0.44f, bounds.depth() * 0.42f, EntityModelPart.ColorRole.HEAD),
                rotatedPart("left_ear", -bounds.width() * 0.11f, 0.54f, -bounds.depth() * 0.36f, bounds.width() * 0.12f, bounds.height() * 0.44f, bounds.depth() * 0.10f, EntityModelPart.ColorRole.HEAD, -0.20f, 0.0f, -0.08f),
                rotatedPart("right_ear", bounds.width() * 0.11f, 0.54f, -bounds.depth() * 0.36f, bounds.width() * 0.12f, bounds.height() * 0.44f, bounds.depth() * 0.10f, EntityModelPart.ColorRole.HEAD, -0.20f, 0.0f, 0.08f),
                part("tail", 0.0f, 0.24f, bounds.depth() * 0.44f, bounds.width() * 0.18f, bounds.height() * 0.18f, bounds.depth() * 0.16f, EntityModelPart.ColorRole.DETAIL),
                part("front_paws", 0.0f, 0.0f, -bounds.depth() * 0.20f, bounds.width() * 0.46f, bounds.height() * 0.16f, bounds.depth() * 0.12f, EntityModelPart.ColorRole.BASE),
                part("back_paws", 0.0f, 0.0f, bounds.depth() * 0.24f, bounds.width() * 0.56f, bounds.height() * 0.18f, bounds.depth() * 0.16f, EntityModelPart.ColorRole.BASE)
        ), true);
    }

    private static EntityModel snail(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        return new EntityModel(typeKey, List.of(
                part("body", 0.0f, 0.02f, -bounds.depth() * 0.06f, bounds.width() * 0.72f, bounds.height() * 0.42f, bounds.depth() * 0.82f, EntityModelPart.ColorRole.BASE),
                part("shell", 0.0f, bounds.height() * 0.20f, bounds.depth() * 0.14f, bounds.width() * 0.64f, bounds.height() * 0.72f, bounds.depth() * 0.56f, EntityModelPart.ColorRole.DARK),
                part("head", 0.0f, bounds.height() * 0.16f, -bounds.depth() * 0.48f, bounds.width() * 0.48f, bounds.height() * 0.38f, bounds.depth() * 0.30f, EntityModelPart.ColorRole.HEAD),
                rotatedPart("left_feeler", -bounds.width() * 0.16f, bounds.height() * 0.50f, -bounds.depth() * 0.58f, bounds.width() * 0.07f, bounds.height() * 0.36f, bounds.depth() * 0.06f, EntityModelPart.ColorRole.HEAD, -0.38f, 0.0f, -0.12f),
                rotatedPart("right_feeler", bounds.width() * 0.16f, bounds.height() * 0.50f, -bounds.depth() * 0.58f, bounds.width() * 0.07f, bounds.height() * 0.36f, bounds.depth() * 0.06f, EntityModelPart.ColorRole.HEAD, -0.38f, 0.0f, 0.12f)
        ), true);
    }

    private static EntityModel boar(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        float legHeight = bounds.height() * 0.34f;
        return new EntityModel(typeKey, List.of(
                part("body", 0.0f, legHeight, 0.04f, bounds.width() * 0.86f, bounds.height() * 0.58f, bounds.depth() * 0.82f, EntityModelPart.ColorRole.BASE),
                part("head", 0.0f, legHeight + bounds.height() * 0.12f, -bounds.depth() * 0.42f, bounds.width() * 0.52f, bounds.height() * 0.46f, bounds.depth() * 0.38f, EntityModelPart.ColorRole.HEAD),
                part("snout", 0.0f, legHeight + bounds.height() * 0.06f, -bounds.depth() * 0.68f, bounds.width() * 0.34f, bounds.height() * 0.20f, bounds.depth() * 0.24f, EntityModelPart.ColorRole.DETAIL),
                rotatedPart("left_tusk", -bounds.width() * 0.19f, legHeight + bounds.height() * 0.02f, -bounds.depth() * 0.70f, bounds.width() * 0.07f, bounds.height() * 0.18f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DETAIL, 0.10f, 0.0f, -0.35f),
                rotatedPart("right_tusk", bounds.width() * 0.19f, legHeight + bounds.height() * 0.02f, -bounds.depth() * 0.70f, bounds.width() * 0.07f, bounds.height() * 0.18f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DETAIL, 0.10f, 0.0f, 0.35f),
                rotatedPart("left_ear", -bounds.width() * 0.24f, legHeight + bounds.height() * 0.40f, -bounds.depth() * 0.36f, bounds.width() * 0.12f, bounds.height() * 0.18f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, -0.12f, 0.0f, -0.32f),
                rotatedPart("right_ear", bounds.width() * 0.24f, legHeight + bounds.height() * 0.40f, -bounds.depth() * 0.36f, bounds.width() * 0.12f, bounds.height() * 0.18f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, -0.12f, 0.0f, 0.32f),
                part("front_left_leg", -bounds.width() * 0.25f, 0.0f, -bounds.depth() * 0.22f, bounds.width() * 0.15f, legHeight, bounds.depth() * 0.14f, EntityModelPart.ColorRole.DARK),
                part("front_right_leg", bounds.width() * 0.25f, 0.0f, -bounds.depth() * 0.22f, bounds.width() * 0.15f, legHeight, bounds.depth() * 0.14f, EntityModelPart.ColorRole.DARK),
                part("back_left_leg", -bounds.width() * 0.25f, 0.0f, bounds.depth() * 0.26f, bounds.width() * 0.15f, legHeight, bounds.depth() * 0.14f, EntityModelPart.ColorRole.DARK),
                part("back_right_leg", bounds.width() * 0.25f, 0.0f, bounds.depth() * 0.26f, bounds.width() * 0.15f, legHeight, bounds.depth() * 0.14f, EntityModelPart.ColorRole.DARK)
        ), true);
    }

    private static EntityModel crawler(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        float legY = bounds.height() * 0.06f;
        return new EntityModel(typeKey, List.of(
                part("low_body", 0.0f, bounds.height() * 0.08f, 0.04f, bounds.width() * 0.82f, bounds.height() * 0.44f, bounds.depth() * 0.74f, EntityModelPart.ColorRole.BASE),
                part("carapace", 0.0f, bounds.height() * 0.26f, 0.10f, bounds.width() * 0.72f, bounds.height() * 0.40f, bounds.depth() * 0.60f, EntityModelPart.ColorRole.DARK),
                part("head", 0.0f, bounds.height() * 0.12f, -bounds.depth() * 0.46f, bounds.width() * 0.46f, bounds.height() * 0.28f, bounds.depth() * 0.24f, EntityModelPart.ColorRole.HEAD),
                rotatedPart("left_front_leg", -bounds.width() * 0.46f, legY, -bounds.depth() * 0.24f, bounds.width() * 0.30f, bounds.height() * 0.10f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, 0.0f, 0.0f, 0.28f),
                rotatedPart("right_front_leg", bounds.width() * 0.46f, legY, -bounds.depth() * 0.24f, bounds.width() * 0.30f, bounds.height() * 0.10f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, 0.0f, 0.0f, -0.28f),
                rotatedPart("left_back_leg", -bounds.width() * 0.46f, legY, bounds.depth() * 0.24f, bounds.width() * 0.30f, bounds.height() * 0.10f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, 0.0f, 0.0f, -0.20f),
                rotatedPart("right_back_leg", bounds.width() * 0.46f, legY, bounds.depth() * 0.24f, bounds.width() * 0.30f, bounds.height() * 0.10f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK, 0.0f, 0.0f, 0.20f)
        ), true);
    }

    private static EntityModel grazer(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        float legHeight = bounds.height() * 0.44f;
        return new EntityModel(typeKey, List.of(
                part("body", 0.0f, legHeight, 0.08f, bounds.width() * 0.80f, bounds.height() * 0.44f, bounds.depth() * 0.78f, EntityModelPart.ColorRole.BASE),
                rotatedPart("neck", 0.0f, legHeight + bounds.height() * 0.34f, -bounds.depth() * 0.30f, bounds.width() * 0.28f, bounds.height() * 0.50f, bounds.depth() * 0.22f, EntityModelPart.ColorRole.BASE, -0.34f, 0.0f, 0.0f),
                part("head", 0.0f, legHeight + bounds.height() * 0.62f, -bounds.depth() * 0.48f, bounds.width() * 0.42f, bounds.height() * 0.28f, bounds.depth() * 0.30f, EntityModelPart.ColorRole.HEAD),
                rotatedPart("left_ear", -bounds.width() * 0.20f, legHeight + bounds.height() * 0.72f, -bounds.depth() * 0.46f, bounds.width() * 0.12f, bounds.height() * 0.16f, bounds.depth() * 0.07f, EntityModelPart.ColorRole.HEAD, -0.12f, 0.0f, -0.30f),
                rotatedPart("right_ear", bounds.width() * 0.20f, legHeight + bounds.height() * 0.72f, -bounds.depth() * 0.46f, bounds.width() * 0.12f, bounds.height() * 0.16f, bounds.depth() * 0.07f, EntityModelPart.ColorRole.HEAD, -0.12f, 0.0f, 0.30f),
                part("tail", 0.0f, legHeight + bounds.height() * 0.16f, bounds.depth() * 0.50f, bounds.width() * 0.10f, bounds.height() * 0.30f, bounds.depth() * 0.08f, EntityModelPart.ColorRole.DARK),
                part("front_left_leg", -bounds.width() * 0.23f, 0.0f, -bounds.depth() * 0.24f, bounds.width() * 0.13f, legHeight, bounds.depth() * 0.12f, EntityModelPart.ColorRole.DARK),
                part("front_right_leg", bounds.width() * 0.23f, 0.0f, -bounds.depth() * 0.24f, bounds.width() * 0.13f, legHeight, bounds.depth() * 0.12f, EntityModelPart.ColorRole.DARK),
                part("back_left_leg", -bounds.width() * 0.23f, 0.0f, bounds.depth() * 0.24f, bounds.width() * 0.13f, legHeight, bounds.depth() * 0.12f, EntityModelPart.ColorRole.DARK),
                part("back_right_leg", bounds.width() * 0.23f, 0.0f, bounds.depth() * 0.24f, bounds.width() * 0.13f, legHeight, bounds.depth() * 0.12f, EntityModelPart.ColorRole.DARK)
        ), true);
    }

    private static EntityModel wisp(String typeKey) {
        EntityBounds bounds = EntityBounds.forType(typeKey);
        return new EntityModel(typeKey, List.of(
                new EntityModelPart("core", 0.0f, bounds.height() * 0.18f, 0.0f, bounds.width() * 0.62f, bounds.height() * 0.62f, bounds.depth() * 0.62f, EntityModelPart.ColorRole.HEAD, true),
                new EntityModelPart("glow", 0.0f, bounds.height() * 0.12f, 0.0f, bounds.width(), bounds.height(), bounds.depth(), EntityModelPart.ColorRole.DETAIL, true)
        ), true);
    }

    private static EntityModel itemDrop(String typeKey) {
        return new EntityModel(typeKey, List.of(
                rotatedPart("item", 0.0f, 0.08f, 0.0f, 0.28f, 0.28f, 0.28f, EntityModelPart.ColorRole.BASE, 0.0f, 0.35f, 0.0f),
                rotatedPart("glint", 0.0f, 0.27f, 0.0f, 0.20f, 0.04f, 0.20f, EntityModelPart.ColorRole.DETAIL, 0.0f, 0.35f, 0.0f)
        ), true);
    }

    private static EntityModelPart part(String name, float x, float y, float z, float width, float height, float depth, EntityModelPart.ColorRole role) {
        return new EntityModelPart(name, x, y, z, width, height, depth, role, false);
    }

    private static EntityModelPart rotatedPart(String name, float x, float y, float z, float width, float height, float depth, EntityModelPart.ColorRole role, float rotationX, float rotationY, float rotationZ) {
        return new EntityModelPart(name, x, y, z, width, height, depth, role, false, rotationX, rotationY, rotationZ);
    }

    private static Vector3f baseColor(String typeKey) {
        if (ItemDropType.isTypeKey(typeKey)) {
            return itemDropColor(typeKey);
        }
        return switch (typeKey) {
            case "voxel:cozy_sheep" -> new Vector3f(0.82f, 0.78f, 0.66f);
            case "voxel:forest_bunny" -> new Vector3f(0.45f, 0.34f, 0.26f);
            case "voxel:moss_snail" -> new Vector3f(0.20f, 0.38f, 0.22f);
            case "voxel:firefly_swarm" -> new Vector3f(0.80f, 0.76f, 0.18f);
            case "voxel:little_boar" -> new Vector3f(0.42f, 0.26f, 0.18f);
            case "voxel:snow_hare" -> new Vector3f(0.62f, 0.68f, 0.70f);
            case "voxel:mire_wisp" -> new Vector3f(0.10f, 0.36f, 0.32f);
            case "voxel:dune_crawler" -> new Vector3f(0.54f, 0.38f, 0.18f);
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> new Vector3f(0.18f, 0.28f, 0.20f);
            case "voxel:player" -> new Vector3f(0.18f, 0.24f, 0.30f);
            default -> new Vector3f(0.24f, 0.28f, 0.20f);
        };
    }

    private static Vector3f headColor(String typeKey) {
        return switch (typeKey) {
            case "voxel:cozy_sheep" -> new Vector3f(0.96f, 0.92f, 0.78f);
            case "voxel:forest_bunny" -> new Vector3f(0.72f, 0.58f, 0.42f);
            case "voxel:moss_snail" -> new Vector3f(0.46f, 0.64f, 0.34f);
            case "voxel:firefly_swarm" -> new Vector3f(1.0f, 0.94f, 0.34f);
            case "voxel:little_boar" -> new Vector3f(0.62f, 0.42f, 0.30f);
            case "voxel:snow_hare" -> new Vector3f(0.92f, 0.96f, 0.94f);
            case "voxel:mire_wisp" -> new Vector3f(0.28f, 0.92f, 0.70f);
            case "voxel:dune_crawler" -> new Vector3f(0.82f, 0.62f, 0.28f);
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> new Vector3f(0.46f, 0.58f, 0.34f);
            case "voxel:player" -> new Vector3f(0.74f, 0.84f, 0.72f);
            default -> new Vector3f(0.62f, 0.72f, 0.44f);
        };
    }

    private static Vector3f detailColor(String typeKey) {
        if (ItemDropType.isTypeKey(typeKey)) {
            return new Vector3f(0.86f, 0.96f, 0.74f);
        }
        return switch (typeKey) {
            case "voxel:forest_bunny" -> new Vector3f(0.84f, 0.72f, 0.56f);
            case "voxel:snow_hare" -> new Vector3f(0.96f, 0.96f, 0.88f);
            case "voxel:firefly_swarm" -> new Vector3f(1.0f, 0.86f, 0.26f);
            case "voxel:little_boar" -> new Vector3f(0.82f, 0.68f, 0.50f);
            case "voxel:mire_wisp" -> new Vector3f(0.18f, 0.80f, 0.66f);
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> new Vector3f(0.64f, 0.72f, 0.46f);
            default -> new Vector3f(0.66f, 0.72f, 0.50f);
        };
    }

    private static Vector3f darkColor(String typeKey) {
        if ("voxel:player".equals(typeKey)) {
            return new Vector3f(0.12f, 0.18f, 0.24f);
        }
        Vector3f base = baseColor(typeKey);
        return new Vector3f(base).mul(0.68f);
    }

    private static Vector3f itemDropColor(String typeKey) {
        return switch (ItemDropType.itemKey(typeKey).orElse("")) {
            case "voxel:moss_clump" -> new Vector3f(0.24f, 0.48f, 0.22f);
            case "voxel:slime_drop" -> new Vector3f(0.34f, 0.86f, 0.54f);
            case "voxel:glow_crystal" -> new Vector3f(0.42f, 0.92f, 0.86f);
            case "voxel:berries" -> new Vector3f(0.72f, 0.16f, 0.22f);
            case "voxel:mushroom" -> new Vector3f(0.72f, 0.36f, 0.28f);
            default -> new Vector3f(0.62f, 0.56f, 0.40f);
        };
    }
}
