package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.InteractionRules;

import java.util.Locale;

public record InteractionHint(String title, String action, String detail, Tone tone) {
    public InteractionHint {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Interaction hint title is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Interaction hint action is required");
        }
        detail = detail == null ? "" : detail;
        tone = tone == null ? Tone.NEUTRAL : tone;
    }

    public static InteractionHint forBlock(BlockType block, Context context) {
        String title = label(block.key());
        if (!context.inReach()) {
            return new InteractionHint(title, "Move closer", rangeDetail(block), Tone.WARNING);
        }
        if (context.miningProgress() > 0.0f && context.canHarvest()) {
            return new InteractionHint(title, "Mining " + percent(context.miningProgress()), "Keep holding left", Tone.READY);
        }
        if (block.id() == Blocks.STORAGE_CRATE) {
            return new InteractionHint(title, "Right click open crate", "Shift-click moves stacks", Tone.READY);
        }
        if (block.id() == Blocks.SLEEPING_MAT) {
            return new InteractionHint(title, "Right click sleep", "Uses the server clock", Tone.READY);
        }
        if (context.selectedFood()) {
            return new InteractionHint(title, "Right click eat", miningDetail(block, context), Tone.READY);
        }
        if (CampfireRules.isCampfire(block.id())) {
            return campfireHint(block, context, title);
        }
        if (block.id() == Blocks.COOKING_POT) {
            return new InteractionHint(title, "Press E nearby to cook", "Soups, stew, tea, and jam", Tone.READY);
        }
        if (block.id() == Blocks.WORKBENCH) {
            return new InteractionHint(title, "Press E nearby to craft", "Workbench recipes and iron tools", Tone.READY);
        }
        if (block.id() == Blocks.FORGE) {
            return new InteractionHint(title, "Press E nearby to forge", "Iron ingots and ruin seals", Tone.READY);
        }
        if (!context.selectedPlaceable()) {
            InteractionHint interactionHint = blockInteractionHint(block, title);
            if (interactionHint != null) {
                return interactionHint;
            }
        }
        if (context.gameMode() == GameMode.CREATIVE) {
            return new InteractionHint(title, "Left click break", "Creative mode", Tone.READY);
        }
        if (!context.canHarvest()) {
            int requiredLevel = InteractionRules.requiredToolLevel(block);
            String tool = toolLabel(block.preferredTool());
            String action = requiredLevel > 0
                    ? "Need Level " + requiredLevel + " " + tool
                    : "Need " + articleFor(block.preferredTool()) + " " + tool;
            return new InteractionHint(title, action, "Swap tools, then hold left", Tone.WARNING);
        }
        if (context.selectedPlaceable()) {
            return new InteractionHint(title, "Right click place block", miningDetail(block, context), Tone.READY);
        }
        if (block.preferredTool() != ToolType.NONE && context.breakMultiplier() < 1.0f) {
            return new InteractionHint(title, "Use " + articleFor(block.preferredTool()) + " " + toolLabel(block.preferredTool()), "Hold left mines slowly", Tone.WARNING);
        }
        return new InteractionHint(title, "Hold left to mine", dropDetail(block), Tone.NEUTRAL);
    }

    public static InteractionHint forEntity(String typeKey, boolean selectedFood) {
        if (ItemDropType.isTypeKey(typeKey)) {
            String itemName = ItemDropType.itemKey(typeKey).map(InteractionHint::label).orElse("Item");
            return new InteractionHint(itemName, "Walk over to pick up", "Inventory collects nearby drops", Tone.READY);
        }
        String title = label(typeKey);
        if ("voxel:player".equals(typeKey)) {
            return new InteractionHint(title, "Right click observe", "Online player", Tone.NEUTRAL);
        }
        if (selectedFood) {
            return new InteractionHint(title, "Right click feed", "May follow if interested", Tone.READY);
        }
        return new InteractionHint(title, "Right click observe", entityDetail(typeKey), Tone.NEUTRAL);
    }

    private static InteractionHint campfireHint(BlockType block, Context context, String title) {
        if (context.selectedFuel()) {
            String action = block.id() == Blocks.CAMPFIRE_ACTIVE ? "Right click add fuel" : "Right click light campfire";
            return new InteractionHint(title, action, "Fuel extends cooking time", Tone.READY);
        }
        if (context.selectedPlaceable()) {
            return new InteractionHint(title, "Right click place block", "Hold fuel to light campfire", Tone.READY);
        }
        if (block.id() == Blocks.CAMPFIRE_ACTIVE) {
            return new InteractionHint(title, "Press E nearby to cook", "Add fuel to keep it active", Tone.READY);
        }
        return new InteractionHint(title, "Needs fuel in hand", "Sticks, logs, coal, grass, bark, reeds", Tone.WARNING);
    }

    private static InteractionHint blockInteractionHint(BlockType block, String title) {
        return switch (block.id()) {
            case Blocks.WILD_GRASS -> new InteractionHint(title, "Right click gather dry grass", "Left click cuts fiber", Tone.READY);
            case Blocks.BERRY_BUSH -> new InteractionHint(title, "Right click harvest berries", "Left click breaks the bush", Tone.READY);
            case Blocks.HERB_PLANTER -> new InteractionHint(title, "Right click pick herbs", "Left click breaks the planter", Tone.READY);
            case Blocks.REEDS -> new InteractionHint(title, "Right click cut reeds", "Used for water containers and fuel", Tone.READY);
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

    private static String entityDetail(String typeKey) {
        if ("voxel:firefly_swarm".equals(typeKey) || "voxel:mire_wisp".equals(typeKey)) {
            return "Glowing ambient creature";
        }
        return "Watch behavior and movement";
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
