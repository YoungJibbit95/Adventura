package dev.voxelgame.common.gameplay;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayEventTest {
    private static final UUID PLAYER_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Test
    void eventTypesAndDebugKeysAreStableForHudAndReplayConsumers() {
        List<GameplayEvent> events = List.of(
                new GameplayEvent.Damage(1L, 7L, 3, "voxel:fall"),
                new GameplayEvent.Heal(2L, 7L, 2),
                new GameplayEvent.StatCritical(3L, "hunger", 1),
                new GameplayEvent.Pickup(4L, "voxel:twig", 2),
                new GameplayEvent.Craft(5L, "voxel:stone_knife", true, "none"),
                new GameplayEvent.CookComplete(6L, "voxel:cooked_berries", "voxel:cooked_berries", 1),
                new GameplayEvent.ProjectileImpact(7L, 8L, "voxel:arrow_projectile", 1.0, 64.0, 1.0, GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY),
                new GameplayEvent.Sleep(8L, PLAYER_ID, true),
                new GameplayEvent.WeatherThunder(9L, 4.0, 90.0, -2.0),
                new GameplayEvent.StatusEffectChanged(10L, PLAYER_ID, "voxel:wet", "applied", 1),
                new GameplayEvent.JournalEntryDiscovered(11L, PLAYER_ID, "voxel:first_ruin"),
                new GameplayEvent.RecipeUnlocked(12L, PLAYER_ID, "voxel:hearty_stew"),
                new GameplayEvent.StructureDiscovered(13L, PLAYER_ID, "voxel:old_ruins")
        );

        assertEquals(GameplayEventType.DAMAGE, events.get(0).type());
        assertEquals("damage:voxel:fall", events.get(0).debugKey());
        assertEquals(GameplayEventType.PICKUP, events.get(3).type());
        assertEquals("pickup:voxel:twig", events.get(3).debugKey());
        assertEquals(GameplayEventType.PROJECTILE_IMPACT, events.get(6).type());
        assertEquals("projectile_impact:voxel:arrow_projectile", events.get(6).debugKey());
        assertEquals(GameplayEventType.STATUS_EFFECT_CHANGED, events.get(9).type());
        assertEquals("status_effect:applied:voxel:wet", events.get(9).debugKey());
        assertEquals(GameplayEventType.STRUCTURE_DISCOVERED, events.get(12).type());
        GameplayEvent.ProjectileImpact serverProjectile = new GameplayEvent.ProjectileImpact(
                13L,
                -1_000_000L,
                "voxel:arrow_projectile",
                2.0,
                64.0,
                2.0,
                GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY
        );
        assertEquals(-1_000_000L, serverProjectile.projectileId());
    }

    @Test
    void batchIsVersionedBoundedAndImmutable() {
        GameplayEventBatch batch = GameplayEventBatch.of(List.of(new GameplayEvent.Pickup(1L, "voxel:twig", 1)));

        assertEquals(GameplayEventBatch.CURRENT_SCHEMA_VERSION, batch.schemaVersion());
        assertThrows(UnsupportedOperationException.class, () -> batch.events().add(new GameplayEvent.Heal(2L, 1L, 1)));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEventBatch(999, List.of()));
        assertThrows(IllegalArgumentException.class, () -> GameplayEventBatch.of(Collections.nCopies(GameplayEventBatch.MAX_EVENTS + 1, new GameplayEvent.Pickup(1L, "voxel:twig", 1))));
    }

    @Test
    void invalidEventsAreRejectedBeforeNetworking() {
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.Damage(-1L, 7L, 1, "voxel:fall"));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.Damage(1L, -1L, 1, "voxel:fall"));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.Heal(1L, 7L, 0));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.Pickup(1L, " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.ProjectileImpact(1L, 0L, "voxel:arrow_projectile", 0.0, 0.0, 0.0, -1L));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.ProjectileImpact(1L, 2L, "voxel:arrow_projectile", Double.NaN, 0.0, 0.0, -2L));
        assertThrows(NullPointerException.class, () -> new GameplayEvent.Sleep(1L, null, true));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.StatusEffectChanged(1L, PLAYER_ID, " ", "applied", 1));
        assertThrows(IllegalArgumentException.class, () -> new GameplayEvent.StatusEffectChanged(1L, PLAYER_ID, "voxel:wet", "applied", 0));
        assertTrue(new GameplayEvent.Craft(1L, "voxel:stick", false, "").debugKey().startsWith("craft.fail"));
    }
}
