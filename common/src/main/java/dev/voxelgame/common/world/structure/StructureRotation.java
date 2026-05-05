package dev.voxelgame.common.world.structure;

import java.util.List;

public enum StructureRotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    public static List<StructureRotation> cardinal() {
        return List.of(NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90);
    }

    public static List<StructureRotation> noneOnly() {
        return List.of(NONE);
    }
}
