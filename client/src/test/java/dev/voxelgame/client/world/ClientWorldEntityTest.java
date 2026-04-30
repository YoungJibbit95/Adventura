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

    @Test
    void interpolatesEntityPositionBetweenServerSnapshots() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 350.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 10.0, 82.0, -4.0, 10.0f, 0.0f, 8, EntitySnapshot.STATE_FOLLOW)
        ), 0.10);

        EntitySnapshot interpolated = world.visibleEntities(0.15).getFirst();

        assertEquals(5.0, interpolated.x(), 0.001);
        assertEquals(81.0, interpolated.y(), 0.001);
        assertEquals(-2.0, interpolated.z(), 0.001);
        assertEquals(0.0f, interpolated.yaw(), 0.001f);
        assertEquals(8, interpolated.health());
        assertEquals(EntitySnapshot.STATE_FOLLOW, interpolated.stateKey());
        assertEquals(100.0, interpolated.velocityX(), 0.001);
        assertEquals(20.0, interpolated.velocityY(), 0.001);
        assertEquals(-40.0, interpolated.velocityZ(), 0.001);
    }

    @Test
    void interpolatesExplicitEntityVelocityForAnimationSignals() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
                        .withVelocity(0.2, 0.0, 0.0)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.2, 80.0, 0.0, 0.0f, 0.0f, 10)
                        .withVelocity(1.0, 0.0, 0.4)
        ), 0.10);

        EntitySnapshot interpolated = world.visibleEntities(0.15).getFirst();

        assertEquals(0.6, interpolated.velocityX(), 0.001);
        assertEquals(0.2, interpolated.velocityZ(), 0.001);
    }

    @Test
    void clampsEntityInterpolationToLatestSnapshot() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 10.0, 80.0, 0.0, 90.0f, 0.0f, 10)
        ), 0.10);

        EntitySnapshot latest = world.visibleEntities(0.25).getFirst();

        assertEquals(10.0, latest.x(), 0.001);
        assertEquals(90.0f, latest.yaw(), 0.001f);
    }

    @Test
    void removesEntitiesMissingFromLatestServerSnapshot() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10),
                new EntitySnapshot(2L, "voxel:snail", null, 2.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(2L, "voxel:snail", null, 3.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.10);

        List<EntitySnapshot> visible = world.visibleEntities(0.25);

        assertEquals(1, visible.size());
        assertEquals(2L, visible.getFirst().entityId());
    }
}
