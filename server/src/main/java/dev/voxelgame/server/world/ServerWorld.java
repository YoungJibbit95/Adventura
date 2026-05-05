package dev.voxelgame.server.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.FluidBlocks;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.ComfortRules;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.loot.LootContext;
import dev.voxelgame.common.loot.LootTable;
import dev.voxelgame.common.loot.LootTableRegistry;
import dev.voxelgame.common.loot.LootTables;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.BlockSurfacePhysics;
import dev.voxelgame.common.physics.CollisionShapeCache;
import dev.voxelgame.common.physics.EnvironmentHazardRules;
import dev.voxelgame.common.physics.EntityPhysicsProfile;
import dev.voxelgame.common.physics.FluidPhysics;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerPhysicsConfig;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.physics.PartialShapeImpactResolver;
import dev.voxelgame.common.physics.ProjectileBounds;
import dev.voxelgame.common.physics.ProjectileHit;
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
import java.util.OptionalDouble;
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
    private static final double ENTITY_PATH_MAX_STEP = 0.35;
    private static final double ENTITY_GROUND_PROBE_DISTANCE = 0.08;

    private final long seed;
    private final InMemoryWorld world;
    private final CollisionShapeCache collisionShapeCache;
    private final OverworldGenerator generator;
    private final OverworldGenerator.SpawnPoint spawnPoint;
    private final LightEngine lightEngine = new LightEngine();
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final LootTableRegistry lootTables = LootTables.createDefaultRegistry();
    private final BlockEntityStore blockEntities = new BlockEntityStore();
    private final Map<BlockPos, Short> changedBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, Double> activeCampfires = new LinkedHashMap<>();
    private final Map<BlockPos, Inventory> storageCrates = new LinkedHashMap<>();
    private final Set<BlockPos> consumedGeneratedLootCrates = new HashSet<>();
    private long dayTimeTicks;

    public ServerWorld(long seed) {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        this.seed = seed;
        this.world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        this.collisionShapeCache = new CollisionShapeCache(new CollisionShapeCache.Source() {
            @Override
            public DimensionSettings dimension() {
                return world.dimension();
            }

            @Override
            public boolean ensureChunkAvailable(ChunkPos pos) {
                getOrGenerateChunk(pos);
                return true;
            }

            @Override
            public short blockIdAt(int x, int y, int z) {
                return ServerWorld.this.blockIdAt(x, y, z);
            }

            @Override
            public BlockType blockType(short blockId) {
                return world.blockType(blockId);
            }
        });
        this.generator = new OverworldGenerator(seed);
        this.spawnPoint = generator.safeSpawnPoint();
    }

    public long seed() {
        return seed;
    }

    public synchronized OverworldGenerator.SpawnPoint spawnPoint() {
        return spawnPoint;
    }

    public synchronized String biomeKeyAt(double x, double z) {
        return generator.biomeAt(floor(x), floor(z)).key();
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

    public synchronized Chunk getOrGenerateChunk(ChunkPos pos) {
        return world.findChunk(pos).orElseGet(() -> {
            Chunk chunk = world.getOrCreateChunk(pos);
            generator.generate(chunk);
            lightEngine.rebuildChunkLighting(world, pos);
            return chunk;
        });
    }

    public synchronized void setBlock(int x, int y, int z, short blockId) {
        setBlockInternal(x, y, z, blockId, true);
    }

    private void setBlockInternal(int x, int y, int z, short blockId, boolean recordDiff) {
        if (!world.dimension().containsY(y)) {
            return;
        }
        getOrGenerateChunk(ChunkPos.fromBlock(x, z));
        short previousBlock = world.blockId(x, y, z);
        world.setBlockId(x, y, z, blockId);
        world.findChunk(ChunkPos.fromBlock(x, z)).ifPresent(Chunk::invalidateTerrainCache);
        collisionShapeCache.invalidateBlock(x, y, z);
        BlockPos pos = new BlockPos(x, y, z);
        syncBlockEntityForBlock(pos, blockId);
        if (previousBlock != blockId && recordDiff) {
            changedBlocks.put(pos, blockId);
        }
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

    public synchronized boolean collidesPlayer(double eyeX, double eyeY, double eyeZ, PlayerBounds bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("Player bounds are required");
        }
        return collisionShapeCache.collidesPlayer(eyeX, eyeY, eyeZ, bounds).collides();
    }

    public synchronized boolean playerPathClear(
            double fromEyeX,
            double fromEyeY,
            double fromEyeZ,
            double toEyeX,
            double toEyeY,
            double toEyeZ,
            PlayerBounds bounds,
            double maxStep
    ) {
        if (bounds == null) {
            throw new IllegalArgumentException("Player bounds are required");
        }
        if (!Double.isFinite(fromEyeX) || !Double.isFinite(fromEyeY) || !Double.isFinite(fromEyeZ)
                || !Double.isFinite(toEyeX) || !Double.isFinite(toEyeY) || !Double.isFinite(toEyeZ)
                || !Double.isFinite(maxStep) || maxStep <= 0.0) {
            return false;
        }
        double dx = toEyeX - fromEyeX;
        double dy = toEyeY - fromEyeY;
        double dz = toEyeZ - fromEyeZ;
        double maxDistance = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int steps = Math.max(1, (int) Math.ceil(maxDistance / maxStep));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (collidesPlayer(fromEyeX + dx * t, fromEyeY + dy * t, fromEyeZ + dz * t, bounds)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean canMoveAmbientEntity(EntitySnapshot current, EntitySnapshot candidate) {
        if (current == null || candidate == null || !current.typeKey().equals(candidate.typeKey())) {
            return false;
        }
        if (!entityPlacementClear(candidate)) {
            return false;
        }
        double dx = candidate.x() - current.x();
        double dy = candidate.y() - current.y();
        double dz = candidate.z() - current.z();
        double maxDistance = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int steps = Math.max(1, (int) Math.ceil(maxDistance / ENTITY_PATH_MAX_STEP));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            EntitySnapshot sample = new EntitySnapshot(
                    candidate.entityId(),
                    candidate.typeKey(),
                    candidate.ownerPlayerId(),
                    current.x() + dx * t,
                    current.y() + dy * t,
                    current.z() + dz * t,
                    candidate.yaw(),
                    candidate.pitch(),
                    candidate.health(),
                    candidate.stateKey(),
                    candidate.velocityX(),
                    candidate.velocityY(),
                    candidate.velocityZ()
            );
            if (!entityPlacementClear(sample)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean entityPlacementClear(EntitySnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (collidesEntity(snapshot)) {
            return false;
        }
        EntityPhysicsProfile profile = EntityPhysicsProfile.forType(snapshot.typeKey());
        if (profile.ignoresTerrainSupport()) {
            return true;
        }
        boolean touchesWater = touchesWater(snapshot);
        if (touchesWater && profile.acceptsWaterPlacement()) {
            return true;
        }
        if (touchesWater && profile.blocksWaterPlacement()) {
            return false;
        }

        double baseY = EntityBounds.baseY(snapshot);
        int supportX = floor(snapshot.x());
        int supportY = floor(baseY - ENTITY_GROUND_PROBE_DISTANCE);
        int supportZ = floor(snapshot.z());
        Optional<BlockType> support = blockAt(supportX, supportY, supportZ);
        if (support.isEmpty() || !support.get().collidable()) {
            return false;
        }
        if (blockIdAt(supportX, floor(baseY), supportZ) == Blocks.WATER || support.get().id() == Blocks.WATER) {
            return false;
        }
        return !prefersGrass(snapshot.typeKey())
                || !EntitySnapshot.STATE_GRAZE.equals(snapshot.stateKey())
                || support.get().id() == Blocks.GRASS;
    }

    public synchronized boolean collidesProjectile(double x, double y, double z, ProjectileBounds bounds) {
        if (bounds == null) {
            return true;
        }
        return collisionShapeCache.collidesProjectile(x, y, z, bounds).collides();
    }

    public synchronized Optional<PartialShapeImpactResolver.ImpactResult> projectileImpact(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            ProjectileBounds bounds
    ) {
        if (bounds == null) {
            return Optional.of(PartialShapeImpactResolver.blocking(0, 0, 0, 0.0, 0.0, 0.0, ProjectileHit.BlockFace.NONE, 0.0));
        }
        return collisionShapeCache.projectileImpact(fromX, fromY, fromZ, toX, toY, toZ, bounds);
    }

    public synchronized boolean projectileInWater(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }
        return FluidBlocks.isWater(blockIdAt(floor(x), floor(y), floor(z)));
    }

    public synchronized FluidPhysics.FluidSample fluidSample(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return FluidPhysics.air();
        }
        int blockX = floor(x);
        int blockY = floor(y);
        int blockZ = floor(z);
        short blockId = blockIdAt(blockX, blockY, blockZ);
        FluidBlocks.FluidKind kind = FluidBlocks.kind(blockId);
        if (!kind.fluid()) {
            return FluidPhysics.air();
        }
        double phase = (blockX * 0.37) + (blockZ * 0.61) + (blockY * 0.13) + (seed & 0xFFFFL) * 0.0003;
        if (kind == FluidBlocks.FluidKind.LAVA) {
            double velocityX = Math.sin(phase) * 0.045;
            double velocityZ = Math.cos(phase * 0.73) * 0.045;
            return FluidPhysics.lava(velocityX, -0.025, velocityZ);
        }
        double velocityX = Math.sin(phase) * 0.16;
        double velocityZ = Math.cos(phase * 0.73) * 0.16;
        return FluidPhysics.water(velocityX, 0.025, velocityZ);
    }

    public synchronized EnvironmentHazardRules.Hazard environmentHazardAtPlayer(double eyeX, double eyeY, double eyeZ, PlayerBounds bounds) {
        if (bounds == null || !Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
            return EnvironmentHazardRules.combine(List.of());
        }
        List<EnvironmentHazardRules.Hazard> hazards = new ArrayList<>();
        for (int y = floor(bounds.minY(eyeY)); y <= floor(bounds.maxY(eyeY)); y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = floor(bounds.minZ(eyeZ)); z <= floor(bounds.maxZ(eyeZ)); z++) {
                for (int x = floor(bounds.minX(eyeX)); x <= floor(bounds.maxX(eyeX)); x++) {
                    if (bounds.intersectsBlock(eyeX, eyeY, eyeZ, x, y, z)) {
                        hazards.add(EnvironmentHazardRules.forBlock(blockIdAt(x, y, z)));
                    }
                }
            }
        }
        return EnvironmentHazardRules.combine(hazards);
    }

    private boolean collidesEntity(EntitySnapshot snapshot) {
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        double baseY = EntityBounds.baseY(snapshot);
        return collisionShapeCache.collidesEntity(bounds, snapshot.x(), baseY, snapshot.z()).collides();
    }

    private boolean touchesWater(EntitySnapshot snapshot) {
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        double baseY = EntityBounds.baseY(snapshot);
        for (int y = floor(bounds.minY(baseY)); y <= floor(bounds.maxY(baseY)); y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = floor(bounds.minZ(snapshot.z())); z <= floor(bounds.maxZ(snapshot.z())); z++) {
                for (int x = floor(bounds.minX(snapshot.x())); x <= floor(bounds.maxX(snapshot.x())); x++) {
                    if (bounds.intersectsBlock(snapshot.x(), baseY, snapshot.z(), x, y, z)
                            && FluidBlocks.isWater(blockIdAt(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean prefersGrass(String typeKey) {
        return "voxel:cozy_sheep".equals(typeKey)
                || "voxel:forest_grazer".equals(typeKey)
                || "voxel:meadow_grazer".equals(typeKey);
    }

    public synchronized PlayerWaterState playerWaterState(double eyeX, double eyeY, double eyeZ, PlayerBounds bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("Player bounds are required");
        }
        int x = floor(eyeX);
        int z = floor(eyeZ);
        int headY = floor(eyeY);
        int bodyY = floor(eyeY - bounds.eyeHeight() * 0.35f);
        int feetY = floor(bounds.minY(eyeY) + 0.05);
        return new PlayerWaterState(
                FluidBlocks.isWater(blockIdAt(x, feetY, z)),
                FluidBlocks.isWater(blockIdAt(x, bodyY, z)),
                FluidBlocks.isWater(blockIdAt(x, headY, z))
        );
    }

    public synchronized BlockSurfacePhysics.SurfaceMaterial playerSurface(
            double eyeX,
            double eyeY,
            double eyeZ,
            PlayerPhysicsConfig config
    ) {
        if (config == null) {
            throw new IllegalArgumentException("Player physics config is required");
        }
        return surfaceAt(eyeX, config.bounds().minY(eyeY) - config.groundProbeDistance(), eyeZ);
    }

    public synchronized BlockSurfacePhysics.SurfaceMaterial surfaceAt(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return BlockSurfacePhysics.DEFAULT;
        }
        return BlockSurfacePhysics.forBlock(blockIdAt(floor(x), floor(y), floor(z)));
    }

    private short blockIdAt(int x, int y, int z) {
        if (!world.dimension().containsY(y)) {
            return Blocks.AIR;
        }
        getOrGenerateChunk(ChunkPos.fromBlock(x, z));
        return world.blockId(x, y, z);
    }

    public Optional<String> blockKey(short blockId) {
        return world.blocks().findById(blockId).map(BlockType::key);
    }

    public synchronized List<BlockChange> saveBlockDiffs() {
        return changedBlocks.entrySet().stream()
                .map(entry -> new BlockChange(
                        entry.getKey().x(),
                        entry.getKey().y(),
                        entry.getKey().z(),
                        blockKey(entry.getValue()).orElse("voxel:air")
                ))
                .toList();
    }

    public synchronized void loadBlockDiffs(List<BlockChange> blockChanges) {
        changedBlocks.clear();
        if (blockChanges == null) {
            return;
        }
        for (BlockChange blockChange : blockChanges) {
            short blockId = world.blocks()
                    .findByKey(blockChange.blockKey())
                    .map(BlockType::id)
                    .orElse(Blocks.AIR);
            setBlockInternal(blockChange.x(), blockChange.y(), blockChange.z(), blockId, false);
            changedBlocks.put(new BlockPos(blockChange.x(), blockChange.y(), blockChange.z()), blockId);
        }
    }

    public synchronized Optional<BlockEntityType> blockEntityTypeAt(int x, int y, int z) {
        Optional<BlockType> block = blockAt(x, y, z);
        if (block.isEmpty()) {
            return Optional.empty();
        }
        return syncBlockEntityForBlock(new BlockPos(x, y, z), block.get().id());
    }

    public synchronized boolean hasBlockLineOfSight(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        return InteractionRules.hasBlockLineOfSight(
                eyeX,
                eyeY,
                eyeZ,
                blockX,
                blockY,
                blockZ,
                this::occludesBlockInteraction
        );
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
        blockEntities.ensure(new BlockEntityStore.Position(x, y, z), BlockEntityType.CAMPFIRE);
        double activeUntil = Math.max(nowSeconds, activeCampfires.getOrDefault(pos, nowSeconds)) + addedFuelSeconds;
        activeCampfires.put(pos, activeUntil);
        if (block.get().id() != Blocks.CAMPFIRE_ACTIVE) {
            setBlock(x, y, z, Blocks.CAMPFIRE_ACTIVE);
            return Optional.of(new GamePacket.BlockUpdate(x, y, z, Blocks.CAMPFIRE_ACTIVE));
        }
        return Optional.empty();
    }

    public synchronized OptionalDouble campfireFuelSecondsRemaining(int x, int y, int z, double nowSeconds) {
        tickCampfires(nowSeconds);
        BlockPos pos = new BlockPos(x, y, z);
        Double activeUntil = activeCampfires.get(pos);
        if (activeUntil == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(0.0, activeUntil - nowSeconds));
    }

    public synchronized BlockEntitySnapshot saveBlockEntities(double nowSeconds) {
        tickCampfires(nowSeconds);
        List<StorageCrateState> storageStates = new ArrayList<>(storageCrates.size());
        for (Map.Entry<BlockPos, Inventory> entry : storageCrates.entrySet()) {
            BlockPos pos = entry.getKey();
            storageStates.add(new StorageCrateState(pos.x(), pos.y(), pos.z(), entry.getValue().slots()));
        }
        List<CampfireState> campfireStates = new ArrayList<>(activeCampfires.size());
        for (Map.Entry<BlockPos, Double> entry : activeCampfires.entrySet()) {
            BlockPos pos = entry.getKey();
            double remaining = Math.max(0.0, entry.getValue() - nowSeconds);
            if (remaining > 0.0) {
                campfireStates.add(new CampfireState(pos.x(), pos.y(), pos.z(), remaining));
            }
        }
        List<BlockEntityPos> consumedLootStates = consumedGeneratedLootCrates.stream()
                .map(pos -> new BlockEntityPos(pos.x(), pos.y(), pos.z()))
                .toList();
        return new BlockEntitySnapshot(storageStates, campfireStates, consumedLootStates, blockEntities.snapshot());
    }

    public synchronized void loadBlockEntities(BlockEntitySnapshot snapshot, double nowSeconds) {
        storageCrates.clear();
        activeCampfires.clear();
        consumedGeneratedLootCrates.clear();
        blockEntities.clear();
        if (snapshot == null) {
            return;
        }
        blockEntities.loadSnapshot(snapshot.blockEntities());
        blockEntities.pruneInvalid((position, type) -> blockIdAt(position.x(), position.y(), position.z()) != Blocks.AIR
                && type.supportsBlock(blockIdAt(position.x(), position.y(), position.z())));
        for (BlockEntityPos pos : snapshot.consumedGeneratedLootCrates()) {
            consumedGeneratedLootCrates.add(new BlockPos(pos.x(), pos.y(), pos.z()));
        }
        for (StorageCrateState state : snapshot.storageCrates()) {
            if (!isStorageCrate(state.x(), state.y(), state.z())) {
                continue;
            }
            blockEntities.ensure(new BlockEntityStore.Position(state.x(), state.y(), state.z()), BlockEntityType.STORAGE_CRATE);
            Inventory storage = new Inventory(STORAGE_CRATE_SLOTS);
            storage.replaceSlots(state.slots());
            storageCrates.put(new BlockPos(state.x(), state.y(), state.z()), storage);
        }
        for (CampfireState state : snapshot.campfires()) {
            if (state.fuelSecondsRemaining() <= 0.0) {
                continue;
            }
            Optional<BlockType> block = blockAt(state.x(), state.y(), state.z());
            if (block.isEmpty() || !CampfireRules.isCampfire(block.get().id())) {
                continue;
            }
            blockEntities.ensure(new BlockEntityStore.Position(state.x(), state.y(), state.z()), BlockEntityType.CAMPFIRE);
            activeCampfires.put(new BlockPos(state.x(), state.y(), state.z()), nowSeconds + state.fuelSecondsRemaining());
            if (block.get().id() != Blocks.CAMPFIRE_ACTIVE) {
                setBlock(state.x(), state.y(), state.z(), Blocks.CAMPFIRE_ACTIVE);
            }
        }
    }

    public synchronized Optional<List<ItemStack>> openStorageCrate(int x, int y, int z) {
        if (!isStorageCrate(x, y, z)) {
            return Optional.empty();
        }
        blockEntities.ensure(new BlockEntityStore.Position(x, y, z), BlockEntityType.STORAGE_CRATE);
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
        blockEntities.ensure(new BlockEntityStore.Position(x, y, z), BlockEntityType.STORAGE_CRATE);
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
        blockEntities.remove(new BlockEntityStore.Position(pos.x(), pos.y(), pos.z()));
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
        return blockEntityTypeAt(x, y, z)
                .filter(type -> type == BlockEntityType.STORAGE_CRATE)
                .isPresent();
    }

    private boolean occludesBlockInteraction(int x, int y, int z) {
        Optional<BlockType> block = blockAt(x, y, z);
        return block.isEmpty() || (block.get().solid() && block.get().opaque());
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
        ChunkPos chunkPos = ChunkPos.fromBlock(pos.x(), pos.z());
        Optional<OverworldGenerator.GeneratedStructure> generatedStructure = world.findChunk(chunkPos)
                .flatMap(Chunk::terrainCache)
                .flatMap(generator::structureAtChunk);
        if (generatedStructure.isEmpty()) {
            generatedStructure = generator.structureAtChunk(chunkPos);
        }
        return generatedStructure
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

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private Optional<BlockEntityType> syncBlockEntityForBlock(BlockPos pos, short blockId) {
        return blockEntities.sync(new BlockEntityStore.Position(pos.x(), pos.y(), pos.z()), blockId);
    }

    public synchronized GamePacket.ChunkData packetFor(ChunkPos pos) {
        return ChunkDataCodec.toPacket(getOrGenerateChunk(pos));
    }

    public record BlockEntitySnapshot(
            List<StorageCrateState> storageCrates,
            List<CampfireState> campfires,
            List<BlockEntityPos> consumedGeneratedLootCrates,
            BlockEntityStore.Snapshot blockEntities
    ) {
        public BlockEntitySnapshot(
                List<StorageCrateState> storageCrates,
                List<CampfireState> campfires,
                List<BlockEntityPos> consumedGeneratedLootCrates
        ) {
            this(storageCrates, campfires, consumedGeneratedLootCrates, BlockEntityStore.Snapshot.EMPTY);
        }

        public BlockEntitySnapshot {
            storageCrates = storageCrates == null ? List.of() : List.copyOf(storageCrates);
            campfires = campfires == null ? List.of() : List.copyOf(campfires);
            consumedGeneratedLootCrates = consumedGeneratedLootCrates == null ? List.of() : List.copyOf(consumedGeneratedLootCrates);
            blockEntities = blockEntities == null ? BlockEntityStore.Snapshot.EMPTY : blockEntities;
        }
    }

    public record BlockChange(int x, int y, int z, String blockKey) {
        public BlockChange {
            if (blockKey == null || blockKey.isBlank()) {
                blockKey = "voxel:air";
            }
        }
    }

    public record StorageCrateState(int x, int y, int z, List<ItemStack> slots) {
        public StorageCrateState {
            slots = slots == null ? List.of() : List.copyOf(slots);
            if (slots.size() != STORAGE_CRATE_SLOTS) {
                throw new IllegalArgumentException("Storage crate snapshot must contain " + STORAGE_CRATE_SLOTS + " slots");
            }
        }
    }

    public record CampfireState(int x, int y, int z, double fuelSecondsRemaining) {
        public CampfireState {
            fuelSecondsRemaining = Math.max(0.0, fuelSecondsRemaining);
        }
    }

    public record BlockEntityPos(int x, int y, int z) {
    }

    private record BlockPos(int x, int y, int z) {
    }

    private record LootMarkerContext(String tableKey, LootContext context) {
    }

    private record LootFill(LootTable table, LootContext context) {
    }
}
