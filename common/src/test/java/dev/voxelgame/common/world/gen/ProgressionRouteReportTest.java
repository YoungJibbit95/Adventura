package dev.voxelgame.common.world.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRouteReportTest {
    private static final List<Long> FIXED_SMOKE_SEEDS = List.of(
            0L,
            1L,
            42L,
            1337L,
            9001L,
            8675309L,
            123456789L,
            -1L,
            0x5EEDL,
            0xAD0E17AL
    );

    @Test
    void routeReportCompletesCoreRoutesForFixedSmokeSeeds() {
        for (long seed : FIXED_SMOKE_SEEDS) {
            ProgressionRouteReport report = ProgressionRouteReport.aroundSpawn(seed);

            assertTrue(report.warnings().isEmpty(), () -> "Seed " + seed + " warnings:\n" + report.format());
            assertEquals(7, report.routes().size());
            assertTrue(report.route("starter_supplies").orElseThrow().nearestResource().isPresent());
            assertTrue(report.route("campfire_storage").orElseThrow().nearestStructure().isPresent());
            assertTrue(report.route("pine_workbench").orElseThrow().nearestBiome().isPresent());
            assertTrue(report.route("lakeside_cooking").orElseThrow().nearestBiome().isPresent());
            assertTrue(report.route("highlands_forge").orElseThrow().nearestBiome().isPresent());
            assertTrue(report.route("ruin_adventure").orElseThrow().nearestStructure().isPresent());
        }
    }

    @Test
    void routeReportCanScanOneHundredGeneratedSeedsWithinRouteContracts() {
        for (int i = 0; i < 100; i++) {
            long seed = 0x9E3779B97F4A7C15L * (i + 1L);
            ProgressionRouteReport report = ProgressionRouteReport.aroundSpawn(seed);

            assertTrue(report.warnings().isEmpty(), () -> "Generated seed " + seed + " warnings:\n" + report.format());
        }
    }

    @Test
    void routeReportFormatsDebuggableSummaryLines() {
        ProgressionRouteReport report = ProgressionRouteReport.aroundSpawn(1337L);

        assertTrue(report.summaryLines().getFirst().contains("Route report seed 1337"));
        assertTrue(report.summaryLines().stream().anyMatch(line -> line.contains("pine_workbench")));
        assertTrue(report.summaryLines().stream().anyMatch(line -> line.contains("ruin_adventure")));
    }
}
