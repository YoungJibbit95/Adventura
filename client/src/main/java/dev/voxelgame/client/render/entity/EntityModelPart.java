package dev.voxelgame.client.render.entity;

public record EntityModelPart(
        String name,
        float x,
        float y,
        float z,
        float width,
        float height,
        float depth,
        ColorRole colorRole,
        boolean emissive,
        float rotationX,
        float rotationY,
        float rotationZ
) {
    public EntityModelPart {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Entity model part name must not be blank");
        }
        if (width <= 0.0f || height <= 0.0f || depth <= 0.0f) {
            throw new IllegalArgumentException("Entity model part dimensions must be > 0");
        }
        if (colorRole == null) {
            throw new IllegalArgumentException("Entity model part color role must not be null");
        }
    }

    public EntityModelPart(String name, float x, float y, float z, float width, float height, float depth, ColorRole colorRole, boolean emissive) {
        this(name, x, y, z, width, height, depth, colorRole, emissive, 0.0f, 0.0f, 0.0f);
    }

    public enum ColorRole {
        BASE,
        HEAD,
        DETAIL,
        DARK
    }
}
