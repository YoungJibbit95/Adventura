package dev.voxelgame.common.content;

public enum ContentTag {
    STARTER("starter"),
    CRAFTING_INGREDIENT("crafting_ingredient"),
    COOKING_INGREDIENT("cooking_ingredient"),
    FOOD("food"),
    RAW_FOOD("raw_food"),
    COOKED_FOOD("cooked_food"),
    HEALING("healing"),
    FUEL("fuel"),
    FLAMMABLE("flammable"),
    AMMO("ammo"),
    PLACEABLE("placeable"),
    TOOL("tool"),
    TOOL_AXE("tool_axe"),
    TOOL_PICKAXE("tool_pickaxe"),
    TOOL_SHOVEL("tool_shovel"),
    TOOL_KNIFE("tool_knife"),
    WEAPON("weapon"),
    STATION("station"),
    STORAGE("storage"),
    COMFORT_SOURCE("comfort_source"),
    LIGHT_SOURCE("light_source"),
    EMISSIVE("emissive"),
    TRANSPARENT("transparent"),
    COLD("cold"),
    HOT("hot"),
    FLOATY("floaty"),
    HEAVY("heavy"),
    RESOURCE_NODE("resource_node"),
    ORE("ore"),
    PLANT("plant"),
    WOOD("wood"),
    STONE("stone"),
    CLAY("clay"),
    METAL("metal"),
    COPPER("copper"),
    IRON("iron"),
    CRYSTAL("crystal"),
    ANCIENT("ancient"),
    RUIN_PROGRESSION("ruin_progression"),
    RARE_LOOT("rare_loot"),
    LORE("lore"),
    POTTERY("pottery"),
    CONTAINER("container"),
    WATER_CONTAINER("water_container"),
    TEXTILE("textile"),
    DECOR("decor"),
    AMBIENT("ambient"),
    COZY("cozy"),
    FRIENDLY("friendly"),
    CREATURE_RESOURCE("creature_resource"),
    NO_KILL("no_kill"),
    NIGHT_VISIBLE("night_visible"),
    ADVENTURE_DANGER("adventure_danger"),
    PROJECTILE("projectile"),
    RANGED("ranged");

    private final String key;

    ContentTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
