package dev.voxelgame.common.gameplay;

public enum AlphaMilestoneKey {
    SPAWN_SECURED("voxel:spawn_secured"),
    FIRST_SUPPLY("voxel:first_supply"),
    FIRST_FOOD("voxel:first_food"),
    FIRST_RECIPE("voxel:first_recipe"),
    FIRST_TOOL("voxel:first_tool"),
    CAMPFIRE_CRAFTED("voxel:campfire_crafted"),
    CAMPFIRE_LIT("voxel:campfire_lit"),
    STORAGE_READY("voxel:storage_ready"),
    WORKBENCH_READY("voxel:workbench_ready"),
    FIRST_COMFORT("voxel:first_comfort"),
    COOKING_POT_READY("voxel:cooking_pot_ready"),
    FORGE_READY("voxel:forge_ready"),
    FIRST_RUIN_DISCOVERED("voxel:first_ruin_discovered"),
    FIRST_RARE_FIND("voxel:first_rare_find");

    private final String key;

    AlphaMilestoneKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
