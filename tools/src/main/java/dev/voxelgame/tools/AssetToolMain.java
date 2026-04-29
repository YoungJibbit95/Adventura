package dev.voxelgame.tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetToolMain {
    private AssetToolMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: ./gradlew :tools:run --args=\"validate-assets <asset-pack-dir>\"");
            return;
        }
        if ("validate-assets".equals(args[0])) {
            Path pack = Path.of(args.length > 1 ? args[1] : ".");
            validateAssetPack(pack);
            return;
        }
        throw new IllegalArgumentException("Unknown tool command: " + args[0]);
    }

    private static void validateAssetPack(Path pack) throws Exception {
        Path license = pack.resolve("LICENSE");
        Path manifest = pack.resolve("asset-pack.properties");
        if (!Files.exists(license)) {
            throw new IllegalStateException("Asset pack is missing LICENSE: " + pack);
        }
        if (!Files.exists(manifest)) {
            throw new IllegalStateException("Asset pack is missing asset-pack.properties: " + pack);
        }
        System.out.println("Asset pack looks valid: " + pack.toAbsolutePath());
    }
}
