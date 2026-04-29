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
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkDataCodec;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.light.LightEngine;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientWorld {
    private static final PlayerBounds PLAYER_BOUNDS = PlayerBounds.DEFAULT;
    private static final double ENTITY_INTERPOLATION_DELAY_SECONDS = 0.10;

    private final InMemoryWorld world;
    private final OverworldGenerator generator;
    private final LightEngine lightEngine = new LightEngine();
    private final long seed;
    private final Set<ChunkPos> dirtyChunks = new LinkedHashSet<>();
    private final Map<Long, EntityTrack> entities = new HashMap<>();
    private final Map<BlockPos, Double> activeCampfires = new HashMap<>();
    private UUID ownPlayerId;
    private boolean spawnEntitiesSeeded;

    public ClientWorld(long seed) {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        this.seed = seed;
        this.world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        this.generator = new OverworldGenerator(seed);
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
        markDirtyWithNeighbors(data.pos());
    }

    public synchronized void applyBlock(GamePacket.BlockUpdate update) {
        world.setBlockId(update.x(), update.y(), update.z(), update.blockId());
        if (!CampfireRules.isActiveCampfire(update.blockId())) {
            activeCampfires.remove(new BlockPos(update.x(), update.y(), update.z()));
        }
        lightEngine.rebuildChunkLighting(world, ChunkPos.fromBlock(update.x(), update.z()));
        markDirtyWithNeighbors(ChunkPos.fromBlock(update.x(), update.z()));
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
            if (ownPlayerId != null && ownPlayerId.equals(snapshot.ownerPlayerId())) {
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
        if (PLAYER_BOUNDS.intersectsBlock(playerEyePosition.x, playerEyePosition.y, playerEyePosition.z, hit.placeX(), hit.placeY(), hit.placeZ())) {
            return false;
        }
        Optional<BlockType> placed = world.blocks().findById(blockId);
        if (placed.isEmpty() || blockId == Blocks.AIR || blockId == Blocks.WATER) {
            return false;
        }
        BlockType current = world.blockType(world.blockId(hit.placeX(), hit.placeY(), hit.placeZ()));
        if (current.id() != Blocks.AIR && current.id() != Blocks.WATER) {
            return false;
        }
        applyBlock(new GamePacket.BlockUpdate(hit.placeX(), hit.placeY(), hit.placeZ(), blockId));
        return true;
    }

    public synchronized boolean collidesPlayer(double eyeX, double eyeY, double eyeZ) {
        double minX = PLAYER_BOUNDS.minX(eyeX);
        double maxX = PLAYER_BOUNDS.maxX(eyeX);
        double minY = PLAYER_BOUNDS.minY(eyeY);
        double maxY = PLAYER_BOUNDS.maxY(eyeY);
        double minZ = PLAYER_BOUNDS.minZ(eyeZ);
        double maxZ = PLAYER_BOUNDS.maxZ(eyeZ);

        for (int y = floor(minY); y <= floor(maxY); y++) {
            if (!world.dimension().containsY(y)) {
                return true;
            }
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                for (int x = floor(minX); x <= floor(maxX); x++) {
                    if (world.findChunk(ChunkPos.fromBlock(x, z)).isEmpty()) {
                        return true;
                    }
                    if (world.blockType(world.blockId(x, y, z)).collidable()) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r = Math.max(0, radius);
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    if (world.blockId(x, y, z) == blockId) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                    } else if (blockId == Blocks.MUSHROOM_CLUSTER || blockId == Blocks.GLOW_CRYSTAL_NODE || blockId == Blocks.RED_MUSHROOM) {
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
            if (world.dimension().containsY(pos.y()) && world.blockId(pos.x(), pos.y(), pos.z()) == Blocks.CAMPFIRE_ACTIVE) {
                applyBlock(new GamePacket.BlockUpdate(pos.x(), pos.y(), pos.z(), Blocks.CAMPFIRE_BURNED_OUT));
            }
            iterator.remove();
        }
    }

    public synchronized Vector3f spawnPosition() {
        int x = 8;
        int z = 8;
        int surfaceY = generator.terrainHeight(x, z, generator.biomeAt(x, z));
        int feetY = surfaceY + 1;
        return new Vector3f(x + 0.5f, feetY + PLAYER_BOUNDS.eyeHeight(), z + 0.5f);
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher) {
        return buildDirtySolidMeshes(mesher, true, Integer.MAX_VALUE);
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher, boolean ambientOcclusion) {
        return buildDirtySolidMeshes(mesher, ambientOcclusion, Integer.MAX_VALUE);
    }

    public synchronized List<MeshBuild> buildDirtySolidMeshes(ChunkMesher mesher, boolean ambientOcclusion, int maxBuilds) {
        List<ChunkPos> positions = takeDirtyPositions(maxBuilds);

        List<MeshBuild> builds = new ArrayList<>();
        for (ChunkPos pos : positions) {
            world.findChunk(pos).ifPresent(chunk -> {
                ChunkMesh mesh = mesher.buildVisibleFaceMesh(world, chunk, BlockRenderLayer.SOLID, ambientOcclusion);
                builds.add(new MeshBuild(pos, mesh));
            });
        }
        return builds;
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater) {
        return buildDirtyLayeredMeshes(mesher, ambientOcclusion, transparentWater, Integer.MAX_VALUE);
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater, int maxBuilds) {
        List<ChunkPos> positions = takeDirtyPositions(maxBuilds);
        return buildLayeredMeshes(mesher, ambientOcclusion, transparentWater, positions);
    }

    public synchronized List<LayeredMeshBuild> buildDirtyLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater, int maxBuilds, Vector3f priorityPosition) {
        List<ChunkPos> positions = takeDirtyPositions(maxBuilds, priorityPosition);
        return buildLayeredMeshes(mesher, ambientOcclusion, transparentWater, positions);
    }

    private List<LayeredMeshBuild> buildLayeredMeshes(ChunkMesher mesher, boolean ambientOcclusion, boolean transparentWater, List<ChunkPos> positions) {
        List<LayeredMeshBuild> builds = new ArrayList<>();
        for (ChunkPos pos : positions) {
            world.findChunk(pos).ifPresent(chunk -> {
                ChunkMesh opaque = mesher.buildVisibleFaceMesh(world, chunk, BlockRenderLayer.SOLID, ambientOcclusion);
                ChunkMesh cutout = mesher.buildVisibleFaceMesh(world, chunk, BlockRenderLayer.CUTOUT, ambientOcclusion);
                ChunkMesh transparent = transparentWater
                        ? mesher.buildVisibleFaceMesh(world, chunk, BlockRenderLayer.TRANSLUCENT, ambientOcclusion)
                        : new ChunkMesh(new float[0], new int[0]);
                builds.add(new LayeredMeshBuild(pos, opaque, cutout, transparent));
            });
        }
        return builds;
    }

    public synchronized int dirtyChunkCount() {
        return dirtyChunks.size();
    }

    public synchronized int loadedChunkCount() {
        return world.loadedChunks().size();
    }

    public synchronized void markAllLoadedDirty() {
        for (Chunk chunk : world.loadedChunks()) {
            dirtyChunks.add(chunk.pos());
        }
    }

    private void markDirtyWithNeighbors(ChunkPos pos) {
        dirtyChunks.add(pos);
        dirtyChunks.add(new ChunkPos(pos.x() + 1, pos.z()));
        dirtyChunks.add(new ChunkPos(pos.x() - 1, pos.z()));
        dirtyChunks.add(new ChunkPos(pos.x(), pos.z() + 1));
        dirtyChunks.add(new ChunkPos(pos.x(), pos.z() - 1));
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
            generator.generate(chunk);
            generated.add(pos);
            spawnAmbientEntities(pos);
        }
        for (ChunkPos pos : generated) {
            lightEngine.rebuildChunkLighting(world, pos);
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

    private List<ChunkPos> takeDirtyPositions(int maxBuilds) {
        int limit = Math.max(1, maxBuilds);
        List<ChunkPos> positions = new ArrayList<>(Math.min(limit, dirtyChunks.size()));
        var iterator = dirtyChunks.iterator();
        while (iterator.hasNext() && positions.size() < limit) {
            positions.add(iterator.next());
            iterator.remove();
        }
        return positions;
    }

    private List<ChunkPos> takeDirtyPositions(int maxBuilds, Vector3f priorityPosition) {
        int limit = Math.max(1, maxBuilds);
        if (priorityPosition == null || dirtyChunks.size() <= limit) {
            return takeDirtyPositions(maxBuilds);
        }

        List<ChunkPos> positions = new ArrayList<>(dirtyChunks);
        positions.sort(Comparator
                .comparingDouble((ChunkPos pos) -> distanceSquaredToChunkCenter(pos, priorityPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        List<ChunkPos> selected = new ArrayList<>(positions.subList(0, Math.min(limit, positions.size())));
        dirtyChunks.removeAll(selected);
        return selected;
    }

    private static double distanceSquaredToChunkCenter(ChunkPos pos, Vector3f priorityPosition) {
        double centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double dx = centerX - priorityPosition.x;
        double dz = centerZ - priorityPosition.z;
        return dx * dx + dz * dz;
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

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double monotonicSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    private static EntitySnapshot interpolate(EntitySnapshot previous, EntitySnapshot current, float t) {
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
                current.stateKey()
        );
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
        return generator.biomeAt(x, z).key();
    }

    public record MeshBuild(ChunkPos pos, ChunkMesh mesh) {
    }

    public record LayeredMeshBuild(ChunkPos pos, ChunkMesh opaqueMesh, ChunkMesh cutoutMesh, ChunkMesh transparentMesh) {
    }

    public record BlockPos(int x, int y, int z) {
    }

    public record AmbientParticleSources(List<BlockPos> leafSources, List<BlockPos> sporeSources) {
        public AmbientParticleSources {
            leafSources = List.copyOf(leafSources);
            sporeSources = List.copyOf(sporeSources);
        }
    }

    private record BlockPosDistance(BlockPos pos, double distance) {
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
            return interpolate(previous, current, t);
        }
    }
}
