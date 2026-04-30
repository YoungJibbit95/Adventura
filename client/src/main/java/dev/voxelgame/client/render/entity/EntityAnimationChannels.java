package dev.voxelgame.client.render.entity;

final class EntityAnimationChannels {
    static final String ROOT_Y = "entity.root.y";

    private EntityAnimationChannels() {
    }

    static String x(String partName) {
        return part(partName, "x");
    }

    static String y(String partName) {
        return part(partName, "y");
    }

    static String z(String partName) {
        return part(partName, "z");
    }

    static String rotationX(String partName) {
        return part(partName, "rotation.x");
    }

    static String rotationY(String partName) {
        return part(partName, "rotation.y");
    }

    static String rotationZ(String partName) {
        return part(partName, "rotation.z");
    }

    static String scaleX(String partName) {
        return part(partName, "scale.x");
    }

    static String scaleY(String partName) {
        return part(partName, "scale.y");
    }

    static String scaleZ(String partName) {
        return part(partName, "scale.z");
    }

    private static String part(String partName, String channel) {
        return "entity.part." + partName + "." + channel;
    }
}
