package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.item.StarterInventory;
import dev.voxelgame.common.registry.Registry;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final Map<StoragePos, Inventory> storageInventories = new HashMap<>();
    private StoragePos openStoragePos;
    private Inventory openStorageInventory;
    private int selectedIndex;

    public synchronized void resetForNewGame() {
        StarterInventory.apply(inventory, items);
        storageInventories.clear();
        openStoragePos = null;
        openStorageInventory = null;
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

    public synchronized boolean scroll(int direction) {
        if (direction == 0) {
            return false;
        }
        int nextIndex = Math.floorMod(selectedIndex + direction, HOTBAR_SLOTS);
        if (nextIndex == selectedIndex) {
            return false;
        }
        selectedIndex = nextIndex;
        return true;
    }

    public synchronized void applySnapshot(List<ItemStack> slots) {
        inventory.replaceSlots(slots);
    }

    public synchronized void openStorage(int x, int y, int z) {
        StoragePos pos = new StoragePos(x, y, z);
        openStoragePos = pos;
        openStorageInventory = storageInventories.computeIfAbsent(pos, ignored -> new Inventory(18));
    }

    public synchronized void applyStorageSnapshot(int x, int y, int z, List<ItemStack> slots) {
        StoragePos pos = new StoragePos(x, y, z);
        Inventory storage = new Inventory(slots.size());
        storage.replaceSlots(slots);
        storageInventories.put(pos, storage);
        openStoragePos = pos;
        openStorageInventory = storage;
    }

    public synchronized void closeStorage() {
        openStoragePos = null;
        openStorageInventory = null;
    }

    public synchronized boolean storageOpen() {
        return openStorageInventory != null && openStoragePos != null;
    }

    public synchronized int storageX() {
        return openStoragePos == null ? 0 : openStoragePos.x();
    }

    public synchronized int storageY() {
        return openStoragePos == null ? 0 : openStoragePos.y();
    }

    public synchronized int storageZ() {
        return openStoragePos == null ? 0 : openStoragePos.z();
    }

    public synchronized int storageSlotCount() {
        return openStorageInventory == null ? 0 : openStorageInventory.size();
    }

    public synchronized SlotView storageSlotView(int index) {
        if (openStorageInventory == null || index < 0 || index >= openStorageInventory.size()) {
            return SlotView.empty();
        }
        return viewFor(openStorageInventory.slot(index));
    }

    public synchronized boolean transferStorage(boolean fromStorage, int slot) {
        if (openStorageInventory == null) {
            return false;
        }
        Inventory source = fromStorage ? openStorageInventory : inventory;
        Inventory target = fromStorage ? inventory : openStorageInventory;
        if (slot < 0 || slot >= source.size()) {
            return false;
        }
        ItemStack stack = source.slot(slot);
        if (stack.isEmpty()) {
            return false;
        }
        int remaining = target.addStack(stack, items);
        int moved = stack.count() - remaining;
        if (moved <= 0) {
            return false;
        }
        source.setSlot(slot, remaining == 0 ? ItemStack.EMPTY : new ItemStack(stack.itemId(), remaining, stack.damage()));
        return true;
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
        return InteractionRules.breakMultiplier(inventory.slot(selectedIndex), items, target);
    }

    public synchronized void damageSelectedTool(BlockType target) {
        int amount = InteractionRules.toolDamage(inventory.slot(selectedIndex), items, target);
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

    public synchronized boolean craft(CraftingRecipe recipe, CraftingStationType stationType) {
        return recipe.craft(inventory, items, stationType);
    }

    public synchronized boolean canCraft(CraftingRecipe recipe) {
        return recipe.canCraft(inventory, items);
    }

    public synchronized boolean canCraft(CraftingRecipe recipe, CraftingStationType stationType) {
        return recipe.canCraft(inventory, items, stationType);
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

    public synchronized Optional<String> selectedItemKey() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.requireById(stack.itemId()).key());
    }

    public synchronized String slotLabel(int index) {
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return (index + 1) + " Empty";
        }
        return (index + 1) + " " + label(items.requireById(stack.itemId()).key()) + " x" + stack.count();
    }

    public synchronized SlotView slotView(int index) {
        return viewFor(inventory.slot(index));
    }

    private SlotView viewFor(ItemStack stack) {
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
        StringBuilder builder = new StringBuilder(stationLabel(recipe.stationType())).append("  ");
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            CraftingRecipe.Ingredient ingredient = recipe.ingredients().get(i);
            if (i > 0) {
                builder.append(" + ");
            }
            builder.append(label(items.requireById(ingredient.itemId()).key())).append(" x").append(ingredient.count());
        }
        return builder.toString();
    }

    public String recipeStatus(CraftingRecipe recipe, CraftingStationType stationType) {
        if (!recipe.isAvailableAt(stationType)) {
            return "Need " + stationLabel(recipe.stationType());
        }
        return canCraft(recipe, stationType) ? "Ready" : "Missing ingredients";
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

    public static String stationLabel(CraftingStationType stationType) {
        return switch (stationType) {
            case INVENTORY -> "Inventory";
            case CRAFTING_TABLE -> "Crafting Table";
            case CAMPFIRE -> "Campfire";
            case COOKING_POT -> "Cooking Pot";
            case WORKBENCH -> "Workbench";
        };
    }

    private record StoragePos(int x, int y, int z) {
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
