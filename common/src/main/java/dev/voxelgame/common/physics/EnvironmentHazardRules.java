package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;

import java.util.Collection;
import java.util.Objects;

public final class EnvironmentHazardRules {
    public static final double DAMAGE_COOLDOWN_SECONDS = 1.0;

    private static final Hazard NONE = new Hazard("", 0, false, false);

    private EnvironmentHazardRules() {
    }

    public static Hazard forBlock(short blockId) {
        if (blockId == Blocks.CAMPFIRE_ACTIVE) {
            return new Hazard("voxel:hot_block", 1, true, false);
        }
        if (blockId == Blocks.CACTUS) {
            return new Hazard("voxel:thorn_block", 1, false, false);
        }
        if (blockId == Blocks.ICE || blockId == Blocks.SNOW) {
            return new Hazard("voxel:cold_block", 0, false, true);
        }
        return NONE;
    }

    public static Hazard combine(Collection<Hazard> hazards) {
        if (hazards == null || hazards.isEmpty()) {
            return NONE;
        }
        Hazard best = NONE;
        for (Hazard hazard : hazards) {
            if (hazard == null || hazard.isNone()) {
                continue;
            }
            if (hazard.damagePerPulse() > best.damagePerPulse()) {
                best = hazard;
            } else if (best.isNone() && (hazard.hot() || hazard.cold())) {
                best = hazard;
            }
        }
        return best;
    }

    public record Hazard(String key, int damagePerPulse, boolean hot, boolean cold) {
        public Hazard {
            key = key == null ? "" : key;
            if (damagePerPulse < 0) {
                throw new IllegalArgumentException("Hazard damage must be >= 0");
            }
        }

        public boolean isNone() {
            return key.isBlank() && damagePerPulse == 0 && !hot && !cold;
        }
    }
}
