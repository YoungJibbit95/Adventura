package dev.voxelgame.client.render;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderingSmokeCoverageTest {
    @Test
    void worldSmokeTestsCoverRenderingDebugViewsAndPresets() throws Exception {
        String smoke = readDoc("WORLD_SMOKE_TESTS.md")
                .toLowerCase(Locale.ROOT);

        assertContains(smoke, "rendering preset matrix");
        assertContains(smoke, "/shaderreload");
        assertContains(smoke, "/preset low");
        assertContains(smoke, "/preset medium");
        assertContains(smoke, "/preset high");
        assertContains(smoke, "preset custom");
        assertContains(smoke, "/lightning");
        assertContains(smoke, "/debugview material");
        assertContains(smoke, "/debugview light");
        assertContains(smoke, "/debugview sky");
        assertContains(smoke, "/debugview block");
        assertContains(smoke, "/debugview emissive");
        assertContains(smoke, "/debugview ao");
        assertContains(smoke, "/debugview layer");
        assertContains(smoke, "/debugview uv");
        assertContains(smoke, "/debugview transparent");
    }

    @Test
    void worldSmokeTestsCoverManualRenderingRiskAreas() throws Exception {
        String smoke = readDoc("WORLD_SMOKE_TESTS.md")
                .toLowerCase(Locale.ROOT);

        assertContains(smoke, "wasser bei tag und nacht");
        assertContains(smoke, "cutout-pflanzen");
        assertContains(smoke, "glow-mushrooms nachts");
        assertContains(smoke, "campfire/lantern bei nacht");
        assertContains(smoke, "weather-lightning erzeugt keinen block-light-rebuild");
        assertContains(smoke, "long explore / chunk unload");
        assertContains(smoke, "ui/hud scale");
        assertContains(smoke, "chunk-unload/reload");
    }

    private static void assertContains(String haystack, String needle) {
        assertTrue(haystack.contains(needle), "Missing smoke-test anchor: " + needle);
    }

    private static String readDoc(String fileName) throws Exception {
        Path rootPath = Path.of("docs", fileName);
        if (Files.exists(rootPath)) {
            return Files.readString(rootPath, StandardCharsets.UTF_8);
        }
        return Files.readString(Path.of("..", "docs", fileName), StandardCharsets.UTF_8);
    }
}
