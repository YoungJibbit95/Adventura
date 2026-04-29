package dev.voxelgame.common.entity;

public record EntityBounds(float width, float height, float depth) {
    public static EntityBounds forType(String typeKey) {
        return switch (typeKey) {
            case "voxel:player" -> new EntityBounds(0.62f, 1.82f, 0.62f);
            case "voxel:cozy_sheep" -> new EntityBounds(0.92f, 0.92f, 1.14f);
            case "voxel:forest_bunny", "voxel:snow_hare" -> new EntityBounds(0.46f, 0.46f, 0.56f);
            case "voxel:moss_snail" -> new EntityBounds(0.54f, 0.30f, 0.72f);
            case "voxel:firefly_swarm", "voxel:mire_wisp" -> new EntityBounds(0.55f, 0.55f, 0.55f);
            case "voxel:little_boar" -> new EntityBounds(0.82f, 0.72f, 1.08f);
            case "voxel:dune_crawler" -> new EntityBounds(0.88f, 0.48f, 1.08f);
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> new EntityBounds(0.92f, 1.12f, 1.18f);
            default -> new EntityBounds(0.70f, 1.10f, 0.70f);
        };
    }

    public static float baseY(EntitySnapshot snapshot) {
        return "voxel:player".equals(snapshot.typeKey())
                ? (float) snapshot.y() - 1.62f
                : (float) snapshot.y();
    }
}
