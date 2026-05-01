package dev.voxelgame.client.hud;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.ComfortRules;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ClientComfortSources {
    private ClientComfortSources() {
    }

    public static ComfortHudInfo scan(ClientWorld world, Vector3f position, double nowSeconds) {
        return scan(world, position, nowSeconds, List.of());
    }

    public static ComfortHudInfo scan(ClientWorld world, Vector3f position, double nowSeconds, Collection<EntitySnapshot> visibleEntities) {
        if (world == null || position == null) {
            return ComfortHudInfo.none();
        }
        int value = world.comfortAt(position);
        List<ComfortHudInfo.Source> sources = new ArrayList<>(5);
        addActiveCampfire(world, position, nowSeconds, sources);
        addBlockSource(world, position, Blocks.SLEEPING_MAT, "Sleeping Mat", sources);
        addBlockSource(world, position, Blocks.ANCIENT_LANTERN, "Ancient Lantern", sources);
        addBlockSource(world, position, Blocks.LANTERN, "Lantern", sources);
        addBlockSource(world, position, Blocks.WOVEN_RUG, "Rug", sources);
        addBlockSource(world, position, Blocks.WOODEN_CHAIR, "Chair", sources);
        addBlockSource(world, position, Blocks.SMALL_TABLE, "Table", sources);
        addFriendlyAnimalSource(position, visibleEntities, sources);
        return ComfortHudInfo.of(value, sources);
    }

    private static void addActiveCampfire(ClientWorld world, Vector3f position, double nowSeconds, List<ComfortHudInfo.Source> sources) {
        if (world.hasActiveCampfireWithin(position, ComfortRules.SCAN_RADIUS_BLOCKS, nowSeconds)) {
            sources.add(new ComfortHudInfo.Source("Campfire", ComfortRules.comfortValue(Blocks.CAMPFIRE_ACTIVE)));
        }
    }

    private static void addBlockSource(ClientWorld world, Vector3f position, short blockId, String label, List<ComfortHudInfo.Source> sources) {
        if (world.hasBlockWithin(position, blockId, ComfortRules.SCAN_RADIUS_BLOCKS)) {
            sources.add(new ComfortHudInfo.Source(label, ComfortRules.comfortValue(blockId)));
        }
    }

    private static void addFriendlyAnimalSource(Vector3f position, Collection<EntitySnapshot> visibleEntities, List<ComfortHudInfo.Source> sources) {
        if (visibleEntities == null || visibleEntities.isEmpty()) {
            return;
        }
        double maxDistanceSquared = ComfortRules.SCAN_RADIUS_BLOCKS * ComfortRules.SCAN_RADIUS_BLOCKS;
        for (EntitySnapshot entity : visibleEntities) {
            if (entity == null || entity.health() <= 0 || EntitySnapshot.STATE_FLEE.equals(entity.stateKey())) {
                continue;
            }
            if (!isFriendlyComfortAnimal(entity.typeKey())) {
                continue;
            }
            double dx = entity.x() - position.x;
            double dy = entity.y() - position.y;
            double dz = entity.z() - position.z;
            if (dx * dx + dy * dy + dz * dz <= maxDistanceSquared) {
                sources.add(new ComfortHudInfo.Source(friendlyAnimalLabel(entity.typeKey()), 0));
                return;
            }
        }
    }

    private static boolean isFriendlyComfortAnimal(String typeKey) {
        return "voxel:cozy_sheep".equals(typeKey)
                || "voxel:forest_grazer".equals(typeKey)
                || "voxel:meadow_grazer".equals(typeKey);
    }

    private static String friendlyAnimalLabel(String typeKey) {
        if ("voxel:cozy_sheep".equals(typeKey)) {
            return "Friendly Sheep";
        }
        return "Friendly Animal";
    }
}
