package dev.voxelgame.common.world;

public record DimensionSettings(int minY, int maxYExclusive) {
    public static final int SECTION_SIZE = 16;
    public static final DimensionSettings OVERWORLD = new DimensionSettings(-64, 320);

    public DimensionSettings {
        if (maxYExclusive <= minY) {
            throw new IllegalArgumentException("Dimension maxY must be greater than minY");
        }
        if (Math.floorMod(minY, SECTION_SIZE) != 0 || Math.floorMod(maxYExclusive, SECTION_SIZE) != 0) {
            throw new IllegalArgumentException("Dimension bounds must align to 16-block sections");
        }
    }

    public int height() {
        return maxYExclusive - minY;
    }

    public int minSectionY() {
        return Math.floorDiv(minY, SECTION_SIZE);
    }

    public int maxSectionYExclusive() {
        return Math.floorDiv(maxYExclusive, SECTION_SIZE);
    }

    public int sectionCount() {
        return height() / SECTION_SIZE;
    }

    public boolean containsY(int y) {
        return y >= minY && y < maxYExclusive;
    }
}
