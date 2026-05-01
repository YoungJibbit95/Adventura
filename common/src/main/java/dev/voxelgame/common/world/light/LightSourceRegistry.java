package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LightSourceRegistry {
    private final Map<Short, Source> sourcesByBlockId;

    private LightSourceRegistry(Map<Short, Source> sourcesByBlockId) {
        this.sourcesByBlockId = Map.copyOf(sourcesByBlockId);
    }

    public static LightSourceRegistry fromBlocks(Registry<BlockType> blocks) {
        Objects.requireNonNull(blocks, "blocks");
        Map<Short, Source> sources = new LinkedHashMap<>();
        for (BlockType block : blocks.values()) {
            if (block.lightEmission() < 0 || block.lightEmission() > LightRules.MAX_LIGHT) {
                throw new IllegalStateException("Light value outside 0..15 for " + block.key());
            }
            if (block.emitsLight()) {
                sources.put(block.id(), new Source(block.id(), block.key(), block.lightEmission()));
            }
        }
        return new LightSourceRegistry(sources);
    }

    public Optional<Source> sourceFor(short blockId) {
        return Optional.ofNullable(sourcesByBlockId.get(blockId));
    }

    public int lightValue(short blockId) {
        Source source = sourcesByBlockId.get(blockId);
        return source == null ? 0 : source.lightValue();
    }

    public int sourceCount() {
        return sourcesByBlockId.size();
    }

    public record Source(short blockId, String key, int lightValue) {
        public Source {
            Objects.requireNonNull(key, "key");
            if (lightValue < 1 || lightValue > LightRules.MAX_LIGHT) {
                throw new IllegalArgumentException("Light source value must be within 1..15");
            }
        }
    }
}
