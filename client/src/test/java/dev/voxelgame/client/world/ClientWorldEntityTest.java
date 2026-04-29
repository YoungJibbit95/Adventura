package dev.voxelgame.client.world;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWorldEntityTest {
    @Test
    void hidesOwnPlayerFromVisibleEntityList() {
        ClientWorld world = new ClientWorld(1L);
        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        world.setOwnPlayerId(own);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:player", own, 0.0, 80.0, 0.0, 0.0f, 0.0f, 20),
                new EntitySnapshot(2L, "voxel:player", other, 4.0, 80.0, 4.0, 0.0f, 0.0f, 20)
        ));

        assertEquals(1, world.visibleEntities().size());
        assertEquals(other, world.visibleEntities().getFirst().ownerPlayerId());
    }
}
