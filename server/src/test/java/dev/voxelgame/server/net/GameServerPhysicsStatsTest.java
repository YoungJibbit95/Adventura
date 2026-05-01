package dev.voxelgame.server.net;

import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.world.ServerWorld;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class GameServerPhysicsStatsTest {
    @Test
    void exportsPhysicsTickMetricsWithoutStartingNetwork() {
        GameServer server = new GameServer(0, new ServerWorld(123L), (username, authToken) -> AuthResult.accepted(UUID.randomUUID()));

        server.tickEntities(1L);

        GameServer.ServerPhysicsStats stats = server.physicsStats();
        assertTrue(stats.activeAmbientEntities() >= 0);
        assertTrue(stats.parkedAmbientEntities() >= 0);
        assertTrue(stats.projectileCount() >= 0);
        assertTrue(stats.ambientTickNanos() >= 0L);
        assertTrue(stats.projectileTickNanos() >= 0L);
        assertTrue(stats.totalTickNanos() >= 0L);
    }
}
