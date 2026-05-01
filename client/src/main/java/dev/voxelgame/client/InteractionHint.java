package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.InteractionRules;

import java.util.List;
import java.util.Locale;

public record InteractionHint(String title, String action, String detail, Tone tone, float progress, List<String> chips) {
    public InteractionHint(String title, String action, String detail, Tone tone) {
        this(title, action, detail, tone, 0.0f, List.of());
    }

    public InteractionHint {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Interaction hint title is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Interaction hint action is required");
        }
        detail = detail == null ? "" : detail;
        tone = tone == null ? Tone.NEUTRAL : tone;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        chips = chips == null ? List.of() : List.copyOf(chips.stream()
                .filter(chip -> chip != null && !chip.isBlank())
                .limit(3)
                .toList());
    }

    public static InteractionHint forBlock(BlockType block, Context context) {
        String title = label(block.key());
        if (!context.inReach()) {
            return new InteractionHint(title, "Move closer", rangeDetail(block), Tone.WARNING, 0.0f, List.of("Out of reach"));
        }
        if (context.miningProgress() > 0.0f && context.canHarvest()) {
            return new InteractionHint(title, "Mining " + percent(context.miningProgress()), miningDetail(block, context), Tone.READY, context.miningProgress(), List.of(toolChip(block), dropDetail(block)));
        }
        if (block.id() == Blocks.STORAGE_CRATE) {
            return new InteractionHint(title, "Right click open crate", "Shift-click moves stacks", Tone.READY, 0.0f, List.of("Storage", "Range OK"));
        }
        if (block.id() == Blocks.SLEEPING_MAT) {
            return new InteractionHint(title, "Right click sleep", "Uses the server clock", Tone.READY, 0.0f, List.of("Rest", "Comfort gated"));
        }
        if (context.selectedFood()) {
            return new InteractionHint(title, "Right click eat", miningDetail(block, context), Tone.READY, 0.0f, List.of("Food ready"));
        }
        if (CampfireRules.isCampfire(block.id())) {
            return campfireHint(block, context, title);
        }
        if (block.id() == Blocks.COOKING_POT) {
            return new InteractionHint(title, "Press E nearby to cook", "Needs water, bowl, herbs or berries", Tone.READY, 0.0f, List.of("Cooking Pot", "Preview recipes"));
        }
        if (block.id() == Blocks.WORKBENCH) {
            return new InteractionHint(title, "Press E nearby to craft", "Workbench recipes and iron tools", Tone.READY, 0.0f, List.of("Workbench", "Station recipes"));
        }
        if (block.id() == Blocks.FORGE) {
            return new InteractionHint(title, "Press E nearby to forge", "Ore, fuel and heat required", Tone.READY, 0.0f, List.of("Forge", "Station required"));
        }
        if (!context.selectedPlaceable()) {
            InteractionHint interactionHint = blockInteractionHint(block, title);
            if (interactionHint != null) {
                return interactionHint;
            }
        }
        if (context.gameMode() == GameMode.CREATIVE) {
            return new InteractionHint(title, "Left click break", "Creative mode", Tone.READY, 0.0f, List.of("Instant"));
        }
        if (!context.canHarvest()) {
            int requiredLevel = InteractionRules.requiredToolLevel(block);
            String tool = toolLabel(block.preferredTool());
            String action = requiredLevel > 0
                    ? "Need Level " + requiredLevel + " " + tool
                    : "Need " + articleFor(block.preferredTool()) + " " + tool;
            return new InteractionHint(title, action, "Wrong tool selected. Swap tools, then hold left", Tone.WARNING, 0.0f, List.of(requiredLevel > 0 ? "Level " + requiredLevel : "Tool required", tool));
        }
        if (context.selectedPlaceable()) {
            return new InteractionHint(title, "Right click place block", "Place on target face. Move if blocked", Tone.READY, 0.0f, List.of("Placement", "Face target"));
        }
        if (block.preferredTool() != ToolType.NONE && context.breakMultiplier() < 1.0f) {
            return new InteractionHint(title, "Use " + articleFor(block.preferredTool()) + " " + toolLabel(block.preferredTool()), "Hold left mines slowly", Tone.WARNING, 0.0f, List.of(toolChip(block), "Slow"));
        }
        return new InteractionHint(title, "Hold left to mine", dropDetail(block), Tone.NEUTRAL, 0.0f, List.of(toolChip(block)));
    }

    public static InteractionHint forEntity(String typeKey, boolean selectedFood) {
        if (ItemDropType.isTypeKey(typeKey)) {
            String itemName = ItemDropType.itemKey(typeKey).map(InteractionHint::label).orElse("Item");
            return new InteractionHint(itemName, "Walk over to pick up", "Inventory collects nearby drops", Tone.READY, 0.0f, List.of("Pickup"));
        }
        String title = label(typeKey);
        if ("voxel:player".equals(typeKey)) {
            return new InteractionHint(title, "Right click observe", "Online player", Tone.NEUTRAL, 0.0f, List.of("Player"));
        }
        if (selectedFood) {
            return new InteractionHint(title, "Right click feed", "May follow if interested", Tone.READY, 0.0f, List.of("Feed", "Food selected"));
        }
        return new InteractionHint(title, "Right click observe", entityDetail(typeKey), Tone.NEUTRAL, 0.0f, List.of(entityStateChip(typeKey)));
    }

    private static InteractionHint campfireHint(BlockType block, Context context, String title) {
        if (context.selectedFuel()) {
            String action = block.id() == Blocks.CAMPFIRE_ACTIVE ? "Right click add fuel" : "Right click light campfire";
            return new InteractionHint(title, action, "Fuel extends cooking time", Tone.READY, 0.0f, List.of("Fuel held"));
        }
        if (context.selectedPlaceable()) {
            return new InteractionHint(title, "Right click place block", "Hold fuel to light campfire", Tone.READY, 0.0f, List.of("Placement"));
        }
        if (block.id() == Blocks.CAMPFIRE_ACTIVE) {
            return new InteractionHint(title, "Press E nearby to cook", "Add fuel to keep it active", Tone.READY, 0.0f, List.of("Active", "Cook station"));
        }
        return new InteractionHint(title, "Needs fuel in hand", "Sticks, logs, coal, grass, bark, reeds", Tone.WARNING, 0.0f, List.of("No fuel"));
    }

    private static InteractionHint blockInteractionHint(BlockType block, String title) {
        return switch (block.id()) {
            case Blocks.WILD_GRASS -> new InteractionHint(title, "Right click gather dry grass", "Left click cuts fiber", Tone.READY);
            case Blocks.BERRY_BUSH -> new InteractionHint(title, "Right click harvest berries", "Left click breaks the bush", Tone.READY);
            case Blocks.HERB_PLANTER -> new InteractionHint(title, "Right click pick herbs", "Left click breaks the planter", Tone.READY);
            case Blocks.REEDS -> new InteractionHint(title, "Right click cut reeds", "Used for water containers and fuel", Tone.READY);
            case Blocks.GLOW_MUSHROOM -> new InteractionHint(title, "Right click gather cap", "Cook into glow mushroom stew", Tone.READY);
            case Blocks.SPORE_BLOSSOM -> new InteractionHint(title, "Right click pick blossom", "Brew with glow caps into spore tea", Tone.READY);
            case Blocks.PINE_LOG -> new InteractionHint(title, "Right click collect resin", "Hold left to chop log", Tone.READY);
            case Blocks.TREE_STUMP -> new InteractionHint(title, "Right click peel bark", "Used for tool handles and fuel", Tone.READY);
            default -> null;
        };
    }

    private static String miningDetail(BlockType block, Context context) {
        if (context.gameMode() == GameMode.CREATIVE) {
            return "Left click breaks instantly";
        }
        if (!context.canHarvest()) {
            return "Tool required to harvest";
        }
        if (block.preferredTool() == ToolType.NONE) {
            return "Hold left to break";
        }
        if (context.breakMultiplier() < 1.0f) {
            return "Hold left, but better tool helps";
        }
        return "Hold left to mine";
    }

    private static String rangeDetail(BlockType block) {
        if (block.id() == Blocks.STORAGE_CRATE) {
            return "Crate opens at reach range";
        }
        if (CampfireRules.isCampfire(block.id())) {
            return "Campfire can be fueled up close";
        }
        if (block.id() == Blocks.COOKING_POT) {
            return "Cooking pot works at reach range";
        }
        if (block.id() == Blocks.WORKBENCH || block.id() == Blocks.FORGE) {
            return "Crafting station works at reach range";
        }
        return "Target is out of reach";
    }

    private static String dropDetail(BlockType block) {
        String drop = block.dropItemKey();
        if (drop == null || drop.isBlank()) {
            return block.preferredTool() == ToolType.NONE ? "Breakable target" : "Best with " + toolLabel(block.preferredTool());
        }
        return "Drops " + label(drop);
    }

    private static String toolChip(BlockType block) {
        int requiredLevel = InteractionRules.requiredToolLevel(block);
        if (block.preferredTool() == ToolType.NONE) {
            return "Hand";
        }
        String tool = toolLabel(block.preferredTool());
        return requiredLevel > 0 ? "L" + requiredLevel + " " + tool : tool;
    }

    private static String entityDetail(String typeKey) {
        if ("voxel:firefly_swarm".equals(typeKey) || "voxel:mire_wisp".equals(typeKey)) {
            return "Glowing ambient creature";
        }
        return "Watch behavior and movement";
    }

    private static String entityStateChip(String typeKey) {
        if ("voxel:forest_bunny".equals(typeKey)) {
            return "Skittish";
        }
        if ("voxel:cozy_sheep".equals(typeKey)) {
            return "Friendly";
        }
        if ("voxel:little_boar".equals(typeKey)) {
            return "Wanders";
        }
        return "Observe";
    }

    private static String percent(float progress) {
        int clamped = Math.max(1, Math.min(99, Math.round(progress * 100.0f)));
        return clamped + "%";
    }

    private static String label(String key) {
        if (key == null || key.isBlank()) {
            return "Unknown";
        }
        int colon = key.indexOf(':');
        String value = colon >= 0 ? key.substring(colon + 1) : key;
        String spaced = value.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private static String articleFor(ToolType toolType) {
        return toolType == ToolType.AXE ? "an" : "a";
    }

    private static String toolLabel(ToolType toolType) {
        return switch (toolType) {
            case PICKAXE -> "pickaxe";
            case SHOVEL -> "shovel";
            case AXE -> "axe";
            case KNIFE -> "knife";
            case NONE -> "tool";
        };
    }

    public record Context(
            boolean inReach,
            boolean selectedFuel,
            boolean selectedFood,
            boolean selectedPlaceable,
            boolean canHarvest,
            float breakMultiplier,
            GameMode gameMode,
            float miningProgress
    ) {
        public Context {
            gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
            breakMultiplier = Math.max(0.0f, breakMultiplier);
            miningProgress = Math.max(0.0f, Math.min(1.0f, miningProgress));
        }
    }

    public enum Tone {
        NEUTRAL,
        READY,
        WARNING
    }
}
