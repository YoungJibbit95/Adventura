package dev.voxelgame.common.world.structure;

import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;

import java.util.List;
import java.util.Objects;

public record StructureTemplate(String key, List<BlockPlacement> blocks, List<StructureMarker> markers) {
    public StructureTemplate {
        Objects.requireNonNull(key, "key");
        blocks = List.copyOf(blocks);
        markers = List.copyOf(markers);
    }

    public StructureTemplate(String key, List<BlockPlacement> blocks) {
        this(key, blocks, List.of());
    }

    public List<StructureMarker> markers(String type) {
        return markers.stream()
                .filter(marker -> marker.type().equals(type))
                .toList();
    }

    public List<StructureMarker> lootMarkers() {
        return markers("loot");
    }

    public void placeIntoChunk(Chunk chunk, int originX, int originY, int originZ) {
        for (BlockPlacement block : blocks) {
            int x = originX + block.x();
            int y = originY + block.y();
            int z = originZ + block.z();
            if (!chunk.dimension().containsY(y)) {
                continue;
            }
            if (!ChunkPos.fromBlock(x, z).equals(chunk.pos())) {
                continue;
            }
            chunk.setBlockId(x, y, z, block.blockId());
        }
    }
}
