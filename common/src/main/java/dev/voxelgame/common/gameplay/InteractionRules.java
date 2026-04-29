package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.registry.Registry;

import java.util.Optional;

public final class InteractionRules {
    public static final int HOTBAR_SLOTS = 9;
    public static final double BLOCK_REACH = 7.0;

    private InteractionRules() {
    }

    public static boolean isHotbarSlot(int slot, int inventorySize) {
        return slot >= 0 && slot < Math.min(HOTBAR_SLOTS, inventorySize);
    }

    public static boolean canReachBlock(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        double dx = blockX + 0.5 - eyeX;
        double dy = blockY + 0.5 - eyeY;
        double dz = blockZ + 0.5 - eyeZ;
        return dx * dx + dy * dy + dz * dz <= BLOCK_REACH * BLOCK_REACH;
    }

    public static float breakMultiplier(ItemStack selectedStack, Registry<ItemType> items, BlockType target) {
        if (selectedStack.isEmpty()) {
            return target.preferredTool() == ToolType.NONE ? 1.0f : 0.65f;
        }
        ItemType item = items.requireById(selectedStack.itemId());
        if (item.toolType() == target.preferredTool() && item.isTool()) {
            return switch (item.toolType()) {
                case PICKAXE -> 3.0f;
                case SHOVEL -> 2.6f;
                case AXE -> 2.8f;
                case KNIFE -> 3.4f;
                case NONE -> 1.0f;
            };
        }
        if (target.preferredTool() == ToolType.NONE) {
            return item.toolType() == ToolType.KNIFE ? 1.4f : 1.0f;
        }
        return item.isTool() ? 0.85f : 0.55f;
    }

    public static int toolDamage(ItemStack selectedStack, Registry<ItemType> items, BlockType target) {
        if (selectedStack.isEmpty()) {
            return 0;
        }
        ItemType item = items.requireById(selectedStack.itemId());
        if (!item.isTool()) {
            return 0;
        }
        return item.toolType() == target.preferredTool() ? 1 : 2;
    }

    public static int dropCount(BlockType target, float breakMultiplier) {
        return target.preferredTool() == ToolType.KNIFE && breakMultiplier > 2.0f ? 2 : 1;
    }

    public static double breakDelaySeconds(BlockType target, float breakMultiplier) {
        if (target.hardness() <= 0.0f) {
            return 0.08;
        }
        double delay = target.hardness() * 0.34 / Math.max(0.35f, breakMultiplier);
        return Math.max(0.08, Math.min(0.85, delay));
    }

    public static Optional<BlockInteraction> blockInteraction(BlockType target) {
        return switch (target.id()) {
            case Blocks.BERRY_BUSH -> Optional.of(new BlockInteraction("voxel:berries", 2, 0.35, "Harvested berries"));
            case Blocks.HERB_PLANTER -> Optional.of(new BlockInteraction("voxel:wild_herbs", 1, 0.35, "Picked wild herbs"));
            default -> Optional.empty();
        };
    }

    public record BlockInteraction(String itemKey, int count, double cooldownSeconds, String message) {
        public BlockInteraction {
            if (itemKey == null || itemKey.isBlank()) {
                throw new IllegalArgumentException("Interaction item key is required");
            }
            if (count < 1) {
                throw new IllegalArgumentException("Interaction count must be positive");
            }
            if (cooldownSeconds < 0.0) {
                throw new IllegalArgumentException("Interaction cooldown must be >= 0");
            }
        }
    }
}
