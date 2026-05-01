package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EntityDrops {
    private static final long MOSS_SNAIL_SLIME_SALT = 0x5A1E5BEEL;
    private static final int MOSS_SNAIL_SLIME_CHANCE_PERCENT = 25;

    private EntityDrops() {
    }

    public static List<Drop> dropsFor(EntitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return dropsFor(snapshot.typeKey(), snapshot.entityId());
    }

    public static List<Drop> dropsFor(String typeKey, long entityId) {
        Objects.requireNonNull(typeKey, "typeKey");
        if (CozyLifeProgression.findCreature(typeKey)
                .filter(design -> design.roles().contains(CreatureRole.RESOURCE))
                .isEmpty()) {
            return List.of();
        }
        return switch (typeKey) {
            case "voxel:moss_snail" -> mossSnailDrops(entityId);
            default -> List.of();
        };
    }

    private static List<Drop> mossSnailDrops(long entityId) {
        List<Drop> drops = new ArrayList<>();
        drops.add(new Drop("voxel:moss_clump", 1));
        if (rareRoll(entityId, MOSS_SNAIL_SLIME_SALT, MOSS_SNAIL_SLIME_CHANCE_PERCENT)) {
            drops.add(new Drop("voxel:slime_drop", 1));
        }
        return List.copyOf(drops);
    }

    private static boolean rareRoll(long entityId, long salt, int chancePercent) {
        long roll = Math.floorMod(mix(entityId ^ salt), 100L);
        return roll < chancePercent;
    }

    private static long mix(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        return mixed ^ (mixed >>> 33);
    }

    public record Drop(String itemKey, int count) {
        public Drop {
            Objects.requireNonNull(itemKey, "itemKey");
            if (itemKey.isBlank()) {
                throw new IllegalArgumentException("Drop item key must not be blank");
            }
            if (count < 1) {
                throw new IllegalArgumentException("Drop count must be >= 1");
            }
        }
    }
}
