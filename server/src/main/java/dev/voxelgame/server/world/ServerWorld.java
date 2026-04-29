package dev.voxelgame.server.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkDataCodec;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.light.LightEngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ServerWorld {
    public static final int STORAGE_CRATE_SLOTS = 18;

    private final long seed;
    private final InMemoryWorld world;
    private final OverworldGenerator generator;
    private final LightEngine lightEngine = new LightEngine();
    private final Map<BlockPos, Double> activeCampfires = new LinkedHashMap<>();
    private final Map<BlockPos, Inventory> storageCrates = new LinkedHashMap<>();

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

    public synchronized List<GamePacket.BlockUpdate> tickCampfires(double nowSeconds) {
        List<GamePacket.BlockUpdate> updates = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, Double>> iterator = activeCampfires.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Double> entry = iterator.next();
            if (entry.getValue() > nowSeconds) {
                continue;
            }
            BlockPos pos = entry.getKey();
            Optional<BlockType> block = blockAt(pos.x(), pos.y(), pos.z());
            if (block.isPresent() && block.get().id() == Blocks.CAMPFIRE_ACTIVE) {
                setBlock(pos.x(), pos.y(), pos.z(), Blocks.CAMPFIRE_BURNED_OUT);
                updates.add(new GamePacket.BlockUpdate(pos.x(), pos.y(), pos.z(), Blocks.CAMPFIRE_BURNED_OUT));
            }
            iterator.remove();
        }
        return updates;
    }

    public Optional<GamePacket.BlockUpdate> applyBlockAction(GamePacket.BlockAction action) {
        return switch (action.action()) {
            case BREAK -> breakBlock(action);
            case PLACE -> placeBlock(action);
        };
    }

    public Optional<String> dropFor(int x, int y, int z) {
        Optional<BlockType> targetBlock = blockAt(x, y, z);
        if (targetBlock.isEmpty()) {
            return Optional.empty();
        }
        BlockType target = targetBlock.get();
        if (target.id() == Blocks.AIR || target.id() == Blocks.WATER || target.dropItemKey() == null) {
            return Optional.empty();
        }
        return Optional.of(target.dropItemKey());
    }

    public Optional<BlockType> blockAt(int x, int y, int z) {
        if (!world.dimension().containsY(y)) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(x, z));
        return Optional.of(world.blockType(world.blockId(x, y, z)));
    }

    public Optional<String> blockKey(short blockId) {
        return world.blocks().findById(blockId).map(BlockType::key);
    }

    public boolean hasBlockWithin(double centerX, double centerY, double centerZ, short blockId, int radius) {
        int cx = (int) Math.floor(centerX);
        int cy = (int) Math.floor(centerY);
        int cz = (int) Math.floor(centerZ);
        int r = Math.max(0, radius);
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    Optional<BlockType> block = blockAt(x, y, z);
                    if (block.isPresent() && block.get().id() == blockId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public synchronized boolean hasActiveCampfireWithin(double centerX, double centerY, double centerZ, int radius, double nowSeconds) {
        tickCampfires(nowSeconds);
        int cx = (int) Math.floor(centerX);
        int cy = (int) Math.floor(centerY);
        int cz = (int) Math.floor(centerZ);
        int r = Math.max(0, radius);
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    Optional<BlockType> block = blockAt(x, y, z);
                    if (block.isPresent() && CampfireRules.isActiveCampfire(block.get().id())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public synchronized Optional<GamePacket.BlockUpdate> fuelCampfire(int x, int y, int z, double nowSeconds, double addedFuelSeconds) {
        Optional<BlockType> block = blockAt(x, y, z);
        if (block.isEmpty() || !CampfireRules.isCampfire(block.get().id()) || addedFuelSeconds <= 0.0) {
            return Optional.empty();
        }
        BlockPos pos = new BlockPos(x, y, z);
        double activeUntil = Math.max(nowSeconds, activeCampfires.getOrDefault(pos, nowSeconds)) + addedFuelSeconds;
        activeCampfires.put(pos, activeUntil);
        if (block.get().id() != Blocks.CAMPFIRE_ACTIVE) {
            setBlock(x, y, z, Blocks.CAMPFIRE_ACTIVE);
            return Optional.of(new GamePacket.BlockUpdate(x, y, z, Blocks.CAMPFIRE_ACTIVE));
        }
        return Optional.empty();
    }

    public synchronized Optional<List<ItemStack>> openStorageCrate(int x, int y, int z) {
        if (!isStorageCrate(x, y, z)) {
            return Optional.empty();
        }
        Inventory storage = storageCrates.computeIfAbsent(new BlockPos(x, y, z), ignored -> new Inventory(STORAGE_CRATE_SLOTS));
        return Optional.of(storage.slots());
    }

    public synchronized Optional<List<ItemStack>> transferStorageStack(
            int x,
            int y,
            int z,
            Inventory playerInventory,
            Registry<ItemType> items,
            boolean fromStorage,
            int slot
    ) {
        if (!isStorageCrate(x, y, z)) {
            return Optional.empty();
        }
        Inventory storage = storageCrates.computeIfAbsent(new BlockPos(x, y, z), ignored -> new Inventory(STORAGE_CRATE_SLOTS));
        if (fromStorage) {
            transferSlot(storage, slot, playerInventory, items);
        } else {
            transferSlot(playerInventory, slot, storage, items);
        }
        return Optional.of(storage.slots());
    }

    private Optional<GamePacket.BlockUpdate> breakBlock(GamePacket.BlockAction action) {
        if (!world.dimension().containsY(action.targetY())) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(action.targetX(), action.targetZ()));
        BlockType target = world.blockType(world.blockId(action.targetX(), action.targetY(), action.targetZ()));
        if (target.id() == Blocks.AIR || target.id() == Blocks.WATER) {
            return Optional.empty();
        }
        BlockPos pos = new BlockPos(action.targetX(), action.targetY(), action.targetZ());
        activeCampfires.remove(pos);
        storageCrates.remove(pos);
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
        if (placed.isEmpty() || action.blockId() == Blocks.AIR || action.blockId() == Blocks.WATER) {
            return Optional.empty();
        }
        getOrGenerateChunk(ChunkPos.fromBlock(action.placeX(), action.placeZ()));
        BlockType current = world.blockType(world.blockId(action.placeX(), action.placeY(), action.placeZ()));
        if (current.id() != Blocks.AIR && current.id() != Blocks.WATER) {
            return Optional.empty();
        }
        setBlock(action.placeX(), action.placeY(), action.placeZ(), action.blockId());
        return Optional.of(new GamePacket.BlockUpdate(action.placeX(), action.placeY(), action.placeZ(), action.blockId()));
    }

    private boolean isStorageCrate(int x, int y, int z) {
        Optional<BlockType> block = blockAt(x, y, z);
        return block.isPresent() && block.get().id() == Blocks.STORAGE_CRATE;
    }

    private static void transferSlot(Inventory source, int slot, Inventory target, Registry<ItemType> items) {
        if (slot < 0 || slot >= source.size()) {
            return;
        }
        ItemStack stack = source.slot(slot);
        if (stack.isEmpty()) {
            return;
        }
        int remaining = target.addStack(stack, items);
        int moved = stack.count() - remaining;
        if (moved <= 0) {
            return;
        }
        source.setSlot(slot, remaining == 0 ? ItemStack.EMPTY : new ItemStack(stack.itemId(), remaining, stack.damage()));
    }

    private static int manhattan(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    public GamePacket.ChunkData packetFor(ChunkPos pos) {
        return ChunkDataCodec.toPacket(getOrGenerateChunk(pos));
    }

    private record BlockPos(int x, int y, int z) {
    }
}
