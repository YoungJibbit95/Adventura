package dev.voxelgame.client.render.entity;

record EntityPartPose(
        float offsetX,
        float offsetY,
        float offsetZ,
        float rotationX,
        float rotationY,
        float rotationZ,
        float scaleX,
        float scaleY,
        float scaleZ
) {
    static final EntityPartPose IDENTITY = new EntityPartPose(
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f);

    static EntityPartPose identity() {
        return IDENTITY;
    }

    EntityPartPose(float offsetX, float offsetY, float offsetZ, float rotationX, float rotationY, float rotationZ) {
        this(offsetX, offsetY, offsetZ, rotationX, rotationY, rotationZ, 1.0f, 1.0f, 1.0f);
    }

    EntityPartPose {
        if (!Float.isFinite(offsetX) || !Float.isFinite(offsetY) || !Float.isFinite(offsetZ)
                || !Float.isFinite(rotationX) || !Float.isFinite(rotationY) || !Float.isFinite(rotationZ)
                || !Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ)) {
            throw new IllegalArgumentException("Entity part pose values must be finite");
        }
        if (scaleX <= 0.0f || scaleY <= 0.0f || scaleZ <= 0.0f) {
            throw new IllegalArgumentException("Entity part pose scale must be > 0");
        }
    }

    EntityPartPose add(EntityPartPose other) {
        return new EntityPartPose(
                offsetX + other.offsetX,
                offsetY + other.offsetY,
                offsetZ + other.offsetZ,
                rotationX + other.rotationX,
                rotationY + other.rotationY,
                rotationZ + other.rotationZ,
                scaleX * other.scaleX,
                scaleY * other.scaleY,
                scaleZ * other.scaleZ);
    }

    boolean isIdentity() {
        return offsetX == 0.0f
                && offsetY == 0.0f
                && offsetZ == 0.0f
                && rotationX == 0.0f
                && rotationY == 0.0f
                && rotationZ == 0.0f
                && scaleX == 1.0f
                && scaleY == 1.0f
                && scaleZ == 1.0f;
    }

    boolean identityPose() {
        return isIdentity();
    }

    static EntityPartPose offset(float x, float y, float z) {
        return new EntityPartPose(x, y, z, 0.0f, 0.0f, 0.0f);
    }

    static EntityPartPose rotation(float x, float y, float z) {
        return new EntityPartPose(0.0f, 0.0f, 0.0f, x, y, z);
    }

    static EntityPartPose scale(float x, float y, float z) {
        return new EntityPartPose(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, x, y, z);
    }
}
