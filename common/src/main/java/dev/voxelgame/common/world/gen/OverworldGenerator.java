package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkTerrainCache;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.structure.BlockPlacement;
import dev.voxelgame.common.world.structure.StructureBounds;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OverworldGenerator implements WorldGenerator {
    public static final int SEA_LEVEL = 63;
    private static final int SPAWN_SEARCH_CENTER_X = 8;
    private static final int SPAWN_SEARCH_CENTER_Z = 8;
    private static final int SPAWN_SEARCH_RADIUS_BLOCKS = 48;
    private static final int SPAWN_STARTER_RESOURCE_RADIUS_BLOCKS = 14;
    private static final int BIOME_BLEND_SAMPLE_DISTANCE = 8;
    private static final double SPAWN_EYE_HEIGHT = PlayerBounds.DEFAULT.eyeHeight();
    private static final StarterResource[] STARTER_RESOURCES = {
            new StarterResource(4, 6, Blocks.TWIG_PILE),
            new StarterResource(6, 3, Blocks.TWIG_PILE),
            new StarterResource(8, 13, Blocks.TWIG_PILE),
            new StarterResource(3, 8, Blocks.SMALL_STONE),
            new StarterResource(5, 12, Blocks.SMALL_STONE),
            new StarterResource(15, 13, Blocks.SMALL_STONE),
            new StarterResource(5, 5, Blocks.WILD_GRASS),
            new StarterResource(7, 12, Blocks.WILD_GRASS),
            new StarterResource(2, 10, Blocks.WILD_GRASS),
            new StarterResource(4, 13, Blocks.BERRY_BUSH),
            new StarterResource(7, 4, Blocks.BERRY_BUSH),
            new StarterResource(2, 6, Blocks.HERB_PLANTER),
            new StarterResource(6, 14, Blocks.SUN_BLOOM),
            new StarterResource(10, 13, Blocks.RED_MUSHROOM),
            new StarterResource(14, 14, Blocks.MUSHROOM_CLUSTER),
            new StarterResource(1, 14, Blocks.FLOWER_POT),
            new StarterResource(9, 15, Blocks.LANTERN),
            new StarterResource(11, 15, Blocks.WOVEN_RUG),
            new StarterResource(12, 15, Blocks.GARDEN_FENCE),
            new StarterResource(13, 15, Blocks.GARDEN_FENCE)
    };
    private static final ProgressionBiomeAnchor[] PROGRESSION_BIOME_ANCHORS = {
            new ProgressionBiomeAnchor("pine_workbench", "voxel:pine_forest", 176, -112, 72, "voxel:simple_house"),
            new ProgressionBiomeAnchor("lakeside_cooking", "voxel:lakeside", -152, 144, 76, "voxel:campsite"),
            new ProgressionBiomeAnchor("highlands_forge", "voxel:highlands", 304, 176, 84, "voxel:watchtower"),
            new ProgressionBiomeAnchor("ruin_adventure", "voxel:old_ruins", -352, -240, 88, "voxel:small_ruin"),
            new ProgressionBiomeAnchor("mushroom_adventure", "voxel:mushroom_grove", 288, -312, 76, "voxel:mushroom_circle")
    };

    private final long seed;
    private final Registry<BiomeType> biomes;
    private volatile GenerationMetrics lastGenerationMetrics = GenerationMetrics.empty();

    public OverworldGenerator(long seed) {
        this(seed, Biomes.createDefaultRegistry());
    }

    public OverworldGenerator(long seed, Registry<BiomeType> biomes) {
        this.seed = seed;
        this.biomes = biomes;
    }

    @Override
    public void generate(Chunk chunk) {
        GenerationPlan plan = planChunk(chunk.pos());
        ChunkTerrainCache terrainCache = plan.terrainCache();
        GenerationMetricsBuilder metrics = new GenerationMetricsBuilder(plan.metrics());
        chunk.setTerrainCache(terrainCache);
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;

        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int x = baseX + localX;
                int z = baseZ + localZ;
                BiomeType biome = terrainCache.biomeAtLocal(localX, localZ);
                int height = terrainCache.heightAtLocal(localX, localZ);
                fillColumn(chunk, x, z, height, biome, terrainCache.surfaceBlockAtLocal(localX, localZ));
                decorateColumn(chunk, x, z, height, biome, metrics);
            }
        }
        decorateChunkStructures(chunk, plan.structure());
        decorateStarterResources(chunk, terrainCache, metrics);
        lastGenerationMetrics = metrics.build();
    }

    public GenerationPlan planChunk(ChunkPos pos) {
        ChunkTerrainCache terrainCache = terrainCacheForChunk(pos);
        Optional<GeneratedStructure> structure = structureAtChunk(terrainCache);
        Optional<SpawnPoint> spawnPoint = new ChunkPos(0, 0).equals(pos)
                ? Optional.of(safeSpawnPoint())
                : Optional.empty();
        GenerationMetrics metrics = GenerationMetrics.planned(
                ChunkTerrainCache.COLUMN_COUNT,
                ChunkTerrainCache.COLUMN_COUNT,
                1,
                structure.isPresent() ? 1 : 0,
                structure.map(value -> value.template().lootMarkers().size()).orElse(0),
                structure.map(value -> value.template().markers("entity").size()).orElse(0),
                spawnPoint.map(SpawnPoint::candidatesScanned).orElse(0),
                spawnPoint.map(SpawnPoint::candidatesAccepted).orElse(0)
        );
        return new GenerationPlan(terrainCache, structure, spawnPoint, metrics);
    }

    public GenerationMetrics lastGenerationMetrics() {
        return lastGenerationMetrics;
    }

    public static List<ProgressionBiomeAnchor> progressionBiomeAnchors() {
        return List.of(PROGRESSION_BIOME_ANCHORS);
    }

    public ChunkTerrainCache terrainCacheForChunk(ChunkPos pos) {
        int[] heights = new int[ChunkTerrainCache.COLUMN_COUNT];
        BiomeType[] chunkBiomes = new BiomeType[ChunkTerrainCache.COLUMN_COUNT];
        short[] surfaceBlocks = new short[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] fluidColumns = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] caveColumns = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        byte[] fluidDepthHints = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] shoreMasks = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] fluidSurfaceFlags = new byte[ChunkTerrainCache.COLUMN_COUNT];
        int baseX = pos.x() * ChunkPos.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int x = baseX + localX;
                int z = baseZ + localZ;
                int index = localZ * ChunkPos.SIZE + localX;
                BiomeType biome = biomeAt(x, z);
                chunkBiomes[index] = biome;
                int height = terrainHeight(x, z, biome);
                heights[index] = height;
                surfaceBlocks[index] = surfaceBlockFor(x, z, height, biome);
                fluidColumns[index] = height < SEA_LEVEL;
                caveColumns[index] = hasCaveColumn(x, z, height);
            }
        }
        FluidSurfacePlanner.fillChunk(
                SEA_LEVEL,
                pos,
                heights,
                fluidColumns,
                fluidDepthHints,
                shoreMasks,
                fluidSurfaceFlags,
                this::terrainHeightAt,
                this::riverStrength
        );
        return new ChunkTerrainCache(pos, heights, chunkBiomes, surfaceBlocks, fluidColumns, caveColumns, fluidDepthHints, shoreMasks, fluidSurfaceFlags);
    }

    public BiomeType biomeAt(int x, int z) {
        ProgressionBiomeAnchor progressionAnchor = progressionBiomeAnchorAt(x, z);
        if (progressionAnchor != null) {
            return biomes.requireByKey(progressionAnchor.biomeKey());
        }
        ClimateSample climate = climateAt(x, z);
        double temperature = climate.temperature();
        double moisture = climate.moisture();
        double ridge = ValueNoise.fbm(seed ^ 0x7711, x, z, 4, 0.004, 0.5);
        double flower = ValueNoise.fbm(seed ^ 0xF10AEL, x, z, 3, 0.006, 0.52);
        double ruin = normalize(ValueNoise.fbm(seed ^ 0x0D12115L, x, z, 2, 0.0018, 0.6));
        if (riverStrength(x, z) > 0.58 && temperature > 0.36) {
            return biomes.requireByKey("voxel:lakeside");
        }
        if (temperature < 0.30 && ridge > 0.12) {
            return biomes.requireByKey("voxel:frost_peaks");
        }
        if (temperature > 0.72 && moisture < 0.35) {
            return biomes.requireByKey("voxel:sun_dunes");
        }
        if (ruin > 0.88 && ridge > 0.10) {
            return biomes.requireByKey("voxel:old_ruins");
        }
        if (moisture > 0.80 && temperature > 0.38 && temperature < 0.70) {
            return biomes.requireByKey("voxel:mushroom_grove");
        }
        if (moisture > 0.78 && temperature > 0.42) {
            return biomes.requireByKey("voxel:mire");
        }
        if (temperature > 0.55 && moisture > 0.44 && flower > 0.28) {
            return biomes.requireByKey("voxel:flower_fields");
        }
        if (moisture > 0.56 && temperature < 0.58) {
            return biomes.requireByKey("voxel:pine_forest");
        }
        if (moisture > 0.62) {
            return biomes.requireByKey("voxel:skyroot_forest");
        }
        if (ridge > 0.42) {
            return biomes.requireByKey("voxel:highlands");
        }
        return biomes.requireByKey("voxel:cozy_meadow");
    }

    public BiomeTransition biomeTransitionAt(int x, int z) {
        String center = biomeAt(x, z).key();
        int differing = 0;
        int samples = 0;
        for (int dz = -BIOME_BLEND_SAMPLE_DISTANCE; dz <= BIOME_BLEND_SAMPLE_DISTANCE; dz += BIOME_BLEND_SAMPLE_DISTANCE) {
            for (int dx = -BIOME_BLEND_SAMPLE_DISTANCE; dx <= BIOME_BLEND_SAMPLE_DISTANCE; dx += BIOME_BLEND_SAMPLE_DISTANCE) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                samples++;
                if (!biomeAt(x + dx, z + dz).key().equals(center)) {
                    differing++;
                }
            }
        }
        return new BiomeTransition(center, differing, samples);
    }

    public int terrainHeight(int x, int z, BiomeType biome) {
        double continents = ValueNoise.fbm(seed, x, z, 5, 0.0016, 0.5);
        double erosion = normalize(ValueNoise.fbm(seed ^ 0xE70510L, x, z, 4, 0.0032, 0.5));
        double hills = ValueNoise.fbm(seed ^ 0x1234ABCDL, x, z, 4, 0.009, 0.48);
        double detail = ValueNoise.fbm(seed ^ 0xD37A11L, x, z, 2, 0.035, 0.45);
        double ridge = Math.abs(ValueNoise.fbm(seed ^ 0xA77A11L, x, z, 4, 0.005, 0.5));
        double mountain = Math.pow(1.0 - ridge, 2.4) * 54.0 * (1.0 - erosion * 0.65);
        int base = 70 + (int) Math.round(continents * 36.0 + hills * 14.0 + detail * 3.0 + mountain);
        base += (int) Math.round(blendedBiomeHeightAdjustment(x, z, biome, base, mountain));
        double river = riverStrength(x, z);
        if (river > 0.0) {
            int riverBed = SEA_LEVEL - 4 + (int) Math.round(ValueNoise.smooth(seed ^ 0xA11EL, x * 0.04, z * 0.04) * 2.0);
            base = (int) Math.round(base * (1.0 - river) + riverBed * river);
        }
        return clamp(base, 28, 235);
    }

    private int terrainHeightAt(int x, int z) {
        BiomeType biome = biomeAt(x, z);
        return terrainHeight(x, z, biome);
    }

    private void fillColumn(Chunk chunk, int x, int z, int height, BiomeType biome, short surfaceBlock) {
        for (int y = chunk.dimension().minY(); y < chunk.dimension().maxYExclusive(); y++) {
            short block = Blocks.AIR;
            if (y <= height) {
                if (isCave(x, y, z) && y < height - 4) {
                    block = Blocks.AIR;
                } else if (y == height) {
                    block = surfaceBlock;
                } else if (y >= height - 4) {
                    block = subsurfaceBlockFor(x, z, height, biome);
                } else {
                    block = oreOrStone(x, y, z);
                }
            } else if (y <= SEA_LEVEL) {
                block = frozenWaterFor(biome, y);
            }
            chunk.setBlockId(x, y, z, block);
        }
    }

    private void decorateColumn(Chunk chunk, int x, int z, int height, BiomeType biome, GenerationMetricsBuilder metrics) {
        if (height + 7 >= chunk.dimension().maxYExclusive() || height < SEA_LEVEL - 3) {
            return;
        }

        double chance = normalize(ValueNoise.hashUnit(seed ^ 0x51A7, x, z));
        if (("voxel:frost_peaks".equals(biome.key()) || "voxel:pine_forest".equals(biome.key())) && chance < biome.treeChance()) {
            if (canPlaceTree(chunk, x, height + 1, z)) {
                placePineTree(chunk, x, height + 1, z);
                metrics.featurePlacement();
                return;
            }
            metrics.rejectedPlacement();
        }

        if (chance < biome.treeChance()) {
            if (canPlaceTree(chunk, x, height + 1, z)) {
                placeTree(chunk, x, height + 1, z);
                metrics.featurePlacement();
                return;
            }
            metrics.rejectedPlacement();
        }

        double plantChance = normalize(ValueNoise.hashUnit(seed ^ 0x61B7, x, z));
        if (plantChance < biome.plantChance()) {
            short plant = plantChance < biome.plantChance() * 0.15 ? Blocks.SUN_BLOOM : Blocks.WILD_GRASS;
            if (("voxel:mire".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key())) && plantChance < biome.plantChance() * 0.55) {
                plant = "voxel:mushroom_grove".equals(biome.key()) && plantChance < biome.plantChance() * 0.25
                        ? Blocks.MUSHROOM_CLUSTER
                        : Blocks.RED_MUSHROOM;
            } else if ("voxel:flower_fields".equals(biome.key()) && plantChance < biome.plantChance() * 0.60) {
                plant = Blocks.SUN_BLOOM;
            } else if ("voxel:lakeside".equals(biome.key()) && plantChance < biome.plantChance() * 0.35) {
                plant = Blocks.BERRY_BUSH;
            }
            chunk.setBlockId(x, height + 1, z, plant);
            metrics.featurePlacement();
        }

        double detailChance = normalize(ValueNoise.hashUnit(seed ^ 0xC07ED11L, x, z));
        short detailResource = detailResourceFor(biome.key(), detailChance);
        if (detailResource != Blocks.AIR) {
            if (canPlaceArea(chunk, x, height + 1, z, 1, 0)) {
                chunk.setBlockId(x, height + 1, z, detailResource);
                metrics.featurePlacement();
            } else {
                metrics.rejectedPlacement();
            }
        }

        if ("voxel:sun_dunes".equals(biome.key())) {
            double cactusChance = normalize(ValueNoise.hashUnit(seed ^ 0xCA77L, x, z));
            if (cactusChance < 0.012) {
                if (canPlaceArea(chunk, x, height + 1, z, 4, 0)) {
                    placeCactus(chunk, x, height + 1, z, 2 + (int) Math.floor(cactusChance * 180.0));
                    metrics.featurePlacement();
                } else {
                    metrics.rejectedPlacement();
                }
            }
        } else if ("voxel:highlands".equals(biome.key()) || "voxel:old_ruins".equals(biome.key())) {
            double boulderChance = normalize(ValueNoise.hashUnit(seed ^ 0xB011L, x, z));
            if (boulderChance < 0.008) {
                if (canPlaceArea(chunk, x, height + 1, z, 3, 2)) {
                    placeBoulder(chunk, x, height + 1, z);
                    metrics.featurePlacement();
                } else {
                    metrics.rejectedPlacement();
                }
            }
        } else if ("voxel:mire".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key())) {
            double stumpChance = normalize(ValueNoise.hashUnit(seed ^ 0x57ADEL, x, z));
            if (stumpChance < 0.010) {
                if (canPlaceArea(chunk, x, height + 1, z, 2, 1)) {
                    chunk.setBlockId(x, height + 1, z, Blocks.TREE_STUMP);
                    chunk.setBlockId(x, height + 2, z, Blocks.RED_MUSHROOM);
                    metrics.featurePlacement();
                } else {
                    metrics.rejectedPlacement();
                }
            }
        }
    }

    private void decorateChunkStructures(Chunk chunk, Optional<GeneratedStructure> structure) {
        structure.ifPresent(value -> {
            prepareStructureSite(chunk, value);
            value.template().placeIntoChunk(chunk, value.originX(), value.originY(), value.originZ());
        });
    }

    private void decorateStarterResources(Chunk chunk, ChunkTerrainCache terrainCache, GenerationMetricsBuilder metrics) {
        if (!new ChunkPos(0, 0).equals(chunk.pos())) {
            return;
        }
        for (StarterResource resource : STARTER_RESOURCES) {
            if (placeStarterResource(chunk, terrainCache, resource.x(), resource.z(), resource.blockId())) {
                metrics.featurePlacement();
            } else {
                metrics.rejectedPlacement();
            }
        }
    }

    private boolean placeStarterResource(Chunk chunk, ChunkTerrainCache terrainCache, int x, int z, short blockId) {
        if (!ChunkPos.fromBlock(x, z).equals(chunk.pos())) {
            return false;
        }
        int y = terrainCache.heightAtWorld(x, z) + 1;
        if (!chunk.dimension().containsY(y) || !chunk.dimension().containsY(y - 1)) {
            return false;
        }
        short support = chunk.blockId(x, y - 1, z);
        if (support == Blocks.AIR || support == Blocks.WATER) {
            return false;
        }
        chunk.setBlockId(x, y, z, blockId);
        return true;
    }

    public Optional<GeneratedStructure> structureAtChunk(ChunkPos pos) {
        return structureAtChunk(terrainCacheForChunk(pos));
    }

    public Optional<GeneratedStructure> structureAtChunk(ChunkTerrainCache terrainCache) {
        Objects.requireNonNull(terrainCache, "terrainCache");
        ChunkPos pos = terrainCache.pos();
        int centerX = pos.x() * ChunkPos.SIZE + 8;
        int centerZ = pos.z() * ChunkPos.SIZE + 8;
        BiomeType biome = terrainCache.biomeAtWorld(centerX, centerZ);
        if (pos.x() == 0 && pos.z() == 0) {
            int campX = centerX + 4;
            int campZ = centerZ;
            StructureTemplate template = Structures.campsite();
            return Optional.of(new GeneratedStructure(template, campX, structureOriginY(terrainCache, template, campX, campZ), campZ));
        }
        if (pos.x() == 1 && pos.z() == 1) {
            StructureTemplate template = Structures.compactVillage();
            return Optional.of(new GeneratedStructure(template, centerX, structureOriginY(terrainCache, template, centerX, centerZ), centerZ));
        }
        ProgressionBiomeAnchor progressionAnchor = progressionStructureAnchorAt(pos);
        if (progressionAnchor != null) {
            StructureTemplate template = progressionStructureTemplate(progressionAnchor.guaranteedStructureKey());
            return Optional.of(new GeneratedStructure(template, centerX, structureOriginY(terrainCache, template, centerX, centerZ), centerZ));
        }
        double roll = normalize(ValueNoise.hashUnit(seed ^ 0x57711A6EL, pos.x(), pos.z()));
        double villageRoll = normalize(ValueNoise.hashUnit(seed ^ 0xA911A6EL, pos.x(), pos.z()));
        if (("voxel:meadow".equals(biome.key()) || "voxel:cozy_meadow".equals(biome.key()) || "voxel:flower_fields".equals(biome.key()) || "voxel:skyroot_forest".equals(biome.key())) && villageRoll < 0.014) {
            StructureTemplate template = Structures.compactVillage();
            return Optional.of(new GeneratedStructure(template, centerX, structureOriginY(terrainCache, template, centerX, centerZ), centerZ));
        }
        if (roll > biome.structureChance()) {
            return Optional.empty();
        }
        StructureTemplate template;
        if ("voxel:sun_dunes".equals(biome.key())) {
            template = Structures.desertWell();
        } else if ("voxel:meadow".equals(biome.key()) || "voxel:cozy_meadow".equals(biome.key()) || "voxel:flower_fields".equals(biome.key()) || "voxel:lakeside".equals(biome.key()) || "voxel:skyroot_forest".equals(biome.key())) {
            if (roll < biome.structureChance() * 0.35) {
                template = Structures.campsite();
            } else {
                template = Structures.simpleHouse();
            }
        } else if ("voxel:frost_peaks".equals(biome.key())) {
            template = Structures.smallRuin();
        } else if ("voxel:highlands".equals(biome.key()) || "voxel:old_ruins".equals(biome.key())) {
            template = Structures.watchtower();
        } else if ("voxel:mushroom_grove".equals(biome.key())) {
            template = roll < biome.structureChance() * 0.70 ? Structures.mushroomCircle() : Structures.smallRuin();
        } else {
            template = Structures.smallRuin();
        }
        return Optional.of(new GeneratedStructure(template, centerX, structureOriginY(terrainCache, template, centerX, centerZ), centerZ));
    }

    private ProgressionBiomeAnchor progressionBiomeAnchorAt(int x, int z) {
        for (ProgressionBiomeAnchor anchor : PROGRESSION_BIOME_ANCHORS) {
            int dx = x - anchor.centerX();
            int dz = z - anchor.centerZ();
            double shapeNoise = ValueNoise.fbm(seed ^ 0xA170A71EL, x, z, 2, 0.018, 0.52);
            double radius = anchor.radiusBlocks() * (1.0 + shapeNoise * 0.12);
            if (dx * dx + dz * dz <= radius * radius) {
                return anchor;
            }
        }
        return null;
    }

    private static ProgressionBiomeAnchor progressionStructureAnchorAt(ChunkPos pos) {
        for (ProgressionBiomeAnchor anchor : PROGRESSION_BIOME_ANCHORS) {
            if (ChunkPos.fromBlock(anchor.centerX(), anchor.centerZ()).equals(pos)) {
                return anchor;
            }
        }
        return null;
    }

    private static StructureTemplate progressionStructureTemplate(String structureKey) {
        return switch (structureKey) {
            case "voxel:campsite" -> Structures.campsite();
            case "voxel:simple_house" -> Structures.simpleHouse();
            case "voxel:watchtower" -> Structures.watchtower();
            case "voxel:small_ruin" -> Structures.smallRuin();
            case "voxel:mushroom_circle" -> Structures.mushroomCircle();
            case "voxel:compact_village" -> Structures.compactVillage();
            case "voxel:desert_well" -> Structures.desertWell();
            default -> throw new IllegalArgumentException("Unknown progression structure key: " + structureKey);
        };
    }

    private int structureOriginY(ChunkTerrainCache terrainCache, StructureTemplate template, int originX, int originZ) {
        StructureBounds bounds = StructureBounds.fromTemplate(template);
        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        int totalSurface = 0;
        int samples = 0;
        for (int dz = bounds.minZ(); dz <= bounds.maxZ(); dz++) {
            for (int dx = bounds.minX(); dx <= bounds.maxX(); dx++) {
                int x = originX + dx;
                int z = originZ + dz;
                if (!ChunkPos.fromBlock(x, z).equals(terrainCache.pos())) {
                    continue;
                }
                int height = terrainCache.heightAtWorld(x, z);
                minSurface = Math.min(minSurface, height);
                maxSurface = Math.max(maxSurface, height);
                totalSurface += height;
                samples++;
            }
        }
        if (samples == 0) {
            return terrainCache.heightAtWorld(originX, originZ) + 1;
        }
        int averageSurface = Math.round(totalSurface / (float) samples);
        int centerSurface = terrainCache.heightAtWorld(originX, originZ);
        int maxLift = Math.min(maxSurface + 1, centerSurface + 3);
        int maxCut = Math.max(minSurface + 1, centerSurface - 3);
        return clamp(averageSurface + 1, Math.min(maxCut, maxLift), Math.max(maxCut, maxLift));
    }

    private void prepareStructureSite(Chunk chunk, GeneratedStructure generated) {
        StructureBounds bounds = StructureBounds.fromTemplate(generated.template());
        int baseY = generated.originY() + bounds.minY();
        int clearTop = generated.originY() + bounds.maxY() + 2;
        short foundation = foundationBlockFor(generated.template().key());
        for (int dz = bounds.minZ(); dz <= bounds.maxZ(); dz++) {
            for (int dx = bounds.minX(); dx <= bounds.maxX(); dx++) {
                int x = generated.originX() + dx;
                int z = generated.originZ() + dz;
                if (!ChunkPos.fromBlock(x, z).equals(chunk.pos())) {
                    continue;
                }
                int surfaceY = terrainHeightAt(x, z);
                for (int y = surfaceY + 1; y < baseY; y++) {
                    if (chunk.dimension().containsY(y)) {
                        chunk.setBlockId(x, y, z, foundation);
                    }
                }
                for (int y = baseY; y <= clearTop; y++) {
                    if (chunk.dimension().containsY(y)) {
                        chunk.setBlockId(x, y, z, Blocks.AIR);
                    }
                }
            }
        }
    }

    private static short foundationBlockFor(String templateKey) {
        if ("voxel:desert_well".equals(templateKey)) {
            return Blocks.SAND;
        }
        if ("voxel:watchtower".equals(templateKey) || "voxel:small_ruin".equals(templateKey)) {
            return Blocks.STONE;
        }
        return Blocks.MOSSY_STONE;
    }

    public SpawnPoint safeSpawnPoint() {
        Map<ChunkPos, ChunkTerrainCache> terrainCaches = new HashMap<>();
        SpawnCandidate best = null;
        int scanned = 0;
        int accepted = 0;
        for (int z = SPAWN_SEARCH_CENTER_Z - SPAWN_SEARCH_RADIUS_BLOCKS; z <= SPAWN_SEARCH_CENTER_Z + SPAWN_SEARCH_RADIUS_BLOCKS; z++) {
            for (int x = SPAWN_SEARCH_CENTER_X - SPAWN_SEARCH_RADIUS_BLOCKS; x <= SPAWN_SEARCH_CENTER_X + SPAWN_SEARCH_RADIUS_BLOCKS; x++) {
                scanned++;
                Optional<SpawnCandidate> candidate = spawnCandidateAt(terrainCaches, x, z);
                if (candidate.isEmpty()) {
                    continue;
                }
                accepted++;
                if (best == null || candidate.get().score() > best.score()) {
                    best = candidate.get();
                }
            }
        }
        if (best != null) {
            return best.toSpawnPoint(scanned, accepted, false);
        }

        ChunkTerrainCache fallbackCache = terrainCaches.computeIfAbsent(
                ChunkPos.fromBlock(SPAWN_SEARCH_CENTER_X, SPAWN_SEARCH_CENTER_Z),
                this::terrainCacheForChunk
        );
        BiomeType biome = fallbackCache.biomeAtWorld(SPAWN_SEARCH_CENTER_X, SPAWN_SEARCH_CENTER_Z);
        int surfaceY = fallbackCache.heightAtWorld(SPAWN_SEARCH_CENTER_X, SPAWN_SEARCH_CENTER_Z);
        return new SpawnPoint(
                SPAWN_SEARCH_CENTER_X,
                SPAWN_SEARCH_CENTER_Z,
                surfaceY,
                surfaceY + 1,
                SPAWN_SEARCH_CENTER_X + 0.5,
                surfaceY + 1 + SPAWN_EYE_HEIGHT,
                SPAWN_SEARCH_CENTER_Z + 0.5,
                biome.key(),
                true,
                scanned,
                accepted
        );
    }

    private Optional<SpawnCandidate> spawnCandidateAt(Map<ChunkPos, ChunkTerrainCache> terrainCaches, int x, int z) {
        if (isStarterResourceColumn(x, z)) {
            return Optional.empty();
        }
        ChunkPos pos = ChunkPos.fromBlock(x, z);
        ChunkTerrainCache terrainCache = terrainCaches.computeIfAbsent(pos, this::terrainCacheForChunk);
        BiomeType biome = terrainCache.biomeAtWorld(x, z);
        int surfaceY = terrainCache.heightAtWorld(x, z);
        int feetY = surfaceY + 1;
        if (!DimensionSettings.OVERWORLD.containsY(surfaceY) || !DimensionSettings.OVERWORLD.containsY(feetY + 1)) {
            return Optional.empty();
        }
        if (surfaceY < SEA_LEVEL) {
            return Optional.empty();
        }
        short support = surfaceBlockFor(x, z, surfaceY, biome);
        if (!isSpawnSupportBlock(support)) {
            return Optional.empty();
        }
        if (riverStrength(x, z) > 0.78) {
            return Optional.empty();
        }
        if (decorationWouldBlockSpawn(pos, x, z, feetY, biome)) {
            return Optional.empty();
        }
        if (structureWouldBlockSpawn(terrainCache, x, z, feetY)) {
            return Optional.empty();
        }
        double score = spawnScore(x, z, surfaceY, biome);
        return Optional.of(new SpawnCandidate(x, z, surfaceY, feetY, biome.key(), score));
    }

    private double spawnScore(int x, int z, int surfaceY, BiomeType biome) {
        double distanceFromStart = square(x - SPAWN_SEARCH_CENTER_X) + square(z - SPAWN_SEARCH_CENTER_Z);
        double starterDistance = nearestStarterResourceDistance(x, z);
        double score = 0.0;
        if (isPreferredSpawnBiome(biome.key())) {
            score += 10_000.0;
        }
        if (starterDistance <= SPAWN_STARTER_RESOURCE_RADIUS_BLOCKS) {
            score += 2_000.0 - starterDistance * 50.0;
        } else {
            score -= starterDistance * 6.0;
        }
        score -= distanceFromStart * 2.0;
        score -= Math.abs(surfaceY - 72) * 3.0;
        return score;
    }

    private boolean decorationWouldBlockSpawn(ChunkPos pos, int x, int z, int feetY, BiomeType biome) {
        double treeChance = normalize(ValueNoise.hashUnit(seed ^ 0x51A7, x, z));
        if (treeChance < biome.treeChance() && canPlaceTreeAt(pos, x, feetY, z)) {
            return true;
        }

        double detailChance = normalize(ValueNoise.hashUnit(seed ^ 0xC07ED11L, x, z));
        short detailResource = detailResourceFor(biome.key(), detailChance);
        if (isSpawnBlockingBlock(detailResource) && canPlaceAreaAt(pos, x, feetY, z, 1, 0)) {
            return true;
        }

        if ("voxel:sun_dunes".equals(biome.key())) {
            double cactusChance = normalize(ValueNoise.hashUnit(seed ^ 0xCA77L, x, z));
            return cactusChance < 0.012 && canPlaceAreaAt(pos, x, feetY, z, 4, 0);
        }
        if ("voxel:highlands".equals(biome.key()) || "voxel:old_ruins".equals(biome.key())) {
            double boulderChance = normalize(ValueNoise.hashUnit(seed ^ 0xB011L, x, z));
            return boulderChance < 0.008 && canPlaceAreaAt(pos, x, feetY, z, 3, 2);
        }
        if ("voxel:mire".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key())) {
            double stumpChance = normalize(ValueNoise.hashUnit(seed ^ 0x57ADEL, x, z));
            return stumpChance < 0.010 && canPlaceAreaAt(pos, x, feetY, z, 2, 1);
        }
        return false;
    }

    private boolean structureWouldBlockSpawn(ChunkTerrainCache terrainCache, int x, int z, int feetY) {
        Optional<GeneratedStructure> structure = structureAtChunk(terrainCache);
        if (structure.isEmpty()) {
            return false;
        }
        GeneratedStructure generated = structure.get();
        for (BlockPlacement block : generated.template().blocks()) {
            int blockX = generated.originX() + block.x();
            int blockY = generated.originY() + block.y();
            int blockZ = generated.originZ() + block.z();
            if (blockX == x && blockZ == z && blockY >= feetY && blockY <= feetY + 2 && isSpawnBlockingBlock(block.blockId())) {
                return true;
            }
        }
        return false;
    }

    public record GeneratedStructure(StructureTemplate template, int originX, int originY, int originZ) {
        public GeneratedStructure {
            Objects.requireNonNull(template, "template");
        }
    }

    public record ProgressionBiomeAnchor(
            String routeKey,
            String biomeKey,
            int centerX,
            int centerZ,
            int radiusBlocks,
            String guaranteedStructureKey
    ) {
        public ProgressionBiomeAnchor {
            Objects.requireNonNull(routeKey, "routeKey");
            Objects.requireNonNull(biomeKey, "biomeKey");
            Objects.requireNonNull(guaranteedStructureKey, "guaranteedStructureKey");
            if (routeKey.isBlank() || biomeKey.isBlank() || guaranteedStructureKey.isBlank()) {
                throw new IllegalArgumentException("Progression route anchors need route, biome and structure keys");
            }
            if (radiusBlocks <= 0) {
                throw new IllegalArgumentException("Progression route radius must be positive");
            }
        }

        public ChunkPos chunkPos() {
            return ChunkPos.fromBlock(centerX, centerZ);
        }
    }

    public record GenerationPlan(
            ChunkTerrainCache terrainCache,
            Optional<GeneratedStructure> structure,
            Optional<SpawnPoint> spawnPoint,
            GenerationMetrics metrics
    ) {
        public GenerationPlan {
            Objects.requireNonNull(terrainCache, "terrainCache");
            structure = structure == null ? Optional.empty() : structure;
            spawnPoint = spawnPoint == null ? Optional.empty() : spawnPoint;
            metrics = metrics == null ? GenerationMetrics.empty() : metrics;
        }
    }

    public record GenerationMetrics(
            int biomeSamples,
            int heightSamples,
            int featurePlacements,
            int structureAttempts,
            int structureSuccesses,
            int rejectedPlacements,
            int lootMarkers,
            int ambientEntityMarkers,
            int spawnCandidatesScanned,
            int spawnCandidatesAccepted
    ) {
        public GenerationMetrics {
            if (biomeSamples < 0 || heightSamples < 0 || featurePlacements < 0 || structureAttempts < 0
                    || structureSuccesses < 0 || rejectedPlacements < 0 || lootMarkers < 0
                    || ambientEntityMarkers < 0 || spawnCandidatesScanned < 0 || spawnCandidatesAccepted < 0) {
                throw new IllegalArgumentException("Generation metrics must be non-negative");
            }
        }

        static GenerationMetrics empty() {
            return new GenerationMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        static GenerationMetrics planned(
                int biomeSamples,
                int heightSamples,
                int structureAttempts,
                int structureSuccesses,
                int lootMarkers,
                int ambientEntityMarkers,
                int spawnCandidatesScanned,
                int spawnCandidatesAccepted
        ) {
            return new GenerationMetrics(
                    biomeSamples,
                    heightSamples,
                    0,
                    structureAttempts,
                    structureSuccesses,
                    0,
                    lootMarkers,
                    ambientEntityMarkers,
                    spawnCandidatesScanned,
                    spawnCandidatesAccepted
            );
        }
    }

    public record SpawnPoint(
            int blockX,
            int blockZ,
            int surfaceY,
            int feetY,
            double eyeX,
            double eyeY,
            double eyeZ,
            String biomeKey,
            boolean fallback,
            int candidatesScanned,
            int candidatesAccepted
    ) {
        public SpawnPoint {
            Objects.requireNonNull(biomeKey, "biomeKey");
            if (!Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
                throw new IllegalArgumentException("Spawn coordinates must be finite");
            }
            if (candidatesScanned < 0 || candidatesAccepted < 0) {
                throw new IllegalArgumentException("Spawn candidate counts must be non-negative");
            }
        }
    }

    private record SpawnCandidate(int x, int z, int surfaceY, int feetY, String biomeKey, double score) {
        private SpawnPoint toSpawnPoint(int candidatesScanned, int candidatesAccepted, boolean fallback) {
            return new SpawnPoint(
                    x,
                    z,
                    surfaceY,
                    feetY,
                    x + 0.5,
                    feetY + SPAWN_EYE_HEIGHT,
                    z + 0.5,
                    biomeKey,
                    fallback,
                    candidatesScanned,
                    candidatesAccepted
            );
        }
    }

    private record StarterResource(int x, int z, short blockId) {
    }

    private static boolean isStarterResourceColumn(int x, int z) {
        for (StarterResource resource : STARTER_RESOURCES) {
            if (resource.x() == x && resource.z() == z) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPreferredSpawnBiome(String biomeKey) {
        return "voxel:cozy_meadow".equals(biomeKey)
                || "voxel:flower_fields".equals(biomeKey)
                || "voxel:lakeside".equals(biomeKey);
    }

    private static boolean isSpawnSupportBlock(short blockId) {
        return switch (blockId) {
            case Blocks.GRASS, Blocks.DIRT, Blocks.STONE, Blocks.SAND, Blocks.CLAY, Blocks.GRAVEL,
                    Blocks.SNOW, Blocks.MOSSY_STONE, Blocks.MOSSY_PATH -> true;
            default -> false;
        };
    }

    private static boolean isSpawnBlockingBlock(short blockId) {
        return switch (blockId) {
            case Blocks.AIR, Blocks.WILD_GRASS, Blocks.SUN_BLOOM, Blocks.RED_MUSHROOM,
                    Blocks.BERRY_BUSH, Blocks.HERB_PLANTER, Blocks.CAMPFIRE, Blocks.SMALL_STONE,
                    Blocks.MUSHROOM_CLUSTER, Blocks.CLAY_DEPOSIT, Blocks.REEDS, Blocks.TWIG_PILE,
                    Blocks.LANTERN, Blocks.WOVEN_RUG, Blocks.TORCH, Blocks.COOKING_POT,
                    Blocks.FLOWER_POT, Blocks.GLOW_MUSHROOM, Blocks.SPORE_BLOSSOM -> false;
            default -> true;
        };
    }

    private static double nearestStarterResourceDistance(int x, int z) {
        double nearest = Double.POSITIVE_INFINITY;
        for (StarterResource resource : STARTER_RESOURCES) {
            nearest = Math.min(nearest, Math.sqrt(square(x - resource.x()) + square(z - resource.z())));
        }
        return nearest;
    }

    private static int square(int value) {
        return value * value;
    }

    private static final class GenerationMetricsBuilder {
        private int biomeSamples;
        private int heightSamples;
        private int featurePlacements;
        private int structureAttempts;
        private int structureSuccesses;
        private int rejectedPlacements;
        private int lootMarkers;
        private int ambientEntityMarkers;
        private int spawnCandidatesScanned;
        private int spawnCandidatesAccepted;

        private GenerationMetricsBuilder(GenerationMetrics initial) {
            this.biomeSamples = initial.biomeSamples();
            this.heightSamples = initial.heightSamples();
            this.featurePlacements = initial.featurePlacements();
            this.structureAttempts = initial.structureAttempts();
            this.structureSuccesses = initial.structureSuccesses();
            this.rejectedPlacements = initial.rejectedPlacements();
            this.lootMarkers = initial.lootMarkers();
            this.ambientEntityMarkers = initial.ambientEntityMarkers();
            this.spawnCandidatesScanned = initial.spawnCandidatesScanned();
            this.spawnCandidatesAccepted = initial.spawnCandidatesAccepted();
        }

        private void featurePlacement() {
            featurePlacements++;
        }

        private void rejectedPlacement() {
            rejectedPlacements++;
        }

        private GenerationMetrics build() {
            return new GenerationMetrics(
                    biomeSamples,
                    heightSamples,
                    featurePlacements,
                    structureAttempts,
                    structureSuccesses,
                    rejectedPlacements,
                    lootMarkers,
                    ambientEntityMarkers,
                    spawnCandidatesScanned,
                    spawnCandidatesAccepted
            );
        }
    }

    private boolean canPlaceTree(Chunk chunk, int x, int y, int z) {
        return ChunkPos.fromBlock(x, z).equals(chunk.pos()) && canPlaceTreeAt(chunk.pos(), x, y, z);
    }

    private boolean canPlaceTreeAt(ChunkPos pos, int x, int y, int z) {
        return x > pos.x() * ChunkPos.SIZE + 2
                && z > pos.z() * ChunkPos.SIZE + 2
                && x < pos.x() * ChunkPos.SIZE + 13
                && z < pos.z() * ChunkPos.SIZE + 13
                && y + 7 < DimensionSettings.OVERWORLD.maxYExclusive();
    }

    private boolean canPlaceAreaAt(ChunkPos pos, int x, int y, int z, int height, int radius) {
        int minX = pos.x() * ChunkPos.SIZE;
        int minZ = pos.z() * ChunkPos.SIZE;
        return x - radius >= minX
                && x + radius < minX + ChunkPos.SIZE
                && z - radius >= minZ
                && z + radius < minZ + ChunkPos.SIZE
                && y + height < DimensionSettings.OVERWORLD.maxYExclusive();
    }

    private void placeTree(Chunk chunk, int x, int y, int z) {
        for (int dy = 0; dy < 5; dy++) {
            chunk.setBlockId(x, y + dy, z, Blocks.SKYROOT_LOG);
        }
        for (int dy = 3; dy <= 6; dy++) {
            int radius = dy == 6 ? 1 : 2;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                        chunk.setBlockId(x + dx, y + dy, z + dz, Blocks.SKYROOT_LEAVES);
                    }
                }
            }
        }
    }

    private void placePineTree(Chunk chunk, int x, int y, int z) {
        for (int dy = 0; dy < 6; dy++) {
            chunk.setBlockId(x, y + dy, z, Blocks.PINE_LOG);
        }
        for (int dy = 2; dy <= 7; dy++) {
            int radius = Math.max(1, 4 - (dy - 2) / 2);
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1 && !(dx == 0 && dz == 0 && dy < 6)) {
                        chunk.setBlockId(x + dx, y + dy, z + dz, Blocks.PINE_LEAVES);
                    }
                }
            }
        }
    }

    private boolean canPlaceArea(Chunk chunk, int x, int y, int z, int height, int radius) {
        return canPlaceAreaAt(chunk.pos(), x, y, z, height, radius)
                && y + height < chunk.dimension().maxYExclusive();
    }

    private void placeCactus(Chunk chunk, int x, int y, int z, int height) {
        for (int dy = 0; dy < height; dy++) {
            chunk.setBlockId(x, y + dy, z, Blocks.CACTUS);
        }
    }

    private void placeBoulder(Chunk chunk, int x, int y, int z) {
        for (int dy = 0; dy <= 2; dy++) {
            int radius = dy == 1 ? 2 : 1;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) + dy <= 3) {
                        chunk.setBlockId(x + dx, y + dy, z + dz, Blocks.MOSSY_STONE);
                    }
                }
            }
        }
    }

    static short detailResourceFor(String biomeKey, double roll) {
        return BiomeResourceProfiles.detailResourceFor(biomeKey, roll);
    }

    private short oreOrStone(int x, int y, int z) {
        double ore = normalize(ValueNoise.hashUnit(seed ^ 0xC0A1, x * 31 + y, z * 17 - y));
        double vein = normalize(ValueNoise.fbm(seed ^ 0x0EE5L, x + y * 2.0, z - y * 1.5, 3, 0.055, 0.55));
        if (y < 18 && vein > 0.88 && ore >= 0.245 && ore < 0.255) {
            return Blocks.PLATIN_ORE;
        }
        if (y < 26 && vein > 0.84 && ore >= 0.255 && ore < 0.267) {
            return Blocks.TITAN_ORE;
        }
        if (y < 42 && vein > 0.82 && ore >= 0.267 && ore < 0.280) {
            return Blocks.RUBY_ORE;
        }
        if (y < 48 && vein > 0.82 && ore >= 0.280 && ore < 0.293) {
            return Blocks.SAPPHIRE_ORE;
        }
        if (y < 54 && vein > 0.80 && ore >= 0.293 && ore < 0.312) {
            return Blocks.GOLD_ORE;
        }
        if (y < 28 && vein > 0.82 && ore < 0.055) {
            return Blocks.IRON_ORE;
        }
        if (y < 38 && vein > 0.86 && ore >= 0.22 && ore < 0.245) {
            return Blocks.GLOW_CRYSTAL_NODE;
        }
        if (y < 68 && vein > 0.76 && ore >= 0.055 && ore < 0.12) {
            return Blocks.COPPER_ORE;
        }
        if (y < 110 && vein > 0.70 && ore >= 0.12 && ore < 0.22) {
            return Blocks.COAL_ORE;
        }
        if (y < 32 && ore < 0.008) {
            return Blocks.IRON_ORE;
        }
        if (y < 24 && ore >= 0.034 && ore < 0.038) {
            return Blocks.PLATIN_ORE;
        }
        if (y < 36 && ore >= 0.038 && ore < 0.043) {
            return Blocks.TITAN_ORE;
        }
        if (y < 48 && ore >= 0.043 && ore < 0.049) {
            return Blocks.RUBY_ORE;
        }
        if (y < 56 && ore >= 0.049 && ore < 0.055) {
            return Blocks.SAPPHIRE_ORE;
        }
        if (y < 64 && ore >= 0.055 && ore < 0.062) {
            return Blocks.GOLD_ORE;
        }
        if (y < 72 && ore >= 0.008 && ore < 0.018) {
            return Blocks.COPPER_ORE;
        }
        if (y < 112 && ore >= 0.018 && ore < 0.034) {
            return Blocks.COAL_ORE;
        }
        return Blocks.STONE;
    }

    private short surfaceBlockFor(int x, int z, int height, BiomeType biome) {
        double river = riverStrength(x, z);
        if (river > 0.45) {
            return river > 0.72 ? Blocks.CLAY : Blocks.GRAVEL;
        }
        if ("voxel:frost_peaks".equals(biome.key()) && height > SEA_LEVEL + 5) {
            return height > SEA_LEVEL + 18 ? Blocks.SNOW : Blocks.SNOWY_GRASS;
        }
        if ("voxel:sun_dunes".equals(biome.key()) && normalize(ValueNoise.hashUnit(seed ^ 0x5A11D00DL, x, z)) > 0.68) {
            return Blocks.RED_SAND;
        }
        if ("voxel:mire".equals(biome.key()) && height <= SEA_LEVEL + 3) {
            return Blocks.CLAY;
        }
        if (height <= SEA_LEVEL + 1) {
            return Blocks.SAND;
        }
        return biome.surfaceBlock();
    }

    private short subsurfaceBlockFor(int x, int z, int height, BiomeType biome) {
        if (riverStrength(x, z) > 0.55) {
            return Blocks.CLAY;
        }
        if ("voxel:frost_peaks".equals(biome.key()) && height > SEA_LEVEL + 5) {
            return Blocks.GRAVEL;
        }
        if (height <= SEA_LEVEL + 1) {
            return Blocks.SAND;
        }
        return biome.subsurfaceBlock();
    }

    private short frozenWaterFor(BiomeType biome, int y) {
        if ("voxel:frost_peaks".equals(biome.key()) && y == SEA_LEVEL) {
            return Blocks.ICE;
        }
        return Blocks.WATER;
    }

    private double riverStrength(int x, int z) {
        double river = Math.abs(ValueNoise.fbm(seed ^ 0x4A7E1L, x, z, 4, 0.0035, 0.52));
        double width = 0.075;
        if (river >= width) {
            return 0.0;
        }
        double t = 1.0 - river / width;
        return t * t * (3.0 - 2.0 * t);
    }

    private boolean isCave(int x, int y, int z) {
        double horizontal = ValueNoise.fbm(seed ^ 0xCA7EL, x, z, 3, 0.025, 0.5);
        double vertical = Math.sin((y + seed * 0.001) * 0.11);
        return horizontal + vertical * 0.35 > 0.82;
    }

    private boolean hasCaveColumn(int x, int z, int height) {
        int maxY = Math.min(height - 5, DimensionSettings.OVERWORLD.maxYExclusive() - 1);
        for (int y = DimensionSettings.OVERWORLD.minY(); y <= maxY; y++) {
            if (isCave(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }

    private ClimateSample climateAt(int x, int z) {
        double temperature = rawTemperature(x, z) * 8.0;
        double moisture = rawMoisture(x, z) * 8.0;
        double weight = 8.0;
        int[][] offsets = {
                {-BIOME_BLEND_SAMPLE_DISTANCE, 0},
                {BIOME_BLEND_SAMPLE_DISTANCE, 0},
                {0, -BIOME_BLEND_SAMPLE_DISTANCE},
                {0, BIOME_BLEND_SAMPLE_DISTANCE}
        };
        for (int[] offset : offsets) {
            temperature += rawTemperature(x + offset[0], z + offset[1]);
            moisture += rawMoisture(x + offset[0], z + offset[1]);
            weight += 1.0;
        }
        return new ClimateSample(temperature / weight, moisture / weight);
    }

    private double rawTemperature(int x, int z) {
        return normalize(ValueNoise.fbm(seed ^ 0xCAFE, x, z, 3, 0.0025, 0.55));
    }

    private double rawMoisture(int x, int z) {
        return normalize(ValueNoise.fbm(seed ^ 0xBEEF, x, z, 3, 0.0028, 0.55));
    }

    private double blendedBiomeHeightAdjustment(int x, int z, BiomeType biome, int base, double mountain) {
        double currentWeight = 10.0;
        double adjustment = biomeHeightAdjustment(x, z, biome, base, mountain) * currentWeight;
        double weight = currentWeight;
        int[][] offsets = {
                {-BIOME_BLEND_SAMPLE_DISTANCE, 0},
                {BIOME_BLEND_SAMPLE_DISTANCE, 0},
                {0, -BIOME_BLEND_SAMPLE_DISTANCE},
                {0, BIOME_BLEND_SAMPLE_DISTANCE},
                {-BIOME_BLEND_SAMPLE_DISTANCE, -BIOME_BLEND_SAMPLE_DISTANCE},
                {BIOME_BLEND_SAMPLE_DISTANCE, -BIOME_BLEND_SAMPLE_DISTANCE},
                {-BIOME_BLEND_SAMPLE_DISTANCE, BIOME_BLEND_SAMPLE_DISTANCE},
                {BIOME_BLEND_SAMPLE_DISTANCE, BIOME_BLEND_SAMPLE_DISTANCE}
        };
        for (int[] offset : offsets) {
            BiomeType neighbor = biomeAt(x + offset[0], z + offset[1]);
            adjustment += biomeHeightAdjustment(x, z, neighbor, base, mountain);
            weight += 1.0;
        }
        return adjustment / weight;
    }

    private double biomeHeightAdjustment(int x, int z, BiomeType biome, int base, double mountain) {
        return switch (biome.key()) {
            case "voxel:sun_dunes" -> -9.0 + Math.round(ValueNoise.fbm(seed ^ 0xD0A35L, x, z, 3, 0.026, 0.48) * 7.0);
            case "voxel:highlands" -> 24.0;
            case "voxel:frost_peaks" -> 42.0 + Math.round(mountain * 0.55);
            case "voxel:mire" -> Math.round((base - 6) * 0.82 + (SEA_LEVEL + 2) * 0.18) - base;
            case "voxel:lakeside" -> Math.round((base - 4) * 0.70 + (SEA_LEVEL + 3) * 0.30) - base;
            case "voxel:mushroom_grove" -> -2.0;
            case "voxel:flower_fields" -> -3.0;
            case "voxel:old_ruins" -> 5.0;
            default -> 0.0;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record BiomeTransition(String biomeKey, int differingSamples, int totalSamples) {
        public BiomeTransition {
            Objects.requireNonNull(biomeKey, "biomeKey");
            if (differingSamples < 0 || totalSamples <= 0 || differingSamples > totalSamples) {
                throw new IllegalArgumentException("invalid biome transition sample counts");
            }
        }

        public double edgeFactor() {
            return differingSamples / (double) totalSamples;
        }

        public boolean boundary() {
            return differingSamples > 0;
        }
    }

    private record ClimateSample(double temperature, double moisture) {
    }
}
