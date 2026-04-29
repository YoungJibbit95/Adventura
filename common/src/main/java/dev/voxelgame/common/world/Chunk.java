package dev.voxelgame.common.world;

public final class Chunk {
    private final ChunkPos pos;
    private final DimensionSettings dimension;
    private final ChunkSection[] sections;

    public Chunk(ChunkPos pos, DimensionSettings dimension) {
        this.pos = pos;
        this.dimension = dimension;
        this.sections = new ChunkSection[dimension.sectionCount()];
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new ChunkSection(dimension.minSectionY() + i);
        }
    }

    public ChunkPos pos() {
        return pos;
    }

    public DimensionSettings dimension() {
        return dimension;
    }

    public ChunkSection sectionByIndex(int index) {
        return sections[index];
    }

    public ChunkSection sectionForY(int y) {
        return sections[sectionIndexForY(y)];
    }

    public int sectionIndexForY(int y) {
        if (!dimension.containsY(y)) {
            throw new IndexOutOfBoundsException("Y outside dimension: " + y);
        }
        return Math.floorDiv(y, ChunkSection.SIZE) - dimension.minSectionY();
    }

    public int sectionCount() {
        return sections.length;
    }

    public short blockId(int worldX, int y, int worldZ) {
        ChunkSection section = sectionForY(y);
        return section.blockId(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ));
    }

    public void setBlockId(int worldX, int y, int worldZ, short blockId) {
        ChunkSection section = sectionForY(y);
        section.setBlockId(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ), blockId);
    }

    public int skyLight(int worldX, int y, int worldZ) {
        ChunkSection section = sectionForY(y);
        return section.skyLight(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ));
    }

    public void setSkyLight(int worldX, int y, int worldZ, int light) {
        ChunkSection section = sectionForY(y);
        section.setSkyLight(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ), light);
    }

    public int blockLight(int worldX, int y, int worldZ) {
        ChunkSection section = sectionForY(y);
        return section.blockLight(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ));
    }

    public void setBlockLight(int worldX, int y, int worldZ, int light) {
        ChunkSection section = sectionForY(y);
        section.setBlockLight(ChunkPos.localCoord(worldX), Math.floorMod(y, ChunkSection.SIZE), ChunkPos.localCoord(worldZ), light);
    }

    public void clearBlockLight() {
        for (ChunkSection section : sections) {
            section.clearBlockLight();
        }
    }
}
