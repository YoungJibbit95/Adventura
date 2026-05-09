package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.Objects;

public final class CreatureCombatRules {
    public static final String PROTECTED_COZY_CREATURE_REASON = "protected_cozy_creature";

    private CreatureCombatRules() {
    }

    public static Protection protectionFor(EntitySnapshot target) {
        Objects.requireNonNull(target, "target");
        return CozyLifeProgression.findCreature(target.typeKey())
                .map(CreatureCombatRules::protectionFor)
                .orElse(Protection.ALLOW);
    }

    public static boolean blocksPlayerDamage(EntitySnapshot target) {
        return protectionFor(target).blocksPlayerDamage();
    }

    private static Protection protectionFor(CreatureDesign design) {
        if (isAdventureDanger(design) || isExplicitDamageResource(design)) {
            return Protection.ALLOW;
        }
        return Protection.blocked(PROTECTED_COZY_CREATURE_REASON);
    }

    private static boolean isAdventureDanger(CreatureDesign design) {
        return design.disposition() == CreatureDisposition.HOSTILE
                || design.roles().contains(CreatureRole.RARE_DANGER);
    }

    private static boolean isExplicitDamageResource(CreatureDesign design) {
        return design.roles().contains(CreatureRole.RESOURCE)
                && !design.roles().contains(CreatureRole.BASE_COMFORT);
    }

    public record Protection(boolean blocksPlayerDamage, String reasonKey) {
        private static final Protection ALLOW = new Protection(false, "none");

        public Protection {
            reasonKey = reasonKey == null || reasonKey.isBlank() ? "none" : reasonKey;
        }

        private static Protection blocked(String reasonKey) {
            return new Protection(true, reasonKey);
        }
    }
}
