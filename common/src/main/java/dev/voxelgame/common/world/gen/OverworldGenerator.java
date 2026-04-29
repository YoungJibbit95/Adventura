package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.structure.Structures;

public final class OverworldGenerator implements WorldGenerator {
    private static final int SEA_LEVEL = 63;

    private final long seed;
    private final Registry<BiomeType> biomes;

    public OverworldGenerator(long seed) {
        this(seed, Biomes.createDefaultRegistry());
    }

    public OverworldGenerator(long seed, Registry<BiomeType> biomes) {
        this.seed = seed;
        this.biomes = biomes;
    }

    @Override
    public void generate(Chunk chunk) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;

        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int x = baseX + localX;
                int z = baseZ + localZ;
                BiomeType biome = biomeAt(x, z);
                int height = terrainHeight(x, z, biome);
                fillColumn(chunk, x, z, height, biome);
                decorateColumn(chunk, x, z, height, biome);
            }
        }
        decorateChunkStructures(chunk);
    }

    public BiomeType biomeAt(int x, int z) {
        double temperature = normalize(ValueNoise.fbm(seed ^ 0xCAFE, x, z, 3, 0.0025, 0.55));
        double moisture = normalize(ValueNoise.fbm(seed ^ 0xBEEF, x, z, 3, 0.0028, 0.55));
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

    public int terrainHeight(int x, int z, BiomeType biome) {
        double continents = ValueNoise.fbm(seed, x, z, 5, 0.0016, 0.5);
        double erosion = normalize(ValueNoise.fbm(seed ^ 0xE70510L, x, z, 4, 0.0032, 0.5));
        double hills = ValueNoise.fbm(seed ^ 0x1234ABCDL, x, z, 4, 0.009, 0.48);
        double detail = ValueNoise.fbm(seed ^ 0xD37A11L, x, z, 2, 0.035, 0.45);
        double ridge = Math.abs(ValueNoise.fbm(seed ^ 0xA77A11L, x, z, 4, 0.005, 0.5));
        double mountain = Math.pow(1.0 - ridge, 2.4) * 54.0 * (1.0 - erosion * 0.65);
        int base = 70 + (int) Math.round(continents * 36.0 + hills * 14.0 + detail * 3.0 + mountain);
        if ("voxel:sun_dunes".equals(biome.key())) {
            double dunes = ValueNoise.fbm(seed ^ 0xD0A35L, x, z, 3, 0.026, 0.48);
            base -= 9;
            base += (int) Math.round(dunes * 7.0);
        } else if ("voxel:highlands".equals(biome.key())) {
            base += 24;
        } else if ("voxel:frost_peaks".equals(biome.key())) {
            base += 42;
            base += (int) Math.round(mountain * 0.55);
        } else if ("voxel:mire".equals(biome.key())) {
            base -= 6;
            base = (int) Math.round(base * 0.82 + (SEA_LEVEL + 2) * 0.18);
        } else if ("voxel:lakeside".equals(biome.key())) {
            base -= 4;
            base = (int) Math.round(base * 0.70 + (SEA_LEVEL + 3) * 0.30);
        } else if ("voxel:mushroom_grove".equals(biome.key())) {
            base -= 2;
        } else if ("voxel:flower_fields".equals(biome.key())) {
            base -= 3;
        } else if ("voxel:old_ruins".equals(biome.key())) {
            base += 5;
        }
        double river = riverStrength(x, z);
        if (river > 0.0) {
            int riverBed = SEA_LEVEL - 4 + (int) Math.round(ValueNoise.smooth(seed ^ 0xA11EL, x * 0.04, z * 0.04) * 2.0);
            base = (int) Math.round(base * (1.0 - river) + riverBed * river);
        }
        return clamp(base, 28, 235);
    }

    private void fillColumn(Chunk chunk, int x, int z, int height, BiomeType biome) {
        for (int y = chunk.dimension().minY(); y < chunk.dimension().maxYExclusive(); y++) {
            short block = Blocks.AIR;
            if (y <= height) {
                if (isCave(x, y, z) && y < height - 4) {
                    block = Blocks.AIR;
                } else if (y == height) {
                    block = surfaceBlockFor(x, z, height, biome);
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

    private void decorateColumn(Chunk chunk, int x, int z, int height, BiomeType biome) {
        if (height + 7 >= chunk.dimension().maxYExclusive() || height < SEA_LEVEL - 3) {
            return;
        }

        double chance = normalize(ValueNoise.hashUnit(seed ^ 0x51A7, x, z));
        if (("voxel:frost_peaks".equals(biome.key()) || "voxel:pine_forest".equals(biome.key())) && chance < biome.treeChance() && canPlaceTree(chunk, x, height + 1, z)) {
            placePineTree(chunk, x, height + 1, z);
            return;
        }

        if (chance < biome.treeChance() && canPlaceTree(chunk, x, height + 1, z)) {
            placeTree(chunk, x, height + 1, z);
            return;
        }

        double plantChance = normalize(ValueNoise.hashUnit(seed ^ 0x61B7, x, z));
        if (plantChance < biome.plantChance()) {
            short plant = plantChance < biome.plantChance() * 0.15 ? Blocks.SUN_BLOOM : Blocks.WILD_GRASS;
            if (("voxel:mire".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key())) && plantChance < biome.plantChance() * 0.55) {
                plant = Blocks.RED_MUSHROOM;
            } else if ("voxel:flower_fields".equals(biome.key()) && plantChance < biome.plantChance() * 0.60) {
                plant = Blocks.SUN_BLOOM;
            } else if ("voxel:lakeside".equals(biome.key()) && plantChance < biome.plantChance() * 0.35) {
                plant = Blocks.BERRY_BUSH;
            }
            chunk.setBlockId(x, height + 1, z, plant);
        }

        double detailChance = normalize(ValueNoise.hashUnit(seed ^ 0xC07ED11L, x, z));
        if (detailChance < 0.009 && canPlaceArea(chunk, x, height + 1, z, 1, 0)) {
            chunk.setBlockId(x, height + 1, z, Blocks.SMALL_STONE);
        } else if (detailChance >= 0.009 && detailChance < 0.014 && canPlaceArea(chunk, x, height + 1, z, 1, 0)) {
            chunk.setBlockId(x, height + 1, z, Blocks.BERRY_BUSH);
        } else if (detailChance >= 0.014 && detailChance < 0.018 && canPlaceArea(chunk, x, height + 1, z, 1, 0)) {
            chunk.setBlockId(x, height + 1, z, Blocks.HERB_PLANTER);
        }

        if ("voxel:sun_dunes".equals(biome.key())) {
            double cactusChance = normalize(ValueNoise.hashUnit(seed ^ 0xCA77L, x, z));
            if (cactusChance < 0.012 && canPlaceArea(chunk, x, height + 1, z, 4, 0)) {
                placeCactus(chunk, x, height + 1, z, 2 + (int) Math.floor(cactusChance * 180.0));
            }
        } else if ("voxel:highlands".equals(biome.key()) || "voxel:old_ruins".equals(biome.key())) {
            double boulderChance = normalize(ValueNoise.hashUnit(seed ^ 0xB011L, x, z));
            if (boulderChance < 0.008 && canPlaceArea(chunk, x, height + 1, z, 3, 2)) {
                placeBoulder(chunk, x, height + 1, z);
            }
        } else if ("voxel:mire".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key())) {
            double stumpChance = normalize(ValueNoise.hashUnit(seed ^ 0x57ADEL, x, z));
            if (stumpChance < 0.010 && canPlaceArea(chunk, x, height + 1, z, 2, 1)) {
                chunk.setBlockId(x, height + 1, z, Blocks.TREE_STUMP);
                chunk.setBlockId(x, height + 2, z, Blocks.RED_MUSHROOM);
            }
        }
    }

    private void decorateChunkStructures(Chunk chunk) {
        int centerX = chunk.pos().x() * ChunkPos.SIZE + 8;
        int centerZ = chunk.pos().z() * ChunkPos.SIZE + 8;
        BiomeType biome = biomeAt(centerX, centerZ);
        if (chunk.pos().x() == 1 && chunk.pos().z() == 1) {
            int groundY = terrainHeight(centerX, centerZ, biome) + 1;
            Structures.compactVillage().placeIntoChunk(chunk, centerX, groundY, centerZ);
            return;
        }
        double roll = normalize(ValueNoise.hashUnit(seed ^ 0x57711A6EL, chunk.pos().x(), chunk.pos().z()));
        double villageRoll = normalize(ValueNoise.hashUnit(seed ^ 0xA911A6EL, chunk.pos().x(), chunk.pos().z()));
        if (("voxel:meadow".equals(biome.key()) || "voxel:cozy_meadow".equals(biome.key()) || "voxel:flower_fields".equals(biome.key()) || "voxel:skyroot_forest".equals(biome.key())) && villageRoll < 0.014) {
            int groundY = terrainHeight(centerX, centerZ, biome) + 1;
            Structures.compactVillage().placeIntoChunk(chunk, centerX, groundY, centerZ);
            return;
        }
        if (roll > biome.structureChance()) {
            return;
        }
        int groundY = terrainHeight(centerX, centerZ, biome) + 1;
        if ("voxel:sun_dunes".equals(biome.key())) {
            Structures.desertWell().placeIntoChunk(chunk, centerX, groundY, centerZ);
        } else if ("voxel:meadow".equals(biome.key()) || "voxel:cozy_meadow".equals(biome.key()) || "voxel:flower_fields".equals(biome.key()) || "voxel:lakeside".equals(biome.key()) || "voxel:skyroot_forest".equals(biome.key())) {
            if (roll < biome.structureChance() * 0.35) {
                Structures.campsite().placeIntoChunk(chunk, centerX, groundY, centerZ);
            } else {
                Structures.simpleHouse().placeIntoChunk(chunk, centerX, groundY, centerZ);
            }
        } else if ("voxel:frost_peaks".equals(biome.key())) {
            Structures.smallRuin().placeIntoChunk(chunk, centerX, groundY, centerZ);
        } else if ("voxel:highlands".equals(biome.key()) || "voxel:old_ruins".equals(biome.key())) {
            Structures.watchtower().placeIntoChunk(chunk, centerX, groundY, centerZ);
        } else if ("voxel:mushroom_grove".equals(biome.key())) {
            Structures.smallRuin().placeIntoChunk(chunk, centerX, groundY, centerZ);
        } else {
            Structures.smallRuin().placeIntoChunk(chunk, centerX, groundY, centerZ);
        }
    }

    private boolean canPlaceTree(Chunk chunk, int x, int y, int z) {
        return ChunkPos.fromBlock(x, z).equals(chunk.pos())
                && x > chunk.pos().x() * ChunkPos.SIZE + 2
                && z > chunk.pos().z() * ChunkPos.SIZE + 2
                && x < chunk.pos().x() * ChunkPos.SIZE + 13
                && z < chunk.pos().z() * ChunkPos.SIZE + 13
                && y + 7 < chunk.dimension().maxYExclusive();
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
        int minX = chunk.pos().x() * ChunkPos.SIZE;
        int minZ = chunk.pos().z() * ChunkPos.SIZE;
        return x - radius >= minX
                && x + radius < minX + ChunkPos.SIZE
                && z - radius >= minZ
                && z + radius < minZ + ChunkPos.SIZE
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

    private short oreOrStone(int x, int y, int z) {
        double ore = normalize(ValueNoise.hashUnit(seed ^ 0xC0A1, x * 31 + y, z * 17 - y));
        double vein = normalize(ValueNoise.fbm(seed ^ 0x0EE5L, x + y * 2.0, z - y * 1.5, 3, 0.055, 0.55));
        if (y < 28 && vein > 0.82 && ore < 0.055) {
            return Blocks.IRON_ORE;
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
            return Blocks.SNOW;
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

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
