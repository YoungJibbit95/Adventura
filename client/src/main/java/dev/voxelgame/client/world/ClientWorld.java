package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesh;
import dev.voxelgame.client.render.ChunkMesher;
import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientWorld {
    private final InMemoryWorld world;
    private final OverworldGenerator generator;
    private final LightEngine lightEngine = new LightEngine();
    private final long seed;
    private final Set<ChunkPos> dirtyChunks = new LinkedHashSet<>();
    private final Map<Long, EntitySnapshot> entities = new HashMap<>();
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

    public synchronized void applyChunk(GamePacket.ChunkData data) {
        ChunkDataCodec.applyToWorld(world, data);
        markDirtyWithNeighbors(data.pos());
    }

    public synchronized void applyBlock(GamePacket.BlockUpdate update) {
        world.setBlockId(update.x(), update.y(), update.z(), update.blockId());
        lightEngine.rebuildChunkLighting(world, ChunkPos.fromBlock(update.x(), update.z()));
        markDirtyWithNeighbors(ChunkPos.fromBlock(update.x(), update.z()));
    }

    public synchronized void applyEntitySnapshots(Collection<EntitySnapshot> snapshots) {
        entities.clear();
        for (EntitySnapshot snapshot : snapshots) {
            entities.put(snapshot.entityId(), snapshot);
        }
    }

    public synchronized List<EntitySnapshot> visibleEntities() {
        List<EntitySnapshot> visible = new ArrayList<>();
        for (EntitySnapshot snapshot : entities.values()) {
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

    public synchronized Optional<String> dropFor(Raycast.Hit hit) {
        if (!world.dimension().containsY(hit.y())) {
            return Optional.empty();
        }
        BlockType target = world.blockType(world.blockId(hit.x(), hit.y(), hit.z()));
        return Optional.ofNullable(target.dropItemKey());
    }

    public synchronized boolean placeBlock(Raycast.Hit hit, short blockId) {
        if (!world.dimension().containsY(hit.placeY())) {
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
        double halfWidth = 0.30;
        double minX = eyeX - halfWidth;
        double maxX = eyeX + halfWidth;
        double minY = eyeY - 1.62;
        double maxY = eyeY + 0.18;
        double minZ = eyeZ - halfWidth;
        double maxZ = eyeZ + halfWidth;

        for (int y = floor(minY); y <= floor(maxY); y++) {
            if (!world.dimension().containsY(y)) {
                return true;
            }
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                for (int x = floor(minX); x <= floor(maxX); x++) {
                    if (world.blockType(world.blockId(x, y, z)).collidable()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public synchronized boolean isUnderwater(Vector3f eyePosition) {
        return world.blockId((int) Math.floor(eyePosition.x), (int) Math.floor(eyePosition.y), (int) Math.floor(eyePosition.z)) == Blocks.WATER;
    }

    public synchronized Vector3f spawnPosition() {
        int x = 8;
        int z = 8;
        int y = generator.terrainHeight(x, z, generator.biomeAt(x, z)) + 3;
        return new Vector3f(x + 0.5f, y + 1.62f, z + 0.5f);
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
                ChunkMesh mesh = mesher.buildTerrainMesh(world, chunk, ambientOcclusion);
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

        List<LayeredMeshBuild> builds = new ArrayList<>();
        for (ChunkPos pos : positions) {
            world.findChunk(pos).ifPresent(chunk -> {
                ChunkMesh opaque = mesher.buildTerrainMesh(world, chunk, ambientOcclusion);
                ChunkMesh transparent = transparentWater
                        ? mesher.buildVisibleFaceMesh(world, chunk, BlockRenderLayer.TRANSLUCENT)
                        : new ChunkMesh(new float[0], new int[0]);
                builds.add(new LayeredMeshBuild(pos, opaque, transparent));
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
        List<ChunkPos> generated = new ArrayList<>();
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (world.findChunk(pos).isPresent()) {
                    continue;
                }
                Chunk chunk = world.getOrCreateChunk(pos);
                generator.generate(chunk);
                generated.add(pos);
                spawnAmbientEntities(pos);
            }
        }
        for (ChunkPos pos : generated) {
            lightEngine.rebuildChunkLighting(world, pos);
            markDirtyWithNeighbors(pos);
        }
        seedSpawnEntities();
    }

    private void spawnAmbientEntities(ChunkPos pos) {
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnForChunk(seed, generator, pos)) {
            entities.putIfAbsent(snapshot.entityId(), snapshot);
        }
    }

    private void seedSpawnEntities() {
        if (spawnEntitiesSeeded) {
            return;
        }
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnAroundSpawn(seed, 1)) {
            entities.putIfAbsent(snapshot.entityId(), snapshot);
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

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public synchronized String biomeKeyAt(int x, int z) {
        return generator.biomeAt(x, z).key();
    }

    public record MeshBuild(ChunkPos pos, ChunkMesh mesh) {
    }

    public record LayeredMeshBuild(ChunkPos pos, ChunkMesh opaqueMesh, ChunkMesh transparentMesh) {
    }
}
