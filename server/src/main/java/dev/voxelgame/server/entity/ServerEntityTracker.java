package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerEntityTracker {
    private final Map<UUID, EntitySnapshot> players = new ConcurrentHashMap<>();
    private final Map<Long, EntitySnapshot> ambientEntities = new ConcurrentHashMap<>();

    public ServerEntityTracker() {
    }

    public ServerEntityTracker(long seed) {
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnAroundSpawn(seed, 5)) {
            ambientEntities.put(snapshot.entityId(), snapshot);
        }
    }

    public EntitySnapshot registerPlayer(UUID playerId) {
        EntitySnapshot snapshot = new EntitySnapshot(
                entityId(playerId),
                "voxel:player",
                playerId,
                8.0,
                118.0,
                8.0,
                0.0f,
                0.0f,
                20
        );
        players.put(playerId, snapshot);
        return snapshot;
    }

    public EntitySnapshot updatePlayer(UUID playerId, double x, double y, double z, float yaw, float pitch) {
        EntitySnapshot snapshot = new EntitySnapshot(entityId(playerId), "voxel:player", playerId, x, y, z, yaw, pitch, 20);
        players.put(playerId, snapshot);
        return snapshot;
    }

    public void removePlayer(UUID playerId) {
        if (playerId != null) {
            players.remove(playerId);
        }
    }

    public List<EntitySnapshot> snapshots() {
        List<EntitySnapshot> snapshots = new ArrayList<>(players.values());
        snapshots.addAll(ambientEntities.values());
        return snapshots;
    }

    public int playerCount() {
        return players.size();
    }

    private static long entityId(UUID playerId) {
        return playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
    }
}
