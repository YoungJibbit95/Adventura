package dev.voxelgame.common.world.structure;

import java.util.Objects;

public record StructureBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public StructureBounds {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Structure bounds minimums must not exceed maximums");
        }
    }

    public static StructureBounds fromTemplate(StructureTemplate template) {
        Objects.requireNonNull(template, "template");
        if (template.blocks().isEmpty() && template.markers().isEmpty()) {
            throw new IllegalArgumentException("Cannot derive bounds from an empty template: " + template.key());
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPlacement block : template.blocks()) {
            minX = Math.min(minX, block.x());
            minY = Math.min(minY, block.y());
            minZ = Math.min(minZ, block.z());
            maxX = Math.max(maxX, block.x());
            maxY = Math.max(maxY, block.y());
            maxZ = Math.max(maxZ, block.z());
        }
        for (StructureMarker marker : template.markers()) {
            minX = Math.min(minX, marker.x());
            minY = Math.min(minY, marker.y());
            minZ = Math.min(minZ, marker.z());
            maxX = Math.max(maxX, marker.x());
            maxY = Math.max(maxY, marker.y());
            maxZ = Math.max(maxZ, marker.z());
        }
        return new StructureBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(BlockPlacement block) {
        Objects.requireNonNull(block, "block");
        return contains(block.x(), block.y(), block.z());
    }

    public boolean contains(StructureMarker marker) {
        Objects.requireNonNull(marker, "marker");
        return contains(marker.x(), marker.y(), marker.z());
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }
}
