package dev.voxelgame.server.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkDataCodec;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.light.LightEngine;

import java.util.Optional;

public final class ServerWorld {
    private final long seed;
    private final InMemoryWorld world;
    private final OverworldGenerator generator;
    private final LightEngine lightEngine = new LightEngine();

    public ServerWorld(long seed) {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        this.seed = seed;
        this.world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        this.generator = new OverworldGenerator(seed);
    }

    public long seed() {
        return seed;
    }

    public DimensionSettings dimension() {
        return world.dimension();
    }

    public Chunk getOrGenerateChunk(ChunkPos pos) {
        return world.findChunk(pos).orElseGet(() -> {
            Chunk chunk = world.getOrCreateChunk(pos);
            generator.generate(chunk);
            lightEngine.rebuildChunkLighting(world, pos);
            return chunk;
        });
    }

    public void setBlock(int x, int y, int z, short blockId) {
        getOrGenerateChunk(ChunkPos.fromBlock(x, z));
        world.setBlockId(x, y, z, blockId);
        lightEngine.rebuildChunkLighting(world, ChunkPos.fromBlock(x, z));
    }

    public Optional<GamePacket.BlockUpdate> applyBlockAction(GamePacket.BlockAction action) {
        return switch (action.action()) {
            case BREAK -> breakBlock(action);
            case PLACE -> placeBlock(action);
        };
    }

    public Optional<String> dropFor(int x, int y, int z) {
        if (!world.dimension().containsY(y)) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(x, z));
        BlockType target = world.blockType(world.blockId(x, y, z));
        if (!target.collidable() || target.dropItemKey() == null) {
            return Optional.empty();
        }
        return Optional.of(target.dropItemKey());
    }

    public Optional<String> blockKey(short blockId) {
        return world.blocks().findById(blockId).map(BlockType::key);
    }

    private Optional<GamePacket.BlockUpdate> breakBlock(GamePacket.BlockAction action) {
        if (!world.dimension().containsY(action.targetY())) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(action.targetX(), action.targetZ()));
        BlockType target = world.blockType(world.blockId(action.targetX(), action.targetY(), action.targetZ()));
        if (!target.collidable()) {
            return Optional.empty();
        }
        setBlock(action.targetX(), action.targetY(), action.targetZ(), Blocks.AIR);
        return Optional.of(new GamePacket.BlockUpdate(action.targetX(), action.targetY(), action.targetZ(), Blocks.AIR));
    }

    private Optional<GamePacket.BlockUpdate> placeBlock(GamePacket.BlockAction action) {
        if (!world.dimension().containsY(action.placeY())) {
            return Optional.empty();
        }
        if (manhattan(action.targetX(), action.targetY(), action.targetZ(), action.placeX(), action.placeY(), action.placeZ()) != 1) {
            return Optional.empty();
        }
        Optional<BlockType> placed = world.blocks().findById(action.blockId());
        if (placed.isEmpty() || action.blockId() == Blocks.AIR || !placed.get().collidable()) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(action.placeX(), action.placeZ()));
        BlockType current = world.blockType(world.blockId(action.placeX(), action.placeY(), action.placeZ()));
        if (current.collidable()) {
            return Optional.empty();
        }
        setBlock(action.placeX(), action.placeY(), action.placeZ(), action.blockId());
        return Optional.of(new GamePacket.BlockUpdate(action.placeX(), action.placeY(), action.placeZ(), action.blockId()));
    }

    private static int manhattan(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    public GamePacket.ChunkData packetFor(ChunkPos pos) {
        return ChunkDataCodec.toPacket(getOrGenerateChunk(pos));
    }
}
