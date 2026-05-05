package dev.voxelgame.server.net;

import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.ProjectileHit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerGameplayEventInterestTest {
    private static final UUID PLAYER_ID = UUID.fromString("21b6d875-f9b8-46c9-9a8f-8f8333d2821d");
    private static final UUID OTHER_PLAYER_ID = UUID.fromString("f8f95423-5126-4e18-b89e-7c1907e9687b");

    @Test
    void spatialEventsUsePositionInterest() {
        ServerGameplayEventInterest.Viewer nearViewer = viewer(false, true);
        ServerGameplayEventInterest.Viewer farViewer = viewer(false, false);
        GameplayEvent.ProjectileImpact event = new GameplayEvent.ProjectileImpact(
                1L,
                -1_000_000L,
                "voxel:arrow_projectile",
                8.5,
                120.0,
                8.5,
                GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY
        );
        GamePacket.ProjectileImpact packet = new GamePacket.ProjectileImpact(
                -1_000_000L,
                "voxel:arrow_projectile",
                ProjectileHit.Type.BLOCK,
                8.5,
                120.0,
                8.5,
                8,
                120,
                8,
                ProjectileHit.BlockFace.WEST,
                0L,
                true,
                1L
        );

        assertTrue(ServerGameplayEventInterest.isRelevant(event, nearViewer));
        assertFalse(ServerGameplayEventInterest.isRelevant(event, farViewer));
        assertTrue(ServerGameplayEventInterest.isRelevant(packet, nearViewer));
        assertFalse(ServerGameplayEventInterest.isRelevant(packet, farViewer));
    }

    @Test
    void playerScopedEventsOnlyReachMatchingPlayer() {
        ServerGameplayEventInterest.Viewer ownViewer = viewer(false, false);
        ServerGameplayEventInterest.Viewer otherViewer = new ServerGameplayEventInterest.Viewer(
                OTHER_PLAYER_ID,
                entityId -> false,
                (x, z) -> false
        );
        GameplayEvent.RecipeUnlocked event = new GameplayEvent.RecipeUnlocked(1L, PLAYER_ID, "voxel:stone_pickaxe");
        GameplayEvent.StatusEffectChanged status = new GameplayEvent.StatusEffectChanged(2L, PLAYER_ID, "wet", "applied", 1);

        assertTrue(ServerGameplayEventInterest.isRelevant(event, ownViewer));
        assertFalse(ServerGameplayEventInterest.isRelevant(event, otherViewer));
        assertTrue(ServerGameplayEventInterest.isRelevant(status, ownViewer));
        assertFalse(ServerGameplayEventInterest.isRelevant(status, otherViewer));
    }

    @Test
    void entityEventsUseVisibleEntityInterest() {
        ServerGameplayEventInterest.Viewer visibleViewer = viewer(true, false);
        ServerGameplayEventInterest.Viewer hiddenViewer = viewer(false, true);
        GameplayEvent.Damage event = new GameplayEvent.Damage(1L, 42L, 3, "voxel:thorn");

        assertTrue(ServerGameplayEventInterest.isRelevant(event, visibleViewer));
        assertFalse(ServerGameplayEventInterest.isRelevant(event, hiddenViewer));
    }

    @Test
    void globalFeedbackEventsStayVisibleUntilTheyGetPlayerOrPositionFields() {
        ServerGameplayEventInterest.Viewer viewer = viewer(false, false);

        assertTrue(ServerGameplayEventInterest.isRelevant(new GameplayEvent.Pickup(1L, "voxel:twig", 1), viewer));
        assertTrue(ServerGameplayEventInterest.isRelevant(new GameplayEvent.Craft(2L, "voxel:planks", true, "none"), viewer));
    }

    private static ServerGameplayEventInterest.Viewer viewer(boolean visibleEntity, boolean visiblePosition) {
        return new ServerGameplayEventInterest.Viewer(
                PLAYER_ID,
                entityId -> visibleEntity && entityId == 42L,
                (x, z) -> visiblePosition
        );
    }
}
