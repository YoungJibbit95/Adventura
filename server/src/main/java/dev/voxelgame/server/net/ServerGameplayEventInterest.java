package dev.voxelgame.server.net;

import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.net.GamePacket;

import java.util.Objects;
import java.util.UUID;
import java.util.function.LongPredicate;

final class ServerGameplayEventInterest {
    private ServerGameplayEventInterest() {
    }

    static boolean isRelevant(GamePacket.ProjectileImpact impact, Viewer viewer) {
        Objects.requireNonNull(impact, "impact");
        Objects.requireNonNull(viewer, "viewer");
        if (impact.hitType() == dev.voxelgame.common.physics.ProjectileHit.Type.ENTITY
                && viewer.visibleEntity().test(impact.entityId())) {
            return true;
        }
        return viewer.visiblePosition().test(impact.x(), impact.z());
    }

    static boolean isRelevant(GameplayEvent event, Viewer viewer) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(viewer, "viewer");
        return switch (event) {
            case GameplayEvent.Damage damage -> viewer.visibleEntity().test(damage.entityId());
            case GameplayEvent.Heal heal -> viewer.visibleEntity().test(heal.entityId());
            case GameplayEvent.ProjectileImpact impact -> impact.targetEntityId() != GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY
                    && viewer.visibleEntity().test(impact.targetEntityId())
                    || viewer.visiblePosition().test(impact.x(), impact.z());
            case GameplayEvent.Sleep sleep -> viewer.isPlayer(sleep.playerId());
            case GameplayEvent.StatusEffectChanged status -> viewer.isPlayer(status.playerId());
            case GameplayEvent.JournalEntryDiscovered journal -> viewer.isPlayer(journal.playerId());
            case GameplayEvent.RecipeUnlocked recipe -> viewer.isPlayer(recipe.playerId());
            case GameplayEvent.StructureDiscovered structure -> viewer.isPlayer(structure.playerId());
            case GameplayEvent.WeatherThunder thunder -> viewer.visiblePosition().test(thunder.x(), thunder.z());
            case GameplayEvent.StatCritical ignored -> true;
            case GameplayEvent.Pickup ignored -> true;
            case GameplayEvent.Craft ignored -> true;
            case GameplayEvent.CookComplete ignored -> true;
        };
    }

    record Viewer(UUID playerId, LongPredicate visibleEntity, PositionInterest visiblePosition) {
        Viewer {
            Objects.requireNonNull(visibleEntity, "visibleEntity");
            Objects.requireNonNull(visiblePosition, "visiblePosition");
        }

        boolean isPlayer(UUID eventPlayerId) {
            return playerId != null && playerId.equals(eventPlayerId);
        }
    }

    @FunctionalInterface
    interface PositionInterest {
        boolean test(double x, double z);
    }
}
