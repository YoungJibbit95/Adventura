package dev.voxelgame.tools;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssetAtlasReport {
    public static final Path DEFAULT_ASSET_ROOT = Path.of("client", "src", "main", "resources", "assets", "game");

    private static final List<String> FALLBACK_SHEETS = List.of(
            "ui_hud_sheet.png",
            "blocks_tiles_sheet.png",
            "tools_weapons_sheet.png",
            "nature_food_sheet.png",
            "ores_materials_sheet.png"
    );

    private static final Map<Short, List<String>> FALLBACK_SHEETS_BY_BLOCK = fallbackSheetsByBlock();

    private AssetAtlasReport() {
    }

    public static Report generate(Path assetRoot) throws IOException {
        Path normalizedRoot = assetRoot.toAbsolutePath().normalize();
        List<BlockStatus> blocks = new ArrayList<>();
        for (BlockType block : Blocks.createDefaultRegistry().values()) {
            if (block.id() <= Blocks.AIR) {
                continue;
            }
            blocks.add(statusFor(normalizedRoot, block));
        }
        return new Report(
                normalizedRoot,
                blocks,
                missingFallbackSheets(normalizedRoot),
                unmappedRootPngs(normalizedRoot),
                duplicateTextureNames(normalizedRoot)
        );
    }

    private static BlockStatus statusFor(Path assetRoot, BlockType block) {
        FaceStatus side = faceStatus(assetRoot, block.key(), TextureFace.SIDE);
        FaceStatus top = faceStatus(assetRoot, block.key(), TextureFace.TOP);
        FaceStatus bottom = faceStatus(assetRoot, block.key(), TextureFace.BOTTOM);
        List<String> fallbackSheets = FALLBACK_SHEETS_BY_BLOCK.getOrDefault(block.id(), List.of());
        boolean fallbackAvailable = fallbackSheets.stream().anyMatch(sheet -> Files.isRegularFile(assetRoot.resolve(sheet)));
        return new BlockStatus(block.id(), block.key(), side, top, bottom, fallbackAvailable, fallbackSheets);
    }

    private static FaceStatus faceStatus(Path assetRoot, String blockKey, TextureFace face) {
        List<String> candidates = textureCandidates(blockKey, face);
        for (String candidate : candidates) {
            if (Files.isRegularFile(assetRoot.resolve(candidate))) {
                return new FaceStatus(face, true, candidate, candidates);
            }
        }
        return new FaceStatus(face, false, null, candidates);
    }

    public static List<String> textureCandidates(String blockKey, TextureFace face) {
        String name = blockKey.substring(blockKey.indexOf(':') + 1);
        List<String> baseNames = new ArrayList<>();
        baseNames.add(name);
        if ("grass_block".equals(name)) {
            baseNames.add("grass");
        }

        List<String> suffixes = switch (face) {
            case TOP -> List.of("_top", "_up", "");
            case BOTTOM -> List.of("_bottom", "_down", "");
            case SIDE -> List.of("_side", "");
        };
        List<String> roots = List.of("textures/block/", "textures/blocks/");
        List<String> candidates = new ArrayList<>();
        for (String baseName : baseNames) {
            for (String suffix : suffixes) {
                for (String root : roots) {
                    String path = root + baseName + suffix + ".png";
                    if (!candidates.contains(path)) {
                        candidates.add(path);
                    }
                }
            }
        }
        return candidates;
    }

    private static List<String> missingFallbackSheets(Path assetRoot) {
        return FALLBACK_SHEETS.stream()
                .filter(sheet -> !Files.isRegularFile(assetRoot.resolve(sheet)))
                .toList();
    }

    private static List<String> unmappedRootPngs(Path assetRoot) throws IOException {
        if (!Files.isDirectory(assetRoot)) {
            return List.of();
        }
        try (var files = Files.list(assetRoot)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(name -> !FALLBACK_SHEETS.contains(name))
                    .sorted()
                    .toList();
        }
    }

    private static List<String> duplicateTextureNames(Path assetRoot) throws IOException {
        List<Path> textureRoots = List.of(assetRoot.resolve("textures/block"), assetRoot.resolve("textures/blocks"));
        Map<String, List<String>> pathsByName = new HashMap<>();
        for (Path textureRoot : textureRoots) {
            if (!Files.isDirectory(textureRoot)) {
                continue;
            }
            try (var files = Files.walk(textureRoot)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(path -> {
                            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                            String relative = assetRoot.relativize(path).toString().replace('\\', '/');
                            pathsByName.computeIfAbsent(name, ignored -> new ArrayList<>()).add(relative);
                        });
            }
        }

        List<String> duplicates = new ArrayList<>();
        pathsByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> duplicates.add(entry.getKey() + " -> " + String.join(", ", entry.getValue())));
        return duplicates;
    }

    private static Map<Short, List<String>> fallbackSheetsByBlock() {
        Map<Short, List<String>> sheets = new LinkedHashMap<>();
        put(sheets, "ui_hud_sheet.png",
                Blocks.CAMPFIRE,
                Blocks.CAMPFIRE_ACTIVE,
                Blocks.CAMPFIRE_BURNED_OUT,
                Blocks.STORAGE_CRATE,
                Blocks.SMALL_TABLE,
                Blocks.WORKBENCH,
                Blocks.WOODEN_CHAIR,
                Blocks.WOVEN_RUG,
                Blocks.SLEEPING_MAT,
                Blocks.GARDEN_FENCE
        );
        put(sheets, "nature_food_sheet.png",
                Blocks.FLOWER_POT,
                Blocks.COOKING_POT,
                Blocks.LANTERN,
                Blocks.ANCIENT_LANTERN,
                Blocks.BERRY_BUSH,
                Blocks.HERB_PLANTER,
                Blocks.WILD_GRASS,
                Blocks.REEDS,
                Blocks.TWIG_PILE,
                Blocks.SMALL_STONE,
                Blocks.SUN_BLOOM,
                Blocks.RED_MUSHROOM,
                Blocks.MUSHROOM_CLUSTER,
                Blocks.SPORE_BLOSSOM
        );
        put(sheets, "blocks_tiles_sheet.png",
                Blocks.STONE,
                Blocks.DIRT,
                Blocks.GRASS,
                Blocks.SAND,
                Blocks.SKYROOT_LOG,
                Blocks.SKYROOT_LEAVES,
                Blocks.TREE_STUMP,
                Blocks.CLAY,
                Blocks.CACTUS,
                Blocks.MOSSY_STONE,
                Blocks.MOSSY_PATH,
                Blocks.GRAVEL,
                Blocks.SNOW,
                Blocks.ICE,
                Blocks.PINE_LOG,
                Blocks.PINE_LEAVES,
                Blocks.SKYROOT_PLANKS
        );
        put(sheets, "blocks_tiles_sheet.png", Blocks.WATER, Blocks.LAVA);
        put(sheets, "nature_food_sheet.png", Blocks.TORCH);
        put(sheets, "ores_materials_sheet.png",
                Blocks.COAL_ORE,
                Blocks.IRON_ORE,
                Blocks.COPPER_ORE,
                Blocks.CLAY_DEPOSIT,
                Blocks.GLOW_CRYSTAL_NODE,
                Blocks.FORGE,
                Blocks.GLOW_MUSHROOM
        );
        return sheets;
    }

    private static void put(Map<Short, List<String>> target, String sheet, short... blockIds) {
        for (short blockId : blockIds) {
            target.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(sheet);
        }
    }

    public enum TextureFace {
        SIDE,
        TOP,
        BOTTOM
    }

    public record FaceStatus(TextureFace face, boolean present, String matchedPath, List<String> candidates) {
        public FaceStatus {
            candidates = List.copyOf(candidates);
        }
    }

    public record BlockStatus(
            short blockId,
            String blockKey,
            FaceStatus side,
            FaceStatus top,
            FaceStatus bottom,
            boolean fallbackAvailable,
            List<String> fallbackSheets
    ) {
        public BlockStatus {
            fallbackSheets = List.copyOf(fallbackSheets);
        }

        public boolean hasIndividualTexture() {
            return side.present() || top.present() || bottom.present();
        }

        public boolean fullyCovered() {
            return fallbackAvailable || side.present() && top.present() && bottom.present();
        }

        public List<TextureFace> missingFaces() {
            List<TextureFace> missing = new ArrayList<>();
            if (!side.present()) {
                missing.add(TextureFace.SIDE);
            }
            if (!top.present()) {
                missing.add(TextureFace.TOP);
            }
            if (!bottom.present()) {
                missing.add(TextureFace.BOTTOM);
            }
            return missing;
        }
    }

    public record Report(
            Path assetRoot,
            List<BlockStatus> blocks,
            List<String> missingFallbackSheets,
            List<String> unmappedRootPngs,
            List<String> duplicateTextureNames
    ) {
        public Report {
            blocks = List.copyOf(blocks);
            missingFallbackSheets = List.copyOf(missingFallbackSheets);
            unmappedRootPngs = List.copyOf(unmappedRootPngs);
            duplicateTextureNames = List.copyOf(duplicateTextureNames);
        }

        public List<BlockStatus> missingBlocks() {
            return blocks.stream()
                    .filter(block -> !block.fullyCovered())
                    .toList();
        }

        public int individualTextureBlockCount() {
            return (int) blocks.stream().filter(BlockStatus::hasIndividualTexture).count();
        }

        public int fallbackBlockCount() {
            return (int) blocks.stream().filter(BlockStatus::fallbackAvailable).count();
        }

        public String format() {
            StringBuilder builder = new StringBuilder();
            builder.append("Block atlas report: ").append(assetRoot).append(System.lineSeparator());
            builder.append("Registered blocks: ").append(blocks.size()).append(System.lineSeparator());
            builder.append("Blocks with individual textures: ").append(individualTextureBlockCount()).append(System.lineSeparator());
            builder.append("Blocks covered by fallback sheets: ").append(fallbackBlockCount()).append(System.lineSeparator());
            builder.append("Blocks missing full coverage: ").append(missingBlocks().size()).append(System.lineSeparator());

            if (!missingFallbackSheets.isEmpty()) {
                builder.append(System.lineSeparator()).append("Missing fallback sheets:").append(System.lineSeparator());
                for (String sheet : missingFallbackSheets) {
                    builder.append("- ").append(sheet).append(System.lineSeparator());
                }
            }

            if (!unmappedRootPngs.isEmpty()) {
                builder.append(System.lineSeparator()).append("Unmapped root PNG files:").append(System.lineSeparator());
                for (String png : unmappedRootPngs) {
                    builder.append("- ").append(png).append(System.lineSeparator());
                }
            }

            List<BlockStatus> missing = missingBlocks();
            if (!missing.isEmpty()) {
                builder.append(System.lineSeparator()).append("Missing block texture mappings:").append(System.lineSeparator());
                for (BlockStatus block : missing) {
                    builder.append("- ")
                            .append(block.blockKey())
                            .append(" missing ")
                            .append(block.missingFaces())
                            .append("; first candidates ")
                            .append(firstCandidates(block))
                            .append(System.lineSeparator());
                }
            }

            if (!duplicateTextureNames.isEmpty()) {
                builder.append(System.lineSeparator()).append("Duplicate texture filenames:").append(System.lineSeparator());
                for (String duplicate : duplicateTextureNames) {
                    builder.append("- ").append(duplicate).append(System.lineSeparator());
                }
            }
            return builder.toString();
        }

        private static List<String> firstCandidates(BlockStatus block) {
            List<String> candidates = new ArrayList<>();
            if (!block.side().present() && !block.side().candidates().isEmpty()) {
                candidates.add(block.side().candidates().getFirst());
            }
            if (!block.top().present() && !block.top().candidates().isEmpty()) {
                candidates.add(block.top().candidates().getFirst());
            }
            if (!block.bottom().present() && !block.bottom().candidates().isEmpty()) {
                candidates.add(block.bottom().candidates().getFirst());
            }
            return candidates;
        }
    }
}
