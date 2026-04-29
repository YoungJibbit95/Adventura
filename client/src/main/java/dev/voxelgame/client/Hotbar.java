package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.item.StarterInventory;
import dev.voxelgame.common.registry.Registry;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public final class Hotbar {
    public static final int HOTBAR_SLOTS = 9;

    private static final int[] KEYS = {
            GLFW_KEY_1,
            GLFW_KEY_2,
            GLFW_KEY_3,
            GLFW_KEY_4,
            GLFW_KEY_5,
            GLFW_KEY_6,
            GLFW_KEY_7,
            GLFW_KEY_8,
            GLFW_KEY_9
    };

    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final Registry<BlockType> blocks = Blocks.createDefaultRegistry();
    private final Inventory inventory = new Inventory(36);
    private final List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
    private int selectedIndex;

    public synchronized void resetForNewGame() {
        StarterInventory.apply(inventory, items);
        selectedIndex = 0;
    }

    public synchronized boolean updateSelection(long window) {
        for (int i = 0; i < KEYS.length; i++) {
            if (glfwGetKey(window, KEYS[i]) == GLFW_PRESS && selectedIndex != i) {
                selectedIndex = i;
                return true;
            }
        }
        return false;
    }

    public synchronized void applySnapshot(List<ItemStack> slots) {
        inventory.replaceSlots(slots);
    }

    public synchronized Optional<Short> selectedPlaceBlockId() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ItemType item = items.requireById(stack.itemId());
        if (item.placesBlockKey() == null) {
            return Optional.empty();
        }
        return blocks.findByKey(item.placesBlockKey()).map(BlockType::id);
    }

    public synchronized boolean consumeSelectedOne() {
        return inventory.removeFromSlot(selectedIndex, 1);
    }

    public synchronized boolean useSelectedFood(PlayerStats stats) {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return false;
        }
        ItemType item = items.requireById(stack.itemId());
        if (!item.isFood() || !stats.canUseFood(item.foodValue(), item.healValue())) {
            return false;
        }
        stats.eat(item.foodValue(), item.healValue());
        inventory.removeFromSlot(selectedIndex, 1);
        return true;
    }

    public synchronized float selectedBreakMultiplier(BlockType target) {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return target.preferredTool() == ToolType.NONE ? 1.0f : 0.65f;
        }
        ItemType item = items.requireById(stack.itemId());
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

    public synchronized void damageSelectedTool(BlockType target) {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return;
        }
        ItemType item = items.requireById(stack.itemId());
        if (!item.isTool()) {
            return;
        }
        int amount = item.toolType() == target.preferredTool() ? 1 : 2;
        inventory.damageSlot(selectedIndex, amount, items);
    }

    public synchronized boolean addItem(String itemKey, int count) {
        return items.findByKey(itemKey)
                .map(item -> inventory.add(item.id(), count, items) == 0)
                .orElse(false);
    }

    public synchronized boolean craft(CraftingRecipe recipe) {
        return recipe.craft(inventory, items);
    }

    public synchronized boolean canCraft(CraftingRecipe recipe) {
        return recipe.canCraft(inventory, items);
    }

    public List<CraftingRecipe> recipes() {
        return recipes;
    }

    public synchronized String selectedLabel() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return "Empty";
        }
        return label(items.requireById(stack.itemId()).key()) + " x" + stack.count();
    }

    public synchronized String slotLabel(int index) {
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return (index + 1) + " Empty";
        }
        return (index + 1) + " " + label(items.requireById(stack.itemId()).key()) + " x" + stack.count();
    }

    public synchronized SlotView slotView(int index) {
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return SlotView.empty();
        }
        ItemType item = items.requireById(stack.itemId());
        int durability = item.durability() <= 0 ? 0 : Math.max(0, item.durability() - stack.damage());
        return new SlotView(item.key(), label(item.key()), stack.count(), durability, item.durability(), item.foodValue(), item.healValue());
    }

    public synchronized int inventorySlotCount() {
        return inventory.size();
    }

    public synchronized String inventorySlotLabel(int index) {
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return (index + 1) + " Empty";
        }
        return label(items.requireById(stack.itemId()).key()) + " x" + stack.count();
    }

    public synchronized String itemKey(short itemId) {
        return items.requireById(itemId).key();
    }

    public synchronized int selectedIndex() {
        return selectedIndex;
    }

    public String recipeSummary(CraftingRecipe recipe) {
        StringBuilder builder = new StringBuilder(recipe.label()).append("  ");
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            CraftingRecipe.Ingredient ingredient = recipe.ingredients().get(i);
            if (i > 0) {
                builder.append(" + ");
            }
            builder.append(label(items.requireById(ingredient.itemId()).key())).append(" x").append(ingredient.count());
        }
        return builder.toString();
    }

    public synchronized String selectedTooltip() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return "Empty hand";
        }
        ItemType item = items.requireById(stack.itemId());
        StringBuilder tooltip = new StringBuilder(label(item.key()));
        if (item.isTool()) {
            tooltip.append(" | ").append(label(item.toolType().name().toLowerCase(Locale.ROOT))).append(" ");
            tooltip.append(Math.max(0, item.durability() - stack.damage())).append("/").append(item.durability());
        }
        if (item.isFood()) {
            tooltip.append(" | Food +").append(item.foodValue());
            if (item.healValue() > 0) {
                tooltip.append(" Heal +").append(item.healValue());
            }
        }
        if (item.placesBlockKey() != null) {
            tooltip.append(" | Placeable");
        }
        return tooltip.toString();
    }

    private static String label(String key) {
        String value = key.substring(key.indexOf(':') + 1).replace('_', ' ');
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    public record SlotView(String itemKey, String label, int count, int durabilityLeft, int maxDurability, int foodValue, int healValue) {
        private static SlotView empty() {
            return new SlotView("", "Empty", 0, 0, 0, 0, 0);
        }

        public boolean isEmpty() {
            return count <= 0 || itemKey.isBlank();
        }

        public boolean hasDurability() {
            return maxDurability > 0;
        }
    }
}
