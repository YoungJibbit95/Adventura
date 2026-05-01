package dev.voxelgame.common.entity;

public record EntityBounds(float width, float height, float depth) {
    public static EntityBounds forType(String typeKey) {
        if (ItemDropType.isTypeKey(typeKey)) {
            return new EntityBounds(0.36f, 0.36f, 0.36f);
        }
        return switch (typeKey) {
            case "voxel:player" -> new EntityBounds(0.62f, 1.82f, 0.62f);
            case "voxel:cozy_sheep" -> new EntityBounds(0.92f, 0.92f, 1.14f);
            case "voxel:forest_bunny", "voxel:snow_hare" -> new EntityBounds(0.46f, 0.46f, 0.56f);
            case "voxel:moss_snail" -> new EntityBounds(0.54f, 0.30f, 0.72f);
            case "voxel:firefly_swarm", "voxel:mire_wisp" -> new EntityBounds(0.55f, 0.55f, 0.55f);
            case "voxel:little_boar" -> new EntityBounds(0.82f, 0.72f, 1.08f);
            case "voxel:dune_crawler" -> new EntityBounds(0.88f, 0.48f, 1.08f);
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> new EntityBounds(0.92f, 1.12f, 1.18f);
            case "voxel:arrow_projectile" -> new EntityBounds(0.18f, 0.18f, 0.18f);
            default -> new EntityBounds(0.70f, 1.10f, 0.70f);
        };
    }

    public static float baseY(EntitySnapshot snapshot) {
        return "voxel:player".equals(snapshot.typeKey())
                ? (float) snapshot.y() - 1.62f
                : (float) snapshot.y();
    }

    public double minX(double centerX) {
        return centerX - width * 0.5;
    }

    public double maxX(double centerX) {
        return centerX + width * 0.5;
    }

    public double minY(double baseY) {
        return baseY;
    }

    public double maxY(double baseY) {
        return baseY + height;
    }

    public double minZ(double centerZ) {
        return centerZ - depth * 0.5;
    }

    public double maxZ(double centerZ) {
        return centerZ + depth * 0.5;
    }

    public boolean intersectsBlock(double centerX, double baseY, double centerZ, int blockX, int blockY, int blockZ) {
        return minX(centerX) < blockX + 1.0
                && maxX(centerX) > blockX
                && minY(baseY) < blockY + 1.0
                && maxY(baseY) > blockY
                && minZ(centerZ) < blockZ + 1.0
                && maxZ(centerZ) > blockZ;
    }

    public static boolean intersectsBlock(EntitySnapshot snapshot, int blockX, int blockY, int blockZ) {
        EntityBounds bounds = forType(snapshot.typeKey());
        return bounds.intersectsBlock(snapshot.x(), baseY(snapshot), snapshot.z(), blockX, blockY, blockZ);
    }
}
