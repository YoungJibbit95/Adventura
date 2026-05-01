package dev.voxelgame.common.engine;

public enum EngineJobPriority {
    PLAYER_ACTION(0),
    VISIBLE_CHUNK(10),
    PREVIEW(20),
    BACKGROUND(30);

    private final int sortOrder;

    EngineJobPriority(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public boolean outranks(EngineJobPriority other) {
        return sortOrder < (other == null ? BACKGROUND.sortOrder : other.sortOrder);
    }
}
