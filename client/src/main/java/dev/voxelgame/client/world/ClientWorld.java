package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesh;
import dev.voxelgame.client.render.ChunkMesher;
import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.ComfortRules;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.CollisionShapeCache;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.physics.PhysicsTickets;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkDataCodec;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.ChunkStreamingRings;
import dev.voxelgame.common.world.ChunkTerrainCache;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.light.LightEngine;
import dev.voxelgame.common.world.light.LightRules;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientWorld {
    private static final PlayerBounds PLAYER_BOUNDS = PlayerBounds.DEFAULT;
    private static final double ENTITY_INTERPOLATION_DELAY_SECONDS = 0.10;
    private static final double PROJECTILE_SWEEP_PREVIEW_SECONDS = 0.20;

    private final InMemoryWorld world;
    private final CollisionShapeCache collisionShapeCache;
    private final OverworldGenerator generator;
    private final OverworldGenerator.SpawnPoint spawnPoint;
    private final LightEngine lightEngine = new LightEngine();
    private final long seed;
    private final ChunkBuildQueue buildQueue = new ChunkBuildQueue();
    private final Set<ChunkPos> modifiedChunks = new HashSet<>();
    private final Map<Long, EntityTrack> entities = new HashMap<>();
    private final Map<BlockPos, Double> activeCampfires = new HashMap<>();
    private final Map<BlockPos, CampfireStatusTrack> campfireStatuses = new HashMap<>();
    private UUID ownPlayerId;
    private boolean spawnEntitiesSeeded;
    private int lastUnloadedChunks;
    private long totalUnloadedChunks;
    private boolean lastPhysicsBlockedByLoading;

    public ClientWorld(long seed) {
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
                return world.findChunk(pos).isPresent();
            }

            @Override
            public short blockIdAt(int x, int y, int z) {
                return world.blockId(x, y, z);
            }

            @Override
            public BlockType blockType(short blockId) {
                return world.blockType(blockId);
            }
        });
        this.generator = new OverworldGenerator(seed);
        this.spawnPoint = generator.safeSpawnPoint();
    }

    public DimensionSettings dimension() {
        return world.dimension();
    }

    public synchronized void setOwnPlayerId(UUID ownPlayerId) {
        this.ownPlayerId = ownPlayerId;
    }

    public synchronized void generatePreview(int radius) {
        ensurePreviewAround(new ChunkPos(0, 0), radius);
        seedSpawnEntities();
    }

    public synchronized void ensurePreviewAround(Vector3f position, int radius) {
        ensurePreviewAround(ChunkPos.fromBlock((int) Math.floor(position.x), (int) Math.floor(position.z)), radius);
    }

    public synchronized void ensurePreviewAround(Vector3f position, int radius, int maxNewChunks) {
        ensurePreviewAround(
                ChunkPos.fromBlock((int) Math.floor(position.x), (int) Math.floor(position.z)),
                radius,
                maxNewChunks
        );
    }

    public synchronized void applyChunk(GamePacket.ChunkData data) {
        ChunkDataCodec.applyToWorld(world, data);
        collisionShapeCache.invalidateChunk(data.pos());
        markDirtyWithNeighbors(data.pos());
    }

    public synchronized void applyBlock(GamePacket.BlockUpdate update) {
        short oldBlockId = world.blockId(update.x(), update.y(), update.z());
        BlockType oldBlock = world.blockType(oldBlockId);
        BlockType newBlock = world.blockType(update.blockId());
        world.setBlockId(update.x(), update.y(), update.z(), update.blockId());
        world.findChunk(ChunkPos.fromBlock(update.x(), update.z())).ifPresent(Chunk::invalidateTerrainCache);
        collisionShapeCache.invalidateBlock(update.x(), update.y(), update.z());
        markBoundarySectionsGeometryDirty(update.x(), update.y(), update.z());
        modifiedChunks.add(ChunkPos.fromBlock(update.x(), update.z()));
        BlockPos pos = new BlockPos(update.x(), update.y(), update.z());
        if (!CampfireRules.isActiveCampfire(update.blockId())) {
            activeCampfires.remove(pos);
        }
        if (!CampfireRules.isCampfire(update.blockId())) {
            campfireStatuses.remove(pos);
        }
        long lightingStartNanos = System.nanoTime();
        ChunkPos center = ChunkPos.fromBlock(update.x(), update.z());
        boolean skyRulesChanged = LightRules.skyLightReduction(oldBlock) != LightRules.skyLightReduction(newBlock);
        LightEngine.LightUpdateResult lightUpdate = lightEngine.updateBlockLight(world, update.x(), update.y(), update.z());
        if (lightUpdate.fullRebuildFallback()) {
            lightEngine.rebuildBlockLight(world, center);
        }
        if (skyRulesChanged) {
            lightEngine.rebuildSkyLight(world, center);
        }
        buildQueue.recordLighting((System.nanoTime() - lightingStartNanos) / 1_000_000.0);
        markDirtyWithNeighbors(center);
        for (ChunkPos affected : lightUpdate.affectedChunks()) {
            enqueueDirty(affected, false);
        }
    }

    public synchronized void applyCampfireStatus(GamePacket.CampfireStatus status) {
        applyCampfireStatus(status, monotonicSeconds());
    }

    synchronized void applyCampfireStatus(GamePacket.CampfireStatus status, double nowSeconds) {
        BlockPos pos = new BlockPos(status.x(), status.y(), status.z());
        if (status.active()) {
            activeCampfires.put(pos, nowSeconds + status.fuelSecondsRemaining());
        } else {
            activeCampfires.remove(pos);
        }
        if (world.dimension().containsY(status.y())) {
            short currentBlock = world.blockId(status.x(), status.y(), status.z());
            if (status.active() && CampfireRules.isCampfire(currentBlock) && currentBlock != Blocks.CAMPFIRE_ACTIVE) {
                applyBlock(new GamePacket.BlockUpdate(status.x(), status.y(), status.z(), Blocks.CAMPFIRE_ACTIVE));
            } else if (!status.active() && currentBlock == Blocks.CAMPFIRE_ACTIVE) {
                applyBlock(new GamePacket.BlockUpdate(status.x(), status.y(), status.z(), Blocks.CAMPFIRE_BURNED_OUT));
            }
        }
        campfireStatuses.put(pos, new CampfireStatusTrack(
                status.active(),
                status.fuelSecondsRemaining(),
                status.cookingRecipeKey(),
                status.cookTotalSeconds(),
                status.cookSecondsRemaining(),
                nowSeconds
        ));
    }

    public synchronized void applyEntitySnapshots(Collection<EntitySnapshot> snapshots) {
        applyEntitySnapshots(snapshots, monotonicSeconds());
    }

    synchronized void applyEntitySnapshots(Collection<EntitySnapshot> snapshots, double nowSeconds) {
        Set<Long> seen = new HashSet<>();
        for (EntitySnapshot snapshot : snapshots) {
            seen.add(snapshot.entityId());
            EntityTrack existing = entities.get(snapshot.entityId());
            entities.put(snapshot.entityId(), existing == null
                    ? EntityTrack.single(snapshot, nowSeconds)
                    : existing.update(snapshot, nowSeconds));
        }
        entities.keySet().removeIf(entityId -> !seen.contains(entityId));
    }

    public synchronized List<EntitySnapshot> visibleEntities() {
        return visibleEntities(monotonicSeconds());
    }

    public synchronized List<EntitySnapshot> visibleEntities(double nowSeconds) {
        List<EntitySnapshot> visible = new ArrayList<>();
        for (EntityTrack track : entities.values()) {
            EntitySnapshot snapshot = track.sample(nowSeconds);
            if (isOwnPlayerSnapshot(snapshot)) {
                continue;
            }
            visible.add(snapshot);
        }
        return visible;
    }

    public synchronized Optional<Raycast.Hit> pick(Vector3f position, Vector3f direction, double maxDistance) {
        return Raycast.firstSolid(
                world,
                new Vector3d(position.x, position.y, position.z),
                new Vector3d(direction.x, direction.y, direction.z),
                maxDistance
        );
    }

    public synchronized boolean breakBlock(Raycast.Hit hit) {
        if (!world.dimension().containsY(hit.y())) {
            return false;
        }
        BlockType target = world.blockType(world.blockId(hit.x(), hit.y(), hit.z()));
        if (target.id() == Blocks.AIR || target.id() == Blocks.WATER) {
            return false;
        }
        applyBlock(new GamePacket.BlockUpdate(hit.x(), hit.y(), hit.z(), Blocks.AIR));
        markDirtyWithNeighborsUrgent(ChunkPos.fromBlock(hit.x(), hit.z()));
        return true;
    }

    public synchronized Optional<BlockType> targetBlock(Raycast.Hit hit) {
        if (!world.dimension().containsY(hit.y())) {
            return Optional.empty();
        }
        BlockType target = world.blockType(world.blockId(hit.x(), hit.y(), hit.z()));
        return target.id() == Blocks.AIR ? Optional.empty() : Optional.of(target);
    }

    public synchronized BlockType blockTypeAt(int x, int y, int z) {
        return world.blockType(world.blockId(x, y, z));
    }

    public synchronized Optional<String> dropFor(Raycast.Hit hit) {
        if (!world.dimension().containsY(hit.y())) {
            return Optional.empty();
        }
        BlockType target = world.blockType(world.blockId(hit.x(), hit.y(), hit.z()));
        return Optional.ofNullable(target.dropItemKey());
    }

    public synchronized boolean placeBlock(Raycast.Hit hit, short blockId, Vector3f playerEyePosition) {
        if (!world.dimension().containsY(hit.placeY())) {
            return false;
        }
        Optional<BlockType> placed = world.blocks().findById(blockId);
        if (placed.isEmpty() || blockId == Blocks.AIR || blockId == Blocks.WATER) {
            return false;
        }
        if (InteractionRules.placementIntersectsPlayer(playerEyePosition.x, playerEyePosition.y, playerEyePosition.z, hit.placeX(), hit.placeY(), hit.placeZ(), blockId)) {
            return false;
        }
        if (placementIntersectsVisibleEntity(hit.placeX(), hit.placeY(), hit.placeZ(), blockId)) {
            return false;
        }
        BlockType current = world.blockType(world.blockId(hit.placeX(), hit.placeY(), hit.placeZ()));
        if (current.id() != Blocks.AIR && current.id() != Blocks.WATER) {
            return false;
        }
        applyBlock(new GamePacket.BlockUpdate(hit.placeX(), hit.placeY(), hit.placeZ(), blockId));
        markDirtyWithNeighborsUrgent(ChunkPos.fromBlock(hit.placeX(), hit.placeZ()));
        return true;
    }

    public synchronized boolean placementIntersectsVisibleEntity(int blockX, int blockY, int blockZ) {
        return placementIntersectsVisibleEntity(blockX, blockY, blockZ, monotonicSeconds());
    }

    public synchronized boolean placementIntersectsVisibleEntity(int blockX, int blockY, int blockZ, double nowSeconds) {
        return placementIntersectsVisibleEntity(blockX, blockY, blockZ, Blocks.STONE, nowSeconds);
    }

    public synchronized boolean placementIntersectsVisibleEntity(int blockX, int blockY, int blockZ, short blockId) {
        return placementIntersectsVisibleEntity(blockX, blockY, blockZ, blockId, monotonicSeconds());
    }

    public synchronized boolean placementIntersectsVisibleEntity(int blockX, int blockY, int blockZ, short blockId, double nowSeconds) {
        for (EntityTrack track : entities.values()) {
            EntitySnapshot snapshot = track.sample(nowSeconds);
            if (ownPlayerId != null && ownPlayerId.equals(snapshot.ownerPlayerId())) {
                continue;
            }
            if (InteractionRules.placementIntersectsEntity(snapshot, blockX, blockY, blockZ, blockId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean collidesPlayer(double eyeX, double eyeY, double eyeZ) {
        lastPhysicsBlockedByLoading = false;
        CollisionShapeCache.CollisionCheck collision = collisionShapeCache.collidesPlayer(eyeX, eyeY, eyeZ, PLAYER_BOUNDS);
        lastPhysicsBlockedByLoading = collision.blockedByMissingChunk();
        return collision.collides();
    }

    public synchronized boolean lastPhysicsBlockedByLoading() {
        return lastPhysicsBlockedByLoading;
    }

    public synchronized PhysicsLoadingStatus physicsLoadingStatus(Vector3f position) {
        return physicsLoadingStatus(position, PhysicsTickets.CLIENT_LOADING_BARRIER_RADIUS_CHUNKS);
    }

    public synchronized PhysicsLoadingStatus physicsLoadingStatus(Vector3f position, int radiusChunks) {
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(position.x), (int) Math.floor(position.z));
        int radius = Math.max(0, radiusChunks);
        int loaded = 0;
        int required = PhysicsTickets.requiredChunkCount(radius);
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                if (world.findChunk(new ChunkPos(x, z)).isPresent()) {
                    loaded++;
                }
            }
        }
        return new PhysicsLoadingStatus(center, radius, loaded, required, loaded < required || lastPhysicsBlockedByLoading);
    }

    public synchronized List<ChunkMesh.Bounds> collisionShapeBoundsAround(Vector3f position, int radiusBlocks) {
        int centerX = floor(position.x);
        int centerY = floor(position.y);
        int centerZ = floor(position.z);
        List<ChunkMesh.Bounds> bounds = new ArrayList<>();
        for (CollisionShapeCache.ShapeBounds box : collisionShapeCache.partialMovementShapeBoundsAround(centerX, centerY, centerZ, radiusBlocks)) {
            bounds.add(new ChunkMesh.Bounds(
                    (float) box.minX(),
                    (float) box.minY(),
                    (float) box.minZ(),
                    (float) box.maxX(),
                    (float) box.maxY(),
                    (float) box.maxZ()
            ));
        }
        return bounds;
    }

    public synchronized List<ChunkMesh.Bounds> projectileSweepBoundsAround(Vector3f position, int radiusBlocks, double nowSeconds) {
        double maxDistanceSquared = Math.max(0, radiusBlocks) * Math.max(0, radiusBlocks);
        List<ChunkMesh.Bounds> bounds = new ArrayList<>();
        for (EntitySnapshot snapshot : visibleEntities(nowSeconds)) {
            if (!EntitySnapshot.STATE_PROJECTILE.equals(snapshot.stateKey())) {
                continue;
            }
            double dx = snapshot.x() - position.x;
            double dy = snapshot.y() - position.y;
            double dz = snapshot.z() - position.z;
            if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) {
                continue;
            }
            double nextX = snapshot.x() + snapshot.velocityX() * PROJECTILE_SWEEP_PREVIEW_SECONDS;
            double nextY = snapshot.y() + snapshot.velocityY() * PROJECTILE_SWEEP_PREVIEW_SECONDS;
            double nextZ = snapshot.z() + snapshot.velocityZ() * PROJECTILE_SWEEP_PREVIEW_SECONDS;
            float radius = 0.11f;
            bounds.add(new ChunkMesh.Bounds(
                    (float) Math.min(snapshot.x(), nextX) - radius,
                    (float) Math.min(snapshot.y(), nextY) - radius,
                    (float) Math.min(snapshot.z(), nextZ) - radius,
                    (float) Math.max(snapshot.x(), nextX) + radius,
                    (float) Math.max(snapshot.y(), nextY) + radius,
                    (float) Math.max(snapshot.z(), nextZ) + radius
            ));
        }
        return bounds;
    }

    public synchronized boolean isUnderwater(Vector3f eyePosition) {
        return playerWaterState(eyePosition).headUnderwater();
    }

    public synchronized PlayerWaterState playerWaterState(Vector3f eyePosition) {
        int x = (int) Math.floor(eyePosition.x);
        int z = (int) Math.floor(eyePosition.z);
        int headY = (int) Math.floor(eyePosition.y);
        int bodyY = (int) Math.floor(eyePosition.y - PLAYER_BOUNDS.eyeHeight() * 0.35f);
        int feetY = (int) Math.floor(PLAYER_BOUNDS.minY(eyePosition.y) + 0.05);
        return new PlayerWaterState(
                world.blockId(x, feetY, z) == Blocks.WATER,
                world.blockId(x, bodyY, z) == Blocks.WATER,
                world.blockId(x, headY, z) == Blocks.WATER
        );
    }

    public synchronized Optional<BlockType> blockBelowPlayer(Vector3f eyePosition) {
        int x = (int) Math.floor(eyePosition.x);
        int y = (int) Math.floor(PLAYER_BOUNDS.minY(eyePosition.y) - 0.08);
        int z = (int) Math.floor(eyePosition.z);
        if (!world.dimension().containsY(y) || world.findChunk(ChunkPos.fromBlock(x, z)).isEmpty()) {
            return Optional.empty();
        }
        short blockId = world.blockId(x, y, z);
        if (blockId == Blocks.AIR || blockId == Blocks.WATER) {
            return Optional.empty();
        }
        return Optional.of(world.blockType(blockId));
    }

    public synchronized int comfortAt(Vector3f position) {
        return ComfortRules.scan(world, position.x, position.y, position.z);
    }

    public synchronized boolean hasBlockWithin(Vector3f center, short blockId, int radius) {
        return nearestBlockWithin(center, blockId, radius).isPresent();
    }

    public synchronized Optional<BlockPos> nearestBlockWithin(Vector3f center, short blockId, int radius) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r = Math.max(0, radius);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    if (world.blockId(x, y, z) == blockId) {
                        double distance = distanceSquared(center, x, y, z);
                        if (distance < nearestDistance) {
                            nearest = new BlockPos(x, y, z);
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }


    public synchronized short blockIdAt(int x, int y, int z) {
        return world.blockId(x, y, z);
    }

    public synchronized int skyLightAt(int x, int y, int z) {
        return world.skyLight(x, y, z);
    }

    public synchronized int blockLightAt(int x, int y, int z) {
        return world.blockLight(x, y, z);
    }

    public synchronized int combinedLightAt(int x, int y, int z) {
        return Math.max(skyLightAt(x, y, z), blockLightAt(x, y, z));
    }

    public synchronized boolean hasActiveCampfireWithin(Vector3f center, int radius, double nowSeconds) {
        return nearestActiveCampfireWithin(center, radius, nowSeconds).isPresent();
    }

    public synchronized Optional<BlockPos> nearestActiveCampfireWithin(Vector3f center, int radius, double nowSeconds) {
        List<BlockPos> campfires = activeCampfiresWithin(center, radius, 1, nowSeconds);
        return campfires.isEmpty() ? Optional.empty() : Optional.of(campfires.get(0));
    }

    public synchronized List<BlockPos> activeCampfiresWithin(Vector3f center, int radius, int maxResults, double nowSeconds) {
        tickCampfires(nowSeconds);
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r = Math.max(0, radius);
        List<BlockPosDistance> found = new ArrayList<>();
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    if (CampfireRules.isActiveCampfire(world.blockId(x, y, z))) {
                        double dx = x + 0.5 - center.x;
                        double dy = y + 0.5 - center.y;
                        double dz = z + 0.5 - center.z;
                        double distance = dx * dx + dy * dy + dz * dz;
                        found.add(new BlockPosDistance(new BlockPos(x, y, z), distance));
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(BlockPosDistance::distance));
        int limit = Math.min(Math.max(0, maxResults), found.size());
        List<BlockPos> positions = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            positions.add(found.get(i).pos());
        }
        return positions;
    }

    public synchronized Optional<CampfireStatusView> campfireStatusAt(int x, int y, int z, double nowSeconds) {
        return campfireStatusAt(new BlockPos(x, y, z), nowSeconds);
    }

    public synchronized Optional<CampfireStatusView> campfireStatusAt(BlockPos pos, double nowSeconds) {
        tickCampfires(nowSeconds);
        CampfireStatusTrack track = campfireStatuses.get(pos);
        if (track == null) {
            return Optional.empty();
        }
        double elapsed = Math.max(0.0, nowSeconds - track.receivedAtSeconds());
        double fuelRemaining = Math.max(0.0, track.fuelSecondsRemaining() - elapsed);
        boolean blockActive = world.dimension().containsY(pos.y())
                && CampfireRules.isActiveCampfire(world.blockId(pos.x(), pos.y(), pos.z()));
        boolean active = (track.active() && fuelRemaining > 0.0) || blockActive;
        double cookRemaining = track.cookingRecipeKey().isBlank()
                ? 0.0
                : Math.max(0.0, track.cookSecondsRemaining() - elapsed);
        double cookProgress = track.cookTotalSeconds() <= 0.0
                ? 0.0
                : 1.0 - cookRemaining / track.cookTotalSeconds();
        return Optional.of(new CampfireStatusView(
                pos,
                active,
                fuelRemaining,
                track.cookingRecipeKey(),
                track.cookTotalSeconds(),
                cookRemaining,
                clamp01(cookProgress)
        ));
    }

    public synchronized AmbientParticleSources ambientParticleSourcesWithin(Vector3f center, int radius, int maxLeaves, int maxSpores) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r = Math.max(0, radius);
        List<BlockPosDistance> leaves = new ArrayList<>();
        List<BlockPosDistance> spores = new ArrayList<>();
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    short blockId = world.blockId(x, y, z);
                    if (blockId == Blocks.SKYROOT_LEAVES || blockId == Blocks.PINE_LEAVES) {
                        leaves.add(new BlockPosDistance(new BlockPos(x, y, z), distanceSquared(center, x, y, z)));
                    } else if (blockId == Blocks.MUSHROOM_CLUSTER
                            || blockId == Blocks.GLOW_CRYSTAL_NODE
                            || blockId == Blocks.GLOW_MUSHROOM
                            || blockId == Blocks.SPORE_BLOSSOM
                            || blockId == Blocks.RED_MUSHROOM) {
                        spores.add(new BlockPosDistance(new BlockPos(x, y, z), distanceSquared(center, x, y, z)));
                    }
                }
            }
        }
        return new AmbientParticleSources(nearest(leaves, maxLeaves), nearest(spores, maxSpores));
    }

    public synchronized boolean fuelCampfire(Raycast.Hit hit, double nowSeconds, double addedFuelSeconds) {
        if (!world.dimension().containsY(hit.y()) || addedFuelSeconds <= 0.0) {
            return false;
        }
        short blockId = world.blockId(hit.x(), hit.y(), hit.z());
        if (!CampfireRules.isCampfire(blockId)) {
            return false;
        }
        BlockPos pos = new BlockPos(hit.x(), hit.y(), hit.z());
        double activeUntil = Math.max(nowSeconds, activeCampfires.getOrDefault(pos, nowSeconds)) + addedFuelSeconds;
        activeCampfires.put(pos, activeUntil);
        if (blockId != Blocks.CAMPFIRE_ACTIVE) {
            applyBlock(new GamePacket.BlockUpdate(hit.x(), hit.y(), hit.z(), Blocks.CAMPFIRE_ACTIVE));
        }
        campfireStatuses.put(pos, new CampfireStatusTrack(true, activeUntil - nowSeconds, "", 0.0, 0.0, nowSeconds));
        return true;
    }

    public synchronized void tickCampfires(double nowSeconds) {
        var iterator = activeCampfires.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Double> entry = iterator.next();
            if (entry.getValue() > nowSeconds) {
                continue;
            }
            BlockPos pos = entry.getKey();
            iterator.remove();
            if (world.dimension().containsY(pos.y()) && world.blockId(pos.x(), pos.y(), pos.z()) == Blocks.CAMPFIRE_ACTIVE) {
                applyBlock(new GamePacket.BlockUpdate(pos.x(), pos.y(), pos.z(), Blocks.CAMPFIRE_BURNED_OUT));
            }
        }
    }

    public synchronized Vector3f spawnPosition() {
        return new Vector3f((float) spawnPoint.eyeX(), (float) spawnPoint.eyeY(), (float) spawnPoint.eyeZ());
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher) {
        return buildDirtySolidMeshes(mesher, true, Integer.MAX_VALUE);
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher, boolean ambientOcclusion) {
        return buildDirtySolidMeshes(mesher, ambientOcclusion, Integer.MAX_VALUE);
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher, boolean ambientOcclusion, int maxBuilds) {
        List<MeshBuild> builds = new ArrayList<>();
        int limit = Math.max(0, maxBuilds);
        while (builds.size() < limit) {
            ChunkBuildQueue.BuildRequest request = buildQueue.poll(null, Integer.MAX_VALUE, Integer.MAX_VALUE);
            if (request == null) {
                break;
            }
            Optional<Chunk> chunk = world.findChunk(request.pos());
            if (chunk.isEmpty()) {
                buildQueue.cancel(request);
                continue;
            }
            long buildStartNanos = System.nanoTime();
            ChunkMesh mesh = mesher.buildVisibleFaceMesh(world, chunk.get(), BlockRenderLayer.SOLID, ambientOcclusion);
            ChunkMesher.MeshBuildStats stats = mesher.lastBuildStats();
            buildQueue.complete(
                    request,
                    (System.nanoTime() - buildStartNanos) / 1_000_000.0,
                    stats.temporaryBufferGrowthBytes(),
                    stats.retainedBufferBytes()
            );
            clearSectionRenderDirtyFlags(chunk.get());
            builds.add(new MeshBuild(request.pos(), mesh));
        }
        return builds;
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater) {
        return buildDirtyLayeredMeshes(mesher, ambientOcclusion, transparentWater, Integer.MAX_VALUE);
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater, int maxBuilds) {
        return buildDirtyLayeredMeshes(mesher, ambientOcclusion, transparentWater, maxBuilds, null);
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater, int maxBuilds, Vector3f priorityPosition) {
        return buildDirtyLayeredMeshes(
                mesher,
                ambientOcclusion,
                transparentWater,
                maxBuilds,
                priorityPosition,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Double.POSITIVE_INFINITY
        );
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(
            ChunkMesher mesher,
            boolean ambientOcclusion,
            boolean transparentWater,
            int maxBuilds,
            Vector3f priorityPosition,
            int renderDistanceChunks,
            int previewRadiusChunks,
            double maxBuildMilliseconds
    ) {
        List<LayeredMeshBuild> builds = new ArrayList<>();
        int limit = Math.max(0, maxBuilds);
        long budgetNanos = Double.isFinite(maxBuildMilliseconds) && maxBuildMilliseconds > 0.0
                ? (long) (maxBuildMilliseconds * 1_000_000.0)
                : Long.MAX_VALUE;
        long frameBuildStartNanos = System.nanoTime();
        while (builds.size() < limit) {
            if (!builds.isEmpty() && System.nanoTime() - frameBuildStartNanos >= budgetNanos) {
                break;
            }
            ChunkBuildQueue.BuildRequest request = buildQueue.poll(priorityPosition, renderDistanceChunks, previewRadiusChunks);
            if (request == null) {
                break;
            }
            Optional<Chunk> chunk = world.findChunk(request.pos());
            if (chunk.isEmpty()) {
                buildQueue.cancel(request);
                continue;
            }
            long buildStartNanos = System.nanoTime();
            ChunkMesh opaque = mesher.buildVisibleFaceMesh(world, chunk.get(), BlockRenderLayer.SOLID, ambientOcclusion);
            ChunkMesher.MeshBuildStats opaqueStats = mesher.lastBuildStats();
            ChunkMesh cutout = mesher.buildVisibleFaceMesh(world, chunk.get(), BlockRenderLayer.CUTOUT, ambientOcclusion);
            ChunkMesher.MeshBuildStats cutoutStats = mesher.lastBuildStats();
            ChunkMesh transparent = transparentWater
                    ? mesher.buildVisibleFaceMesh(world, chunk.get(), BlockRenderLayer.TRANSLUCENT, ambientOcclusion)
                    : new ChunkMesh(new float[0], new int[0]);
            ChunkMesher.MeshBuildStats transparentStats = transparentWater ? mesher.lastBuildStats() : ChunkMesher.MeshBuildStats.empty();
            buildQueue.complete(
                    request,
                    (System.nanoTime() - buildStartNanos) / 1_000_000.0,
                    opaqueStats.temporaryBufferGrowthBytes() + cutoutStats.temporaryBufferGrowthBytes() + transparentStats.temporaryBufferGrowthBytes(),
                    Math.max(opaqueStats.retainedBufferBytes(), Math.max(cutoutStats.retainedBufferBytes(), transparentStats.retainedBufferBytes()))
            );
            clearSectionRenderDirtyFlags(chunk.get());
            builds.add(new LayeredMeshBuild(request.pos(), opaque, cutout, transparent));
        }
        return builds;
    }

    public synchronized int dirtyChunkCount() {
        return buildQueue.size();
    }

    public synchronized ChunkBuildQueue.Snapshot buildQueueStats() {
        return buildQueue.snapshot();
    }

    public synchronized void recordChunkGpuUpload(double milliseconds) {
        buildQueue.recordGpuUpload(milliseconds);
    }

    public synchronized int loadedChunkCount() {
        return world.loadedChunks().size();
    }

    public synchronized int lastUnloadedChunkCount() {
        return lastUnloadedChunks;
    }

    public synchronized long totalUnloadedChunkCount() {
        return totalUnloadedChunks;
    }

    public synchronized List<ChunkPos> unloadOutside(Vector3f position, int retainRadiusChunks) {
        return unloadOutside(position, retainRadiusChunks, Integer.MAX_VALUE);
    }

    public synchronized List<ChunkPos> unloadOutside(Vector3f position, int retainRadiusChunks, int maxUnloads) {
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(position.x), (int) Math.floor(position.z));
        int retainRadius = Math.max(0, retainRadiusChunks);
        int unloadLimit = Math.max(0, maxUnloads);
        if (unloadLimit == 0) {
            lastUnloadedChunks = 0;
            return List.of();
        }
        List<ChunkPos> loadedPositions = world.loadedChunks()
                .stream()
                .map(Chunk::pos)
                .sorted(Comparator
                        .comparingLong((ChunkPos pos) -> ChunkStreamingRings.distanceSquared(center, pos))
                        .reversed()
                        .thenComparingInt(ChunkPos::x)
                        .thenComparingInt(ChunkPos::z))
                .toList();
        List<ChunkPos> unloaded = new ArrayList<>();
        for (ChunkPos pos : loadedPositions) {
            if (ChunkStreamingRings.distance(center, pos) <= retainRadius || modifiedChunks.contains(pos)) {
                continue;
            }
            if (unloaded.size() >= unloadLimit) {
                break;
            }
            world.removeChunk(pos).ifPresent(chunk -> {
                collisionShapeCache.invalidateChunk(chunk.pos());
                unloaded.add(chunk.pos());
            });
        }

        if (!unloaded.isEmpty()) {
            totalUnloadedChunks += unloaded.size();
            buildQueue.cancelIf(pos -> world.findChunk(pos).isEmpty());
            removeRuntimeDataOutsideLoadedChunks();
        }
        lastUnloadedChunks = unloaded.size();
        return unloaded;
    }

    public synchronized void markAllLoadedDirty() {
        for (Chunk chunk : world.loadedChunks()) {
            buildQueue.enqueue(chunk.pos());
        }
    }

    public synchronized SectionStats sectionStats() {
        int totalSections = 0;
        int emptySections = 0;
        int nonEmptySections = 0;
        int chunksWithBounds = 0;
        int dirtyGeometrySections = 0;
        int dirtyLightSections = 0;
        int dirtyFluidSections = 0;
        int dirtyBlockEntitySections = 0;
        for (Chunk chunk : world.loadedChunks()) {
            boolean hasNonEmptySection = false;
            totalSections += chunk.sectionCount();
            for (int i = 0; i < chunk.sectionCount(); i++) {
                ChunkSection section = chunk.sectionByIndex(i);
                if (section.isDirty(ChunkSection.DirtyAspect.GEOMETRY)) {
                    dirtyGeometrySections++;
                }
                if (section.isDirty(ChunkSection.DirtyAspect.LIGHT)) {
                    dirtyLightSections++;
                }
                if (section.isDirty(ChunkSection.DirtyAspect.FLUID)) {
                    dirtyFluidSections++;
                }
                if (section.isDirty(ChunkSection.DirtyAspect.BLOCK_ENTITY)) {
                    dirtyBlockEntitySections++;
                }
                if (section.isEmpty()) {
                    emptySections++;
                } else {
                    nonEmptySections++;
                    hasNonEmptySection = true;
                }
            }
            if (hasNonEmptySection) {
                chunksWithBounds++;
            }
        }
        return new SectionStats(
                totalSections,
                emptySections,
                nonEmptySections,
                chunksWithBounds,
                dirtyGeometrySections,
                dirtyLightSections,
                dirtyFluidSections,
                dirtyBlockEntitySections
        );
    }

    public synchronized Optional<ChunkVerticalBounds> verticalBounds(ChunkPos pos) {
        return world.findChunk(pos).flatMap(this::verticalBounds);
    }

    public synchronized List<ChunkMesh.Bounds> sectionBoundsAround(Vector3f cameraPosition, int radiusChunks) {
        int radius = Math.max(0, radiusChunks);
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(cameraPosition.x), (int) Math.floor(cameraPosition.z));
        List<ChunkMesh.Bounds> bounds = new ArrayList<>();
        for (Chunk chunk : world.loadedChunks()) {
            ChunkPos pos = chunk.pos();
            if (Math.abs(pos.x() - center.x()) > radius || Math.abs(pos.z() - center.z()) > radius) {
                continue;
            }
            addSectionBounds(bounds, chunk);
        }
        return bounds;
    }

    public synchronized List<SectionLayerBounds> sectionLayerBoundsAround(Vector3f cameraPosition, int radiusChunks) {
        int radius = Math.max(0, radiusChunks);
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(cameraPosition.x), (int) Math.floor(cameraPosition.z));
        List<SectionLayerBounds> bounds = new ArrayList<>();
        for (Chunk chunk : world.loadedChunks()) {
            ChunkPos pos = chunk.pos();
            if (Math.abs(pos.x() - center.x()) > radius || Math.abs(pos.z() - center.z()) > radius) {
                continue;
            }
            addSectionLayerBounds(bounds, chunk);
        }
        return bounds;
    }

    private void markDirtyWithNeighbors(ChunkPos pos) {
        markDirtyWithNeighbors(pos, false);
    }

    private void markDirtyWithNeighborsUrgent(ChunkPos pos) {
        markDirtyWithNeighbors(pos, true);
    }

    private void markDirtyWithNeighbors(ChunkPos pos, boolean urgent) {
        ChunkPos east = new ChunkPos(pos.x() + 1, pos.z());
        ChunkPos west = new ChunkPos(pos.x() - 1, pos.z());
        ChunkPos south = new ChunkPos(pos.x(), pos.z() + 1);
        ChunkPos north = new ChunkPos(pos.x(), pos.z() - 1);
        enqueueDirty(pos, urgent);
        enqueueDirty(east, urgent);
        enqueueDirty(west, urgent);
        enqueueDirty(south, urgent);
        enqueueDirty(north, urgent);
    }

    private void markBoundarySectionsGeometryDirty(int x, int y, int z) {
        ChunkPos center = ChunkPos.fromBlock(x, z);
        markSectionDirtyIfLoaded(center, y, ChunkSection.DirtyAspect.GEOMETRY);

        int localX = ChunkPos.localCoord(x);
        int localY = Math.floorMod(y, ChunkSection.SIZE);
        int localZ = ChunkPos.localCoord(z);
        if (localX == 0) {
            markSectionDirtyIfLoaded(new ChunkPos(center.x() - 1, center.z()), y, ChunkSection.DirtyAspect.GEOMETRY);
        } else if (localX == ChunkSection.SIZE - 1) {
            markSectionDirtyIfLoaded(new ChunkPos(center.x() + 1, center.z()), y, ChunkSection.DirtyAspect.GEOMETRY);
        }
        if (localZ == 0) {
            markSectionDirtyIfLoaded(new ChunkPos(center.x(), center.z() - 1), y, ChunkSection.DirtyAspect.GEOMETRY);
        } else if (localZ == ChunkSection.SIZE - 1) {
            markSectionDirtyIfLoaded(new ChunkPos(center.x(), center.z() + 1), y, ChunkSection.DirtyAspect.GEOMETRY);
        }
        if (localY == 0 && world.dimension().containsY(y - 1)) {
            markSectionDirtyIfLoaded(center, y - 1, ChunkSection.DirtyAspect.GEOMETRY);
        } else if (localY == ChunkSection.SIZE - 1 && world.dimension().containsY(y + 1)) {
            markSectionDirtyIfLoaded(center, y + 1, ChunkSection.DirtyAspect.GEOMETRY);
        }
    }

    private void markSectionDirtyIfLoaded(ChunkPos pos, int y, ChunkSection.DirtyAspect aspect) {
        world.findChunk(pos).ifPresent(chunk -> {
            if (chunk.dimension().containsY(y)) {
                chunk.markSectionDirtyForY(y, aspect);
            }
        });
    }

    private void enqueueDirty(ChunkPos pos, boolean urgent) {
        if (urgent) {
            buildQueue.enqueueUrgent(pos);
        } else {
            buildQueue.enqueue(pos);
        }
    }

    private void ensurePreviewAround(ChunkPos center, int radius) {
        ensurePreviewAround(center, radius, Integer.MAX_VALUE);
    }

    private void ensurePreviewAround(ChunkPos center, int radius, int maxNewChunks) {
        List<ChunkPos> generated = new ArrayList<>();
        List<ChunkPos> missing = new ArrayList<>();
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (world.findChunk(pos).isPresent()) {
                    continue;
                }
                missing.add(pos);
            }
        }
        missing.sort(Comparator
                .comparingInt((ChunkPos pos) -> square(pos.x() - center.x()) + square(pos.z() - center.z()))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        int limit = Math.min(missing.size(), Math.max(0, maxNewChunks));
        for (int i = 0; i < limit; i++) {
            ChunkPos pos = missing.get(i);
            Chunk chunk = world.getOrCreateChunk(pos);
            long generationStartNanos = System.nanoTime();
            generator.generate(chunk);
            buildQueue.recordGeneration((System.nanoTime() - generationStartNanos) / 1_000_000.0);
            generated.add(pos);
            spawnAmbientEntities(pos);
        }
        for (ChunkPos pos : generated) {
            long lightingStartNanos = System.nanoTime();
            lightEngine.rebuildChunkLighting(world, pos);
            buildQueue.recordLighting((System.nanoTime() - lightingStartNanos) / 1_000_000.0);
            markDirtyWithNeighbors(pos);
        }
        seedSpawnEntities();
    }

    private void spawnAmbientEntities(ChunkPos pos) {
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnForChunk(seed, generator, pos)) {
            entities.putIfAbsent(snapshot.entityId(), EntityTrack.single(snapshot, 0.0));
        }
    }

    private void seedSpawnEntities() {
        if (spawnEntitiesSeeded) {
            return;
        }
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnAroundSpawn(seed, 1)) {
            entities.putIfAbsent(snapshot.entityId(), EntityTrack.single(snapshot, 0.0));
        }
        spawnEntitiesSeeded = true;
    }

    private static double distanceSquared(Vector3f center, int x, int y, int z) {
        double dx = x + 0.5 - center.x;
        double dy = y + 0.5 - center.y;
        double dz = z + 0.5 - center.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static List<BlockPos> nearest(List<BlockPosDistance> positions, int maxResults) {
        positions.sort(Comparator.comparingDouble(BlockPosDistance::distance));
        int limit = Math.min(Math.max(0, maxResults), positions.size());
        List<BlockPos> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            result.add(positions.get(i).pos());
        }
        return result;
    }

    private static int square(int value) {
        return value * value;
    }

    private void removeRuntimeDataOutsideLoadedChunks() {
        Set<ChunkPos> loadedPositions = world.loadedChunks()
                .stream()
                .map(Chunk::pos)
                .collect(java.util.stream.Collectors.toSet());
        activeCampfires.keySet().removeIf(pos -> !loadedPositions.contains(ChunkPos.fromBlock(pos.x(), pos.z())));
        campfireStatuses.keySet().removeIf(pos -> !loadedPositions.contains(ChunkPos.fromBlock(pos.x(), pos.z())));
        entities.entrySet().removeIf(entry -> {
            EntitySnapshot snapshot = entry.getValue().current();
            if (ownPlayerId != null && ownPlayerId.equals(snapshot.ownerPlayerId())) {
                return false;
            }
            return !loadedPositions.contains(ChunkPos.fromBlock((int) Math.floor(snapshot.x()), (int) Math.floor(snapshot.z())));
        });
    }

    private static void clearSectionRenderDirtyFlags(Chunk chunk) {
        for (int i = 0; i < chunk.sectionCount(); i++) {
            ChunkSection section = chunk.sectionByIndex(i);
            section.clearDirty(ChunkSection.DirtyAspect.GEOMETRY);
            section.clearDirty(ChunkSection.DirtyAspect.LIGHT);
            section.clearDirty(ChunkSection.DirtyAspect.FLUID);
        }
    }

    private Optional<ChunkVerticalBounds> verticalBounds(Chunk chunk) {
        int firstSection = -1;
        int lastSection = -1;
        for (int i = 0; i < chunk.sectionCount(); i++) {
            ChunkSection section = chunk.sectionByIndex(i);
            if (!section.isEmpty()) {
                if (firstSection < 0) {
                    firstSection = i;
                }
                lastSection = i;
            }
        }
        if (firstSection < 0) {
            return Optional.empty();
        }
        int minY = chunk.sectionByIndex(firstSection).sectionY() * ChunkSection.SIZE;
        int maxY = (chunk.sectionByIndex(lastSection).sectionY() + 1) * ChunkSection.SIZE;
        return Optional.of(new ChunkVerticalBounds(
                Math.max(world.dimension().minY(), minY),
                Math.min(world.dimension().maxYExclusive(), maxY)
        ));
    }

    private void addSectionBounds(List<ChunkMesh.Bounds> target, Chunk chunk) {
        float minX = chunk.pos().x() * ChunkPos.SIZE;
        float maxX = minX + ChunkPos.SIZE;
        float minZ = chunk.pos().z() * ChunkPos.SIZE;
        float maxZ = minZ + ChunkPos.SIZE;
        for (int i = 0; i < chunk.sectionCount(); i++) {
            ChunkSection section = chunk.sectionByIndex(i);
            if (section.isEmpty()) {
                continue;
            }
            float minY = Math.max(world.dimension().minY(), section.sectionY() * ChunkSection.SIZE);
            float maxY = Math.min(world.dimension().maxYExclusive(), minY + ChunkSection.SIZE);
            target.add(new ChunkMesh.Bounds(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    private void addSectionLayerBounds(List<SectionLayerBounds> target, Chunk chunk) {
        float minX = chunk.pos().x() * ChunkPos.SIZE;
        float maxX = minX + ChunkPos.SIZE;
        float minZ = chunk.pos().z() * ChunkPos.SIZE;
        float maxZ = minZ + ChunkPos.SIZE;
        for (int i = 0; i < chunk.sectionCount(); i++) {
            ChunkSection section = chunk.sectionByIndex(i);
            if (section.isEmpty()) {
                continue;
            }
            float minY = Math.max(world.dimension().minY(), section.sectionY() * ChunkSection.SIZE);
            float maxY = Math.min(world.dimension().maxYExclusive(), minY + ChunkSection.SIZE);
            ChunkMesh.Bounds bounds = new ChunkMesh.Bounds(minX, minY, minZ, maxX, maxY, maxZ);
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (sectionContainsRenderLayer(section, layer)) {
                    target.add(new SectionLayerBounds(chunk.pos(), section.sectionY(), layer, bounds));
                }
            }
        }
    }

    private boolean sectionContainsRenderLayer(ChunkSection section, BlockRenderLayer layer) {
        for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
            for (int localZ = 0; localZ < ChunkSection.SIZE; localZ++) {
                for (int localX = 0; localX < ChunkSection.SIZE; localX++) {
                    short blockId = section.blockId(localX, localY, localZ);
                    if (blockId != Blocks.AIR && world.blockType(blockId).renderLayer() == layer) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double monotonicSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    private boolean isOwnPlayerSnapshot(EntitySnapshot snapshot) {
        return ownPlayerId != null
                && ownPlayerId.equals(snapshot.ownerPlayerId())
                && "voxel:player".equals(snapshot.typeKey());
    }

    private static EntitySnapshot interpolate(EntitySnapshot previous, EntitySnapshot current, float t, double spanSeconds) {
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                lerp(previous.x(), current.x(), t),
                lerp(previous.y(), current.y(), t),
                lerp(previous.z(), current.z(), t),
                lerpAngleDegrees(previous.yaw(), current.yaw(), t),
                (float) lerp(previous.pitch(), current.pitch(), t),
                current.health(),
                current.stateKey(),
                interpolatedVelocity(previous.velocityX(), current.velocityX(), previous.x(), current.x(), t, spanSeconds),
                interpolatedVelocity(previous.velocityY(), current.velocityY(), previous.y(), current.y(), t, spanSeconds),
                interpolatedVelocity(previous.velocityZ(), current.velocityZ(), previous.z(), current.z(), t, spanSeconds)
        );
    }

    private static double interpolatedVelocity(
            double previousVelocity,
            double currentVelocity,
            double previousPosition,
            double currentPosition,
            float t,
            double spanSeconds
    ) {
        if (Math.abs(previousVelocity) > 0.000001 || Math.abs(currentVelocity) > 0.000001) {
            return lerp(previousVelocity, currentVelocity, t);
        }
        if (spanSeconds <= 0.000001) {
            return currentVelocity;
        }
        return (currentPosition - previousPosition) / spanSeconds;
    }

    private static double lerp(double from, double to, float t) {
        return from + (to - from) * t;
    }

    private static float lerpAngleDegrees(float from, float to, float t) {
        float delta = ((to - from + 540.0f) % 360.0f) - 180.0f;
        float value = from + delta * t;
        return ((value % 360.0f) + 360.0f) % 360.0f;
    }

    private static float clamp01(double value) {
        if (value <= 0.0) {
            return 0.0f;
        }
        if (value >= 1.0) {
            return 1.0f;
        }
        return (float) value;
    }

    public synchronized String biomeKeyAt(int x, int z) {
        return terrainCacheAt(x, z).biomeAtWorld(x, z).key();
    }

    public synchronized int terrainHeightAt(int x, int z) {
        return terrainCacheAt(x, z).heightAtWorld(x, z);
    }

    public synchronized short terrainSurfaceBlockAt(int x, int z) {
        return terrainCacheAt(x, z).surfaceBlockAtWorld(x, z);
    }

    public synchronized boolean terrainHasFluidAt(int x, int z) {
        return terrainCacheAt(x, z).hasFluidAtWorld(x, z);
    }

    public synchronized boolean terrainHasCaveAt(int x, int z) {
        return terrainCacheAt(x, z).hasCaveAtWorld(x, z);
    }

    public synchronized int terrainCacheChunkCount() {
        int count = 0;
        for (Chunk chunk : world.loadedChunks()) {
            if (chunk.terrainCache().isPresent()) {
                count++;
            }
        }
        return count;
    }

    public synchronized long terrainCacheBytes() {
        long bytes = 0L;
        for (Chunk chunk : world.loadedChunks()) {
            bytes += chunk.terrainCache().map(ChunkTerrainCache::estimatedBytes).orElse(0);
        }
        return bytes;
    }

    public synchronized OverworldGenerator.BiomeTransition biomeTransitionAt(int x, int z) {
        return generator.biomeTransitionAt(x, z);
    }

    private ChunkTerrainCache terrainCacheAt(int x, int z) {
        ChunkPos pos = ChunkPos.fromBlock(x, z);
        return world.findChunk(pos)
                .flatMap(Chunk::terrainCache)
                .orElseGet(() -> generator.terrainCacheForChunk(pos));
    }

    public record MeshBuild(ChunkPos pos, ChunkMesh mesh) {
    }

    public record LayeredMeshBuild(ChunkPos pos, ChunkMesh opaqueMesh, ChunkMesh cutoutMesh, ChunkMesh transparentMesh) {
    }

    public record ChunkVerticalBounds(int minY, int maxYExclusive) {
    }

    public record SectionStats(
            int totalSections,
            int emptySections,
            int nonEmptySections,
            int chunksWithSectionBounds,
            int dirtyGeometrySections,
            int dirtyLightSections,
            int dirtyFluidSections,
            int dirtyBlockEntitySections
    ) {
    }

    public record SectionLayerBounds(ChunkPos pos, int sectionY, BlockRenderLayer layer, ChunkMesh.Bounds bounds) {
    }

    public record BlockPos(int x, int y, int z) {
    }

    public record CampfireStatusView(
            BlockPos pos,
            boolean active,
            double fuelSecondsRemaining,
            String cookingRecipeKey,
            double cookTotalSeconds,
            double cookSecondsRemaining,
            float cookProgress
    ) {
        public boolean cooking() {
            return !cookingRecipeKey.isBlank();
        }
    }

    public record AmbientParticleSources(List<BlockPos> leafSources, List<BlockPos> sporeSources) {
        public AmbientParticleSources {
            leafSources = List.copyOf(leafSources);
            sporeSources = List.copyOf(sporeSources);
        }
    }

    private record BlockPosDistance(BlockPos pos, double distance) {
    }

    private record CampfireStatusTrack(
            boolean active,
            double fuelSecondsRemaining,
            String cookingRecipeKey,
            double cookTotalSeconds,
            double cookSecondsRemaining,
            double receivedAtSeconds
    ) {
    }

    public record PhysicsLoadingStatus(
            ChunkPos center,
            int radiusChunks,
            int loadedChunks,
            int requiredChunks,
            boolean blocked
    ) {
        public PhysicsLoadingStatus {
            requiredChunks = Math.max(0, requiredChunks);
            loadedChunks = Math.max(0, Math.min(loadedChunks, requiredChunks));
            radiusChunks = Math.max(0, radiusChunks);
        }
    }

    private record EntityTrack(EntitySnapshot previous, EntitySnapshot current, double previousTime, double currentTime) {
        static EntityTrack single(EntitySnapshot snapshot, double nowSeconds) {
            return new EntityTrack(snapshot, snapshot, nowSeconds, nowSeconds);
        }

        EntityTrack update(EntitySnapshot snapshot, double nowSeconds) {
            double safeTime = Math.max(nowSeconds, currentTime);
            return new EntityTrack(current, snapshot, currentTime, safeTime);
        }

        EntitySnapshot sample(double nowSeconds) {
            double span = currentTime - previousTime;
            if (span <= 0.000001) {
                return current;
            }
            double sampleTime = nowSeconds - ENTITY_INTERPOLATION_DELAY_SECONDS;
            float t = clamp01((sampleTime - previousTime) / span);
            return interpolate(previous, current, t, span);
        }
    }
}
