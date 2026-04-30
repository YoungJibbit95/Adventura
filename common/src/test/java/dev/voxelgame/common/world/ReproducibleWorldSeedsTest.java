package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproducibleWorldSeedsTest {
    private static final Set<String> REQUIRED_SCENARIOS = Set.of(
            "spawn",
            "river",
            "pine_forest",
            "mushroom_grove",
            "cave",
            "village_market",
            "frost_peaks",
            "desert_dunes",
            "old_ruins",
            "highlands_ores",
            "water_heavy",
            "performance_stress"
    );

    @Test
    void allRequiredSmokeScenariosAreDefinedWithUniqueKeys() {
        Set<String> keys = new HashSet<>();

        for (ReproducibleWorldSeeds.Scenario scenario : ReproducibleWorldSeeds.all()) {
            assertTrue(keys.add(scenario.key()), "Duplicate scenario key " + scenario.key());
            assertFalse(scenario.smokeChecks().isEmpty(), scenario.key() + " needs smoke checks");
        }

        assertTrue(keys.containsAll(REQUIRED_SCENARIOS));
    }

    @Test
    void biomeFocusedScenariosStillResolveToExpectedBiomes() {
        for (ReproducibleWorldSeeds.Scenario scenario : ReproducibleWorldSeeds.all()) {
            if (scenario.expectedBiomeKey().isBlank()) {
                continue;
            }
            OverworldGenerator generator = new OverworldGenerator(scenario.seed());

            assertEquals(
                    scenario.expectedBiomeKey(),
                    generator.biomeAt(scenario.focusX(), scenario.focusZ()).key(),
                    scenario.key() + " focus biome changed"
            );
        }
    }

    @Test
    void villageScenarioStillPointsAtGuaranteedCompactVillage() {
        ReproducibleWorldSeeds.Scenario scenario = ReproducibleWorldSeeds.findByKey("village_market").orElseThrow();
        OverworldGenerator generator = new OverworldGenerator(scenario.seed());

        OverworldGenerator.GeneratedStructure structure = generator.structureAtChunk(scenario.focusChunk()).orElseThrow();

        assertEquals(scenario.expectedStructureKey(), structure.template().key());
    }

    @Test
    void caveScenarioStillTargetsGeneratedAirBelowTerrain() {
        ReproducibleWorldSeeds.Scenario scenario = ReproducibleWorldSeeds.findByKey("cave").orElseThrow();
        OverworldGenerator generator = new OverworldGenerator(scenario.seed());
        Chunk chunk = new Chunk(scenario.focusChunk(), DimensionSettings.OVERWORLD);

        generator.generate(chunk);

        assertEquals(Blocks.AIR, chunk.blockId(scenario.focusX(), scenario.focusY(), scenario.focusZ()));
    }
}
