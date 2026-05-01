package dev.voxelgame.common.world.structure;

import dev.voxelgame.common.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public final class Structures {
    private Structures() {
    }

    public static StructureTemplate smallRuin() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                blocks.add(new BlockPlacement(x, 0, z, Blocks.MOSSY_STONE));
            }
        }
        for (int y = 1; y <= 3; y++) {
            blocks.add(new BlockPlacement(-2, y, -2, Blocks.MOSSY_STONE));
            blocks.add(new BlockPlacement(2, y, -2, Blocks.STONE));
            blocks.add(new BlockPlacement(-2, y, 2, Blocks.STONE));
            if (y <= 2) {
                blocks.add(new BlockPlacement(2, y, 2, Blocks.MOSSY_STONE));
            }
        }
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.STORAGE_CRATE));
        blocks.add(new BlockPlacement(-1, 1, 0, Blocks.STORAGE_CRATE));
        blocks.add(new BlockPlacement(1, 1, 0, Blocks.LANTERN));
        return new StructureTemplate(
                "voxel:small_ruin",
                blocks,
                List.of(
                        StructureMarker.loot("voxel:ruin_crate", 0, 1, 0),
                        StructureMarker.loot("voxel:ruin_rare_crate", -1, 1, 0)
                )
        );
    }

    public static StructureTemplate simpleHouse() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -3; z <= 3; z++) {
            for (int x = -3; x <= 3; x++) {
                blocks.add(new BlockPlacement(x, 0, z, Blocks.MOSSY_STONE));
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int z = -3; z <= 3; z++) {
                for (int x = -3; x <= 3; x++) {
                    boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                    boolean doorway = z == -3 && x == 0 && y <= 2;
                    if (wall && !doorway) {
                        blocks.add(new BlockPlacement(x, y, z, corner(x, z) ? Blocks.SKYROOT_LOG : Blocks.SKYROOT_PLANKS));
                    }
                }
            }
        }
        for (int z = -4; z <= 4; z++) {
            for (int x = -4; x <= 4; x++) {
                if (Math.abs(x) + Math.abs(z) < 7) {
                    blocks.add(new BlockPlacement(x, 4, z, Blocks.SKYROOT_LEAVES));
                }
            }
        }
        blocks.add(new BlockPlacement(0, 2, 2, Blocks.TORCH));
        blocks.add(new BlockPlacement(-1, 1, 1, Blocks.SMALL_TABLE));
        blocks.add(new BlockPlacement(1, 1, 1, Blocks.WOODEN_CHAIR));
        blocks.add(new BlockPlacement(2, 1, -1, Blocks.FLOWER_POT));
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.WOVEN_RUG));
        return new StructureTemplate("voxel:simple_house", blocks);
    }

    public static StructureTemplate desertWell() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                blocks.add(new BlockPlacement(x, 0, z, Blocks.SAND));
            }
        }
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                blocks.add(new BlockPlacement(x, 1, z, Blocks.STONE));
            }
        }
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.WATER));
        for (int y = 2; y <= 4; y++) {
            blocks.add(new BlockPlacement(-1, y, -1, Blocks.STONE));
            blocks.add(new BlockPlacement(1, y, -1, Blocks.STONE));
            blocks.add(new BlockPlacement(-1, y, 1, Blocks.STONE));
            blocks.add(new BlockPlacement(1, y, 1, Blocks.STONE));
        }
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    blocks.add(new BlockPlacement(x, 5, z, Blocks.STONE));
                }
            }
        }
        return new StructureTemplate("voxel:desert_well", blocks);
    }

    public static StructureTemplate watchtower() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int y = 0; y <= 7; y++) {
            blocks.add(new BlockPlacement(-2, y, -2, Blocks.SKYROOT_LOG));
            blocks.add(new BlockPlacement(2, y, -2, Blocks.SKYROOT_LOG));
            blocks.add(new BlockPlacement(-2, y, 2, Blocks.SKYROOT_LOG));
            blocks.add(new BlockPlacement(2, y, 2, Blocks.SKYROOT_LOG));
        }
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                blocks.add(new BlockPlacement(x, 4, z, Blocks.SKYROOT_PLANKS));
                blocks.add(new BlockPlacement(x, 8, z, Blocks.SKYROOT_LEAVES));
            }
        }
        for (int z = -3; z <= 3; z++) {
            for (int x = -3; x <= 3; x++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    blocks.add(new BlockPlacement(x, 5, z, Blocks.SKYROOT_PLANKS));
                }
            }
        }
        blocks.add(new BlockPlacement(0, 5, 0, Blocks.TORCH));
        return new StructureTemplate("voxel:watchtower", blocks);
    }

    public static StructureTemplate campsite() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -3; z <= 3; z++) {
            for (int x = -3; x <= 3; x++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    blocks.add(new BlockPlacement(x, 0, z, Blocks.GRAVEL));
                }
            }
        }
        blocks.add(new BlockPlacement(0, 0, 0, Blocks.MOSSY_PATH));
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.CAMPFIRE));
        blocks.add(new BlockPlacement(1, 1, 0, Blocks.LANTERN));
        blocks.add(new BlockPlacement(-1, 1, 0, Blocks.COOKING_POT));
        blocks.add(new BlockPlacement(-2, 1, -1, Blocks.SKYROOT_LOG));
        blocks.add(new BlockPlacement(-2, 1, 0, Blocks.SKYROOT_LOG));
        blocks.add(new BlockPlacement(2, 1, 1, Blocks.PINE_LOG));
        blocks.add(new BlockPlacement(2, 1, 0, Blocks.PINE_LOG));
        blocks.add(new BlockPlacement(0, 1, 2, Blocks.STORAGE_CRATE));
        return new StructureTemplate(
                "voxel:campsite",
                blocks,
                List.of(
                        StructureMarker.metadata("voxel:campfire_anchor", 0, 1, 0),
                        StructureMarker.loot("voxel:campsite_crate", 0, 1, 2)
                )
        );
    }

    public static StructureTemplate mushroomCircle() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -4; z <= 4; z++) {
            for (int x = -4; x <= 4; x++) {
                int distance = x * x + z * z;
                if (distance >= 9 && distance <= 17) {
                    blocks.add(new BlockPlacement(x, 0, z, Blocks.MOSSY_PATH));
                }
            }
        }
        blocks.add(new BlockPlacement(0, 0, 0, Blocks.MOSSY_STONE));
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.GLOW_CRYSTAL_NODE));

        blocks.add(new BlockPlacement(0, 1, -4, Blocks.GLOW_MUSHROOM));
        blocks.add(new BlockPlacement(4, 1, 0, Blocks.GLOW_MUSHROOM));
        blocks.add(new BlockPlacement(0, 1, 4, Blocks.GLOW_MUSHROOM));
        blocks.add(new BlockPlacement(-4, 1, 0, Blocks.GLOW_MUSHROOM));

        blocks.add(new BlockPlacement(-3, 1, -2, Blocks.SPORE_BLOSSOM));
        blocks.add(new BlockPlacement(3, 1, -2, Blocks.SPORE_BLOSSOM));
        blocks.add(new BlockPlacement(-3, 1, 2, Blocks.SPORE_BLOSSOM));
        blocks.add(new BlockPlacement(3, 1, 2, Blocks.SPORE_BLOSSOM));

        blocks.add(new BlockPlacement(-2, 1, -3, Blocks.MUSHROOM_CLUSTER));
        blocks.add(new BlockPlacement(2, 1, -3, Blocks.RED_MUSHROOM));
        blocks.add(new BlockPlacement(-2, 1, 3, Blocks.RED_MUSHROOM));
        blocks.add(new BlockPlacement(2, 1, 3, Blocks.MUSHROOM_CLUSTER));
        return new StructureTemplate(
                "voxel:mushroom_circle",
                blocks,
                List.of(StructureMarker.metadata("voxel:mushroom_circle_center", 0, 1, 0))
        );
    }

    public static StructureTemplate compactVillage() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int z = -7; z <= 7; z++) {
            blocks.add(new BlockPlacement(0, 0, z, Blocks.MOSSY_PATH));
            if (z % 3 == 0) {
                blocks.add(new BlockPlacement(-1, 0, z, Blocks.MOSSY_PATH));
                blocks.add(new BlockPlacement(1, 0, z, Blocks.MOSSY_PATH));
            }
        }
        for (int x = -7; x <= 7; x++) {
            blocks.add(new BlockPlacement(x, 0, 0, Blocks.MOSSY_PATH));
        }
        addTinyHouse(blocks, -5, 1, -5);
        addTinyHouse(blocks, 5, 1, -5);
        addTinyHouse(blocks, -5, 1, 5);
        addMarketStall(blocks, 5, 1, 5);
        blocks.add(new BlockPlacement(5, 1, 5, Blocks.SKYROOT_LOG));
        blocks.add(new BlockPlacement(5, 2, 5, Blocks.LANTERN));
        blocks.add(new BlockPlacement(0, 1, 0, Blocks.WATER));
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                    blocks.add(new BlockPlacement(x, 1, z, Blocks.MOSSY_STONE));
                }
            }
        }
        blocks.add(new BlockPlacement(-2, 1, 2, Blocks.BERRY_BUSH));
        blocks.add(new BlockPlacement(2, 1, -2, Blocks.HERB_PLANTER));
        return new StructureTemplate(
                "voxel:compact_village",
                blocks,
                List.of(
                        StructureMarker.metadata("voxel:village_center", 0, 1, 0),
                        StructureMarker.metadata("voxel:market_anchor", 5, 1, 5),
                        StructureMarker.entity("voxel:villager_spawn", -5, 1, -5),
                        StructureMarker.entity("voxel:villager_spawn", 5, 1, -5),
                        StructureMarker.loot("voxel:village_house_crate", -4, 1, -6),
                        StructureMarker.loot("voxel:village_house_crate", 6, 1, -6),
                        StructureMarker.loot("voxel:village_house_crate", -4, 1, 4)
                )
        );
    }

    private static void addTinyHouse(List<BlockPlacement> blocks, int originX, int originY, int originZ) {
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                blocks.add(new BlockPlacement(originX + x, originY - 1, originZ + z, Blocks.MOSSY_STONE));
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
                for (int x = -2; x <= 2; x++) {
                    boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                    boolean door = z == 2 && x == 0 && y <= 1;
                    if (wall && !door) {
                        blocks.add(new BlockPlacement(originX + x, originY + y, originZ + z, corner(x, z) ? Blocks.SKYROOT_LOG : Blocks.SKYROOT_PLANKS));
                    }
                }
            }
        }
        for (int z = -3; z <= 3; z++) {
            for (int x = -3; x <= 3; x++) {
                if (Math.abs(x) + Math.abs(z) <= 5) {
                    blocks.add(new BlockPlacement(originX + x, originY + 3, originZ + z, Blocks.SKYROOT_LEAVES));
                }
            }
        }
        blocks.add(new BlockPlacement(originX, originY + 1, originZ, Blocks.TORCH));
        blocks.add(new BlockPlacement(originX - 1, originY, originZ, Blocks.WOVEN_RUG));
        blocks.add(new BlockPlacement(originX + 1, originY, originZ - 1, Blocks.STORAGE_CRATE));
    }

    private static void addMarketStall(List<BlockPlacement> blocks, int originX, int originY, int originZ) {
        for (int x = -2; x <= 2; x++) {
            blocks.add(new BlockPlacement(originX + x, originY, originZ, Blocks.SKYROOT_PLANKS));
        }
        for (int y = 1; y <= 3; y++) {
            blocks.add(new BlockPlacement(originX - 2, originY + y, originZ - 1, Blocks.SKYROOT_LOG));
            blocks.add(new BlockPlacement(originX + 2, originY + y, originZ - 1, Blocks.SKYROOT_LOG));
        }
        for (int z = -2; z <= 1; z++) {
            for (int x = -3; x <= 3; x++) {
                if (Math.abs(x) + Math.abs(z) < 5) {
                    blocks.add(new BlockPlacement(originX + x, originY + 4, originZ + z, Blocks.SKYROOT_LEAVES));
                }
            }
        }
        blocks.add(new BlockPlacement(originX, originY + 1, originZ, Blocks.SMALL_TABLE));
        blocks.add(new BlockPlacement(originX - 1, originY + 1, originZ, Blocks.FLOWER_POT));
    }

    private static boolean corner(int x, int z) {
        return Math.abs(x) == 3 && Math.abs(z) == 3;
    }
}
