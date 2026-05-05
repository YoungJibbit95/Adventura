package dev.voxelgame.tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetToolMain {
    private AssetToolMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: ./gradlew :tools:run --args=\"validate-assets <asset-pack-dir>\"");
            System.out.println("Usage: ./gradlew :tools:run --args=\"atlas-report [asset-root]\"");
            return;
        }
        if ("validate-assets".equals(args[0])) {
            Path pack = Path.of(args.length > 1 ? args[1] : ".");
            validateAssetPack(pack);
            return;
        }
        if ("atlas-report".equals(args[0])) {
            Path assetRoot = args.length > 1 ? Path.of(args[1]) : defaultAssetRoot();
            System.out.print(AssetAtlasReport.generate(assetRoot).format());
            return;
        }
        throw new IllegalArgumentException("Unknown tool command: " + args[0]);
    }

    private static Path defaultAssetRoot() {
        Path rootRelative = AssetAtlasReport.DEFAULT_ASSET_ROOT;
        if (Files.isDirectory(rootRelative)) {
            return rootRelative;
        }
        Path parentRelative = Path.of("..").resolve(rootRelative).normalize();
        if (Files.isDirectory(parentRelative)) {
            return parentRelative;
        }
        return rootRelative;
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
