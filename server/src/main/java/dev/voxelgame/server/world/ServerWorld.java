package dev.voxelgame.server.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.ComfortRules;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.loot.LootContext;
import dev.voxelgame.common.loot.LootTable;
import dev.voxelgame.common.loot.LootTableRegistry;
import dev.voxelgame.common.loot.LootTables;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkDataCodec;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.light.LightEngine;
import dev.voxelgame.common.world.structure.StructureMarker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ServerWorld {
    public static final int STORAGE_CRATE_SLOTS = 18;
    public static final long DAY_LENGTH_TICKS = 24_000L;
    public static final long MORNING_TICK = 1_000L;
    public static final long NIGHT_START_TICK = 13_000L;
    public static final long NIGHT_END_TICK = 23_000L;
    public static final int MIN_SLEEP_COMFORT = 4;
    public static final int SLEEP_SHELTER_RADIUS = 2;
    public static final int SLEEP_SHELTER_HEIGHT = 4;

    private final long seed;
    private final InMemoryWorld world;
    private final OverworldGenerator generator;
    private final LightEngine lightEngine = new LightEngine();
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final LootTableRegistry lootTables = LootTables.createDefaultRegistry();
    private final Map<BlockPos, Double> activeCampfires = new LinkedHashMap<>();
    private final Map<BlockPos, Inventory> storageCrates = new LinkedHashMap<>();
    private final Set<BlockPos> consumedGeneratedLootCrates = new HashSet<>();
    private long dayTimeTicks;

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

    public synchronized long dayTimeTicks() {
        return dayTimeTicks;
    }

    public synchronized void setDayTimeTicks(long dayTimeTicks) {
        this.dayTimeTicks = Math.max(0L, dayTimeTicks);
    }

    public synchronized void tickTime(long ticks) {
        if (ticks > 0L) {
            dayTimeTicks += ticks;
        }
    }

    public synchronized boolean isNight() {
        long timeOfDay = Math.floorMod(dayTimeTicks, DAY_LENGTH_TICKS);
        return timeOfDay >= NIGHT_START_TICK && timeOfDay < NIGHT_END_TICK;
    }

    public synchronized boolean trySleepAt(int x, int y, int z) {
        if (!canSleepAt(x, y, z)) {
            return false;
        }
        long day = dayTimeTicks / DAY_LENGTH_TICKS;
        dayTimeTicks = (day + 1L) * DAY_LENGTH_TICKS + MORNING_TICK;
        return true;
    }

    public synchronized boolean canSleepAt(int x, int y, int z) {
        return isSleepingMat(x, y, z)
                && isNight()
                && comfortAt(x + 0.5, y + 0.5, z + 0.5) >= MIN_SLEEP_COMFORT
                && hasSleepShelterAt(x, y, z);
    }

    public synchronized boolean hasSleepShelterAt(int x, int y, int z) {
        for (int yy = y + 1; yy <= y + SLEEP_SHELTER_HEIGHT; yy++) {
            if (!world.dimension().containsY(yy)) {
                continue;
            }
            for (int dz = -SLEEP_SHELTER_RADIUS; dz <= SLEEP_SHELTER_RADIUS; dz++) {
                for (int dx = -SLEEP_SHELTER_RADIUS; dx <= SLEEP_SHELTER_RADIUS; dx++) {
                    Optional<BlockType> block = blockAt(x + dx, yy, z + dz);
                    if (block.isPresent() && block.get().solid() && block.get().opaque()) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public synchronized int comfortAt(double centerX, double centerY, double centerZ) {
        return ComfortRules.scan(world, centerX, centerY, centerZ);
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
        Inventory storage = storageCrates.computeIfAbsent(new BlockPos(x, y, z), this::createStorageInventory);
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
        return transferStorageStack(
                x,
                y,
                z,
                playerInventory,
                items,
                fromStorage,
                slot,
                GamePacket.StorageTransfer.AUTO_TARGET_SLOT,
                Integer.MAX_VALUE
        );
    }

    public synchronized Optional<List<ItemStack>> transferStorageStack(
            int x,
            int y,
            int z,
            Inventory playerInventory,
            Registry<ItemType> items,
            boolean fromStorage,
            int sourceSlot,
            int targetSlot,
            int count
    ) {
        if (!isStorageCrate(x, y, z)) {
            return Optional.empty();
        }
        Inventory storage = storageCrates.computeIfAbsent(new BlockPos(x, y, z), this::createStorageInventory);
        if (fromStorage) {
            transferSlot(storage, sourceSlot, playerInventory, targetSlot, count, items);
        } else {
            transferSlot(playerInventory, sourceSlot, storage, targetSlot, count, items);
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
        if (lootMarkerFor(pos).isPresent()) {
            consumedGeneratedLootCrates.add(pos);
        }
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

    private boolean isSleepingMat(int x, int y, int z) {
        Optional<BlockType> block = blockAt(x, y, z);
        return block.isPresent() && block.get().id() == Blocks.SLEEPING_MAT;
    }

    private Inventory createStorageInventory(BlockPos pos) {
        Inventory storage = new Inventory(STORAGE_CRATE_SLOTS);
        if (consumedGeneratedLootCrates.contains(pos)) {
            return storage;
        }
        lootMarkerFor(pos)
                .flatMap(marker -> lootTables.findByKey(marker.tableKey())
                        .map(table -> new LootFill(table, marker.context())))
                .ifPresent(fill -> {
                    for (ItemStack stack : fill.table().roll(items, fill.context())) {
                        storage.addStack(stack, items);
                    }
                });
        return storage;
    }

    private Optional<LootMarkerContext> lootMarkerFor(BlockPos pos) {
        return generator.structureAtChunk(ChunkPos.fromBlock(pos.x(), pos.z()))
                .flatMap(structure -> {
                    for (StructureMarker marker : structure.template().lootMarkers()) {
                        int markerX = structure.originX() + marker.x();
                        int markerY = structure.originY() + marker.y();
                        int markerZ = structure.originZ() + marker.z();
                        if (markerX == pos.x() && markerY == pos.y() && markerZ == pos.z()) {
                            return Optional.of(new LootMarkerContext(
                                    marker.key(),
                                    new LootContext(seed, structure.template().key(), marker.key(), markerX, markerY, markerZ)
                            ));
                        }
                    }
                    return Optional.empty();
                });
    }

    private static void transferSlot(Inventory source, int sourceSlot, Inventory target, int targetSlot, int count, Registry<ItemType> items) {
        if (sourceSlot < 0 || sourceSlot >= source.size() || count <= 0) {
            return;
        }
        if (targetSlot < GamePacket.StorageTransfer.AUTO_TARGET_SLOT || targetSlot >= target.size()) {
            return;
        }
        ItemStack stack = source.slot(sourceSlot);
        if (stack.isEmpty()) {
            return;
        }
        int requested = Math.min(count, stack.count());
        int moved = targetSlot == GamePacket.StorageTransfer.AUTO_TARGET_SLOT
                ? transferToFirstAvailableSlot(target, stack, requested, items)
                : transferToTargetSlot(target, targetSlot, stack, requested, items);
        if (moved <= 0) {
            return;
        }
        int left = stack.count() - moved;
        source.setSlot(sourceSlot, left == 0 ? ItemStack.EMPTY : new ItemStack(stack.itemId(), left, stack.damage()));
    }

    private static int transferToFirstAvailableSlot(Inventory target, ItemStack stack, int requested, Registry<ItemType> items) {
        int remaining = target.addStack(new ItemStack(stack.itemId(), requested, stack.damage()), items);
        return requested - remaining;
    }

    private static int transferToTargetSlot(Inventory target, int targetSlot, ItemStack stack, int requested, Registry<ItemType> items) {
        ItemStack targetStack = target.slot(targetSlot);
        ItemType item = items.requireById(stack.itemId());
        if (targetStack.isEmpty()) {
            int moved = Math.min(requested, item.maxStackSize());
            target.setSlot(targetSlot, new ItemStack(stack.itemId(), moved, stack.damage()));
            return moved;
        }
        if (stack.damage() != 0 || targetStack.itemId() != stack.itemId() || targetStack.damage() != 0) {
            return 0;
        }
        int moved = Math.min(requested, item.maxStackSize() - targetStack.count());
        if (moved <= 0) {
            return 0;
        }
        target.setSlot(targetSlot, new ItemStack(stack.itemId(), targetStack.count() + moved));
        return moved;
    }

    private static int manhattan(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    public GamePacket.ChunkData packetFor(ChunkPos pos) {
        return ChunkDataCodec.toPacket(getOrGenerateChunk(pos));
    }

    private record BlockPos(int x, int y, int z) {
    }

    private record LootMarkerContext(String tableKey, LootContext context) {
    }

    private record LootFill(LootTable table, LootContext context) {
    }
}
