package dev.voxelgame.common.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ReproducibleWorldSeeds {
    private static final long BASE_SEED = 1337L;
    private static final List<String> BASE_SMOKE_CHECKS = List.of(
            "Spawn and teleport target are safe",
            "No visible terrain seams around the focus chunk",
            "Biome and surface materials match the scenario",
            "Entities or natural details are visible where expected",
            "FPS and debug overlay values look plausible"
    );

    public static final Scenario SPAWN = scenario(
            "spawn",
            "Spawn baseline",
            BASE_SEED,
            8,
            120,
            8,
            "",
            "",
            "Default start area for baseline singleplayer smoke checks.",
            "Hotbar, HUD and default movement work from a fresh start"
    );

    public static final Scenario RIVER = scenario(
            "river",
            "River / lakeside",
            BASE_SEED,
            -968,
            65,
            -1272,
            "voxel:lakeside",
            "",
            "Lakeside biome with river-carved terrain and water-adjacent resources.",
            "Water, shore materials and nearby clay or berries are visible"
    );

    public static final Scenario PINE_FOREST = scenario(
            "pine_forest",
            "Pine forest",
            BASE_SEED,
            -920,
            95,
            -1272,
            "voxel:pine_forest",
            "",
            "Cold forest sample for pine trees, forest entities and denser foliage.",
            "Pine logs or pine leaves are visible near the focus area"
    );

    public static final Scenario MUSHROOM_GROVE = scenario(
            "mushroom_grove",
            "Mushroom grove",
            BASE_SEED,
            -40,
            94,
            -936,
            "voxel:mushroom_grove",
            "",
            "Moist grove sample for mushroom resources, glow details and moss-snail ambience.",
            "Mushroom clusters or red mushrooms are visible nearby"
    );

    public static final Scenario CAVE = scenario(
            "cave",
            "Cave pocket",
            BASE_SEED,
            -56,
            68,
            -504,
            "voxel:pine_forest",
            "",
            "Subsurface air pocket under the terrain for cave-light and collision smoke checks.",
            "Target block is generated air below the terrain surface"
    );

    public static final Scenario VILLAGE_MARKET = scenario(
            "village_market",
            "Village / market",
            BASE_SEED,
            24,
            120,
            24,
            "",
            "voxel:compact_village",
            "Guaranteed compact village chunk used for market, loot and NPC smoke checks.",
            "Roads, houses, market stall and loot markers are present"
    );

    public static final Scenario FROST_PEAKS = scenario(
            "frost_peaks",
            "Frost peaks",
            BASE_SEED,
            760,
            102,
            -952,
            "voxel:frost_peaks",
            "",
            "Cold mountain sample for snow, ice and high-altitude visibility checks.",
            "Snow or ice appears at the focus area"
    );

    public static final Scenario DESERT_DUNES = scenario(
            "desert_dunes",
            "Desert / dunes",
            BASE_SEED,
            1128,
            80,
            -632,
            "voxel:sun_dunes",
            "",
            "Warm dry biome sample for dunes, cactus and sand visibility checks.",
            "Sand terrain dominates and desert details are plausible"
    );

    public static final Scenario OLD_RUINS = scenario(
            "old_ruins",
            "Old ruins",
            BASE_SEED,
            -1144,
            79,
            1016,
            "voxel:old_ruins",
            "",
            "Ruin-biome sample for mossy stone, glow crystals and structure checks.",
            "Ruin materials or old-ruins biome surfaces are visible"
    );

    public static final Scenario HIGHLANDS_ORES = scenario(
            "highlands_ores",
            "Highlands / ores",
            BASE_SEED,
            -3440,
            158,
            1312,
            "voxel:highlands",
            "",
            "High-elevation highlands sample for steep terrain, boulders and ore-progression smoke checks.",
            "Highlands terrain is visible and underground ore layers can be inspected nearby"
    );

    public static final Scenario WATER_HEAVY = scenario(
            "water_heavy",
            "Water-heavy world",
            BASE_SEED,
            -192,
            66,
            -3008,
            "voxel:lakeside",
            "",
            "Low lakeside sample with a high water surface ratio for transparent water and shore meshing checks.",
            "Water renders transparent and shore chunks do not show mesh holes"
    );

    public static final Scenario PERFORMANCE_STRESS = scenario(
            "performance_stress",
            "Performance stress",
            BASE_SEED,
            -1184,
            123,
            3456,
            "voxel:skyroot_forest",
            "",
            "Mixed-biome focus area used for high render distance, chunk-load and debug-stat smoke checks.",
            "Several biomes are visible within a short flight and debug counters remain plausible"
    );

    private static final List<Scenario> ALL = List.of(
            SPAWN,
            RIVER,
            PINE_FOREST,
            MUSHROOM_GROVE,
            CAVE,
            VILLAGE_MARKET,
            FROST_PEAKS,
            DESERT_DUNES,
            OLD_RUINS,
            HIGHLANDS_ORES,
            WATER_HEAVY,
            PERFORMANCE_STRESS
    );

    private ReproducibleWorldSeeds() {
    }

    public static List<Scenario> all() {
        return ALL;
    }

    public static Optional<Scenario> findByKey(String key) {
        return ALL.stream().filter(seed -> seed.key().equals(key)).findFirst();
    }

    private static Scenario scenario(
            String key,
            String label,
            long seed,
            int focusX,
            int focusY,
            int focusZ,
            String expectedBiomeKey,
            String expectedStructureKey,
            String notes,
            String... scenarioSmokeChecks
    ) {
        List<String> smokeChecks = new ArrayList<>(BASE_SMOKE_CHECKS);
        smokeChecks.addAll(List.of(scenarioSmokeChecks));
        return new Scenario(key, label, seed, focusX, focusY, focusZ, expectedBiomeKey, expectedStructureKey, notes, smokeChecks);
    }

    public record Scenario(
            String key,
            String label,
            long seed,
            int focusX,
            int focusY,
            int focusZ,
            String expectedBiomeKey,
            String expectedStructureKey,
            String notes,
            List<String> smokeChecks
    ) {
        public Scenario {
            requireText(key, "key");
            requireText(label, "label");
            requireText(notes, "notes");
            expectedBiomeKey = Objects.requireNonNull(expectedBiomeKey, "expectedBiomeKey");
            expectedStructureKey = Objects.requireNonNull(expectedStructureKey, "expectedStructureKey");
            smokeChecks = List.copyOf(smokeChecks);
            if (smokeChecks.isEmpty()) {
                throw new IllegalArgumentException("Smoke checks are required");
            }
        }

        public ChunkPos focusChunk() {
            return ChunkPos.fromBlock(focusX, focusZ);
        }

        private static void requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
        }
    }
}
