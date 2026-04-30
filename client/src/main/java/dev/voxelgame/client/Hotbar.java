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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final Set<Short> discoveredItems = new HashSet<>();
    private StoragePos openStoragePos;
    private Inventory openStorageInventory;
    private int selectedIndex;

    public synchronized void resetForNewGame() {
        StarterInventory.apply(inventory, items);
        storageInventories.clear();
        openStoragePos = null;
        openStorageInventory = null;
        selectedIndex = 0;
        discoveredItems.clear();
        refreshDiscoveredItems();
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
        refreshDiscoveredItems();
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
        return transferStorage(fromStorage, slot, Integer.MAX_VALUE);
    }

    public synchronized boolean transferStorage(boolean fromStorage, int slot, int count) {
        if (openStorageInventory == null) {
            return false;
        }
        Inventory source = fromStorage ? openStorageInventory : inventory;
        Inventory target = fromStorage ? inventory : openStorageInventory;
        if (slot < 0 || slot >= source.size() || count < 1) {
            return false;
        }
        ItemStack stack = source.slot(slot);
        if (stack.isEmpty()) {
            return false;
        }
        int requested = Math.min(count, stack.count());
        int remaining = target.addStack(new ItemStack(stack.itemId(), requested, stack.damage()), items);
        int moved = requested - remaining;
        if (moved <= 0) {
            return false;
        }
        int left = stack.count() - moved;
        source.setSlot(slot, left == 0 ? ItemStack.EMPTY : new ItemStack(stack.itemId(), left, stack.damage()));
        if (fromStorage) {
            discoveredItems.add(stack.itemId());
        }
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

    public synchronized boolean selectedItemIsFood() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return false;
        }
        return items.requireById(stack.itemId()).isFood();
    }

    public synchronized float selectedBreakMultiplier(BlockType target) {
        return InteractionRules.breakMultiplier(inventory.slot(selectedIndex), items, target);
    }

    public synchronized boolean canHarvestSelected(BlockType target) {
        return InteractionRules.canHarvest(inventory.slot(selectedIndex), items, target);
    }

    public synchronized void damageSelectedTool(BlockType target) {
        int amount = InteractionRules.toolDamage(inventory.slot(selectedIndex), items, target);
        inventory.damageSlot(selectedIndex, amount, items);
    }

    public synchronized boolean addItem(String itemKey, int count) {
        return items.findByKey(itemKey)
                .map(item -> {
                    int remaining = inventory.add(item.id(), count, items);
                    if (remaining < count) {
                        discoveredItems.add(item.id());
                    }
                    return remaining == 0;
                })
                .orElse(false);
    }

    public synchronized boolean craft(CraftingRecipe recipe) {
        boolean crafted = recipe.craft(inventory, items);
        if (crafted) {
            discoveredItems.add(recipe.result().itemId());
        }
        return crafted;
    }

    public synchronized boolean craft(CraftingRecipe recipe, CraftingStationType stationType) {
        boolean crafted = recipe.craft(inventory, items, stationType);
        if (crafted) {
            discoveredItems.add(recipe.result().itemId());
        }
        return crafted;
    }

    public synchronized boolean canCraft(CraftingRecipe recipe) {
        return recipe.canCraft(inventory, items);
    }

    public synchronized boolean canCraft(CraftingRecipe recipe, CraftingStationType stationType) {
        return recipe.canCraft(inventory, items, stationType);
    }

    public synchronized Optional<List<Integer>> inputSlotsFor(CraftingRecipe recipe) {
        Inventory simulated = inventory.copy();
        List<Integer> inputSlots = new ArrayList<>();
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            int remaining = ingredient.count();
            for (int slot = 0; slot < simulated.size() && remaining > 0; slot++) {
                ItemStack stack = simulated.slot(slot);
                if (stack.itemId() != ingredient.itemId()) {
                    continue;
                }
                int removed = Math.min(remaining, stack.count());
                if (removed > 0 && simulated.removeFromSlot(slot, removed)) {
                    if (!inputSlots.contains(slot)) {
                        inputSlots.add(slot);
                    }
                    remaining -= removed;
                }
            }
            if (remaining > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(inputSlots));
    }

    public synchronized int itemCount(short itemId) {
        return inventory.count(itemId);
    }

    public synchronized boolean sortBackpack() {
        List<ItemStack> before = backpackSlots();
        List<ItemStack> sorted = before.stream()
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator
                        .comparing((ItemStack stack) -> items.requireById(stack.itemId()).key())
                        .thenComparingInt(ItemStack::damage)
                        .thenComparing(Comparator.comparingInt(ItemStack::count).reversed()))
                .toList();
        int slot = HOTBAR_SLOTS;
        for (ItemStack stack : sorted) {
            inventory.setSlot(slot, stack);
            slot++;
        }
        while (slot < inventory.size()) {
            inventory.setSlot(slot, ItemStack.EMPTY);
            slot++;
        }
        return !before.equals(backpackSlots());
    }

    public synchronized boolean quickMoveInventorySlot(int sourceSlot) {
        if (sourceSlot < 0 || sourceSlot >= inventory.size()) {
            return false;
        }
        ItemStack stack = inventory.slot(sourceSlot);
        if (stack.isEmpty()) {
            return false;
        }
        int targetStart = sourceSlot < HOTBAR_SLOTS ? HOTBAR_SLOTS : 0;
        int targetEnd = sourceSlot < HOTBAR_SLOTS ? inventory.size() : HOTBAR_SLOTS;
        return moveSlotToRange(sourceSlot, targetStart, targetEnd);
    }

    public synchronized boolean moveInventorySlot(int sourceSlot, int targetSlot) {
        if (sourceSlot < 0 || sourceSlot >= inventory.size() || targetSlot < 0 || targetSlot >= inventory.size() || sourceSlot == targetSlot) {
            return false;
        }
        ItemStack source = inventory.slot(sourceSlot);
        if (source.isEmpty()) {
            return false;
        }
        ItemStack target = inventory.slot(targetSlot);
        if (target.isEmpty()) {
            inventory.setSlot(targetSlot, source);
            inventory.setSlot(sourceSlot, ItemStack.EMPTY);
            return true;
        }
        if (target.itemId() == source.itemId() && target.damage() == source.damage() && source.damage() == 0) {
            ItemType type = items.requireById(source.itemId());
            int capacity = type.maxStackSize() - target.count();
            if (capacity > 0) {
                int moved = Math.min(capacity, source.count());
                inventory.setSlot(targetSlot, new ItemStack(target.itemId(), target.count() + moved, target.damage()));
                int remaining = source.count() - moved;
                inventory.setSlot(sourceSlot, remaining <= 0 ? ItemStack.EMPTY : new ItemStack(source.itemId(), remaining, source.damage()));
                return true;
            }
        }
        inventory.setSlot(sourceSlot, target);
        inventory.setSlot(targetSlot, source);
        return true;
    }

    public synchronized boolean trashInventorySlot(int slot) {
        if (slot < 0 || slot >= inventory.size() || inventory.slot(slot).isEmpty()) {
            return false;
        }
        inventory.setSlot(slot, ItemStack.EMPTY);
        return true;
    }

    public synchronized boolean splitInventorySlot(int sourceSlot) {
        if (sourceSlot < 0 || sourceSlot >= inventory.size()) {
            return false;
        }
        ItemStack source = inventory.slot(sourceSlot);
        if (source.isEmpty() || source.count() < 2 || source.damage() != 0) {
            return false;
        }
        int targetSlot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (i != sourceSlot && inventory.slot(i).isEmpty()) {
                targetSlot = i;
                break;
            }
        }
        if (targetSlot < 0) {
            return false;
        }
        int split = source.count() / 2;
        int remaining = source.count() - split;
        inventory.setSlot(sourceSlot, new ItemStack(source.itemId(), remaining, source.damage()));
        inventory.setSlot(targetSlot, new ItemStack(source.itemId(), split, source.damage()));
        return true;
    }

    private boolean moveSlotToRange(int sourceSlot, int targetStart, int targetEnd) {
        ItemStack source = inventory.slot(sourceSlot);
        ItemType type = items.requireById(source.itemId());
        int remaining = source.count();
        if (source.damage() == 0) {
            for (int i = targetStart; i < targetEnd && remaining > 0; i++) {
                ItemStack target = inventory.slot(i);
                if (target.itemId() == source.itemId() && target.damage() == 0 && target.count() < type.maxStackSize()) {
                    int moved = Math.min(remaining, type.maxStackSize() - target.count());
                    inventory.setSlot(i, new ItemStack(source.itemId(), target.count() + moved, source.damage()));
                    remaining -= moved;
                }
            }
        }
        for (int i = targetStart; i < targetEnd && remaining > 0; i++) {
            if (inventory.slot(i).isEmpty()) {
                int moved = Math.min(remaining, type.maxStackSize());
                inventory.setSlot(i, new ItemStack(source.itemId(), moved, source.damage()));
                remaining -= moved;
            }
        }
        int moved = source.count() - remaining;
        if (moved <= 0) {
            return false;
        }
        inventory.setSlot(sourceSlot, remaining == 0 ? ItemStack.EMPTY : new ItemStack(source.itemId(), remaining, source.damage()));
        return true;
    }

    private List<ItemStack> backpackSlots() {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = HOTBAR_SLOTS; i < inventory.size(); i++) {
            slots.add(inventory.slot(i));
        }
        return slots;
    }

    private void refreshDiscoveredItems() {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.slot(i);
            if (!stack.isEmpty()) {
                discoveredItems.add(stack.itemId());
            }
        }
    }

    private boolean ingredientsDiscovered(CraftingRecipe recipe) {
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            if (!discoveredItems.contains(ingredient.itemId())) {
                return false;
            }
        }
        return true;
    }

    public List<CraftingRecipe> recipes() {
        return recipes;
    }

    public synchronized List<CraftingRecipe> discoveredRecipes() {
        return recipes.stream()
                .filter(this::ingredientsDiscovered)
                .toList();
    }

    public synchronized String selectedLabel() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return "Empty";
        }
        return itemDisplayName(stack.itemId()) + " x" + stack.count();
    }

    public synchronized Optional<String> selectedItemKey() {
        ItemStack stack = inventory.slot(selectedIndex);
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return items.findById(stack.itemId())
                .map(ItemType::key)
                .or(() -> Optional.of("unknown:" + stack.itemId()));
    }

    public synchronized String slotLabel(int index) {
        if (index < 0 || index >= inventory.size()) {
            return "Invalid slot";
        }
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return (index + 1) + " Empty";
        }
        return (index + 1) + " " + itemDisplayName(stack.itemId()) + " x" + stack.count();
    }

    public synchronized SlotView slotView(int index) {
        if (index < 0 || index >= inventory.size()) {
            return SlotView.empty();
        }
        return viewFor(inventory.slot(index));
    }

    private SlotView viewFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return SlotView.empty();
        }
        ItemType item = items.findById(stack.itemId()).orElse(null);
        if (item == null) {
            return new SlotView(
                    "unknown:" + stack.itemId(),
                    "Unknown Item",
                    "Unknown",
                    "Unrecognized item id " + stack.itemId(),
                    "Unknown",
                    stack.count(),
                    0,
                    0,
                    "-",
                    0,
                    0,
                    0,
                    0,
                    false
            );
        }
        int durability = item.durability() <= 0 ? 0 : Math.max(0, item.durability() - stack.damage());
        return new SlotView(
                item.key(),
                label(item.key()),
                itemCategory(item),
                itemDescription(item),
                itemRarity(item),
                stack.count(),
                durability,
                item.durability(),
                toolTypeLabel(item),
                toolLevel(item),
                item.foodValue(),
                item.healValue(),
                comfortValue(item),
                item.placesBlockKey() != null
        );
    }

    public synchronized int inventorySlotCount() {
        return inventory.size();
    }

    public synchronized String inventorySlotLabel(int index) {
        if (index < 0 || index >= inventory.size()) {
            return "Invalid slot";
        }
        ItemStack stack = inventory.slot(index);
        if (stack.isEmpty()) {
            return (index + 1) + " Empty";
        }
        return itemDisplayName(stack.itemId()) + " x" + stack.count();
    }

    private String itemDisplayName(short itemId) {
        return items.findById(itemId)
                .map(ItemType::key)
                .map(Hotbar::label)
                .orElse("Unknown Item");
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
        ItemType item = items.findById(stack.itemId()).orElse(null);
        if (item == null) {
            return "Unknown Item | Unknown | Unknown | Unrecognized item id " + stack.itemId();
        }
        StringBuilder tooltip = new StringBuilder(label(item.key()));
        tooltip.append(" | ").append(itemCategory(item));
        tooltip.append(" | ").append(itemRarity(item));
        tooltip.append(" | ").append(itemDescription(item));
        if (item.isTool()) {
            tooltip.append(" | ").append(toolTypeLabel(item)).append(" Level ").append(toolLevel(item)).append(" ");
            tooltip.append(Math.max(0, item.durability() - stack.damage())).append("/").append(item.durability());
        }
        if (item.isFood()) {
            tooltip.append(" | Food +").append(item.foodValue());
            if (item.healValue() > 0) {
                tooltip.append(" Heal +").append(item.healValue());
            }
        }
        int comfort = comfortValue(item);
        if (comfort > 0) {
            tooltip.append(" | Comfort +").append(comfort);
        }
        if (item.placesBlockKey() != null) {
            tooltip.append(" | Placeable");
        }
        return tooltip.toString();
    }

    private static String itemCategory(ItemType item) {
        if (item.isFood()) {
            return "Food";
        }
        if (item.isTool()) {
            return "Tool";
        }
        if (item.placesBlockKey() != null) {
            return "Placeable";
        }
        return "Material";
    }

    private static String itemDescription(ItemType item) {
        if (item.isFood()) {
            return "Restores hunger" + (item.healValue() > 0 ? " and health" : "");
        }
        if (item.isTool()) {
            return "Useful for " + label(item.toolType().name().toLowerCase(Locale.ROOT)) + " work";
        }
        if (item.placesBlockKey() != null) {
            return "Can be placed in the world";
        }
        return "Crafting material";
    }

    private static String itemRarity(ItemType item) {
        String key = item.key();
        if (key.contains("ancient") || key.contains("lost") || key.contains("ruin")) {
            return "Legendary";
        }
        if (key.contains("glow") || key.contains("crystal")) {
            return "Rare";
        }
        if (key.contains("copper") || key.contains("iron") || key.contains("lantern") || key.contains("stew") || key.contains("soup")) {
            return "Uncommon";
        }
        return "Common";
    }

    private static String toolTypeLabel(ItemType item) {
        if (!item.isTool()) {
            return "";
        }
        return label(item.toolType().name().toLowerCase(Locale.ROOT));
    }

    private static int toolLevel(ItemType item) {
        return InteractionRules.toolLevel(item);
    }

    private static int comfortValue(ItemType item) {
        if (item.placesBlockKey() == null) {
            return 0;
        }
        return switch (item.placesBlockKey()) {
            case "voxel:lantern", "voxel:woven_rug" -> 3;
            case "voxel:campfire", "voxel:wooden_chair", "voxel:small_table" -> 2;
            case "voxel:flower_pot", "voxel:herb_planter", "voxel:berry_bush" -> 1;
            default -> 0;
        };
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

    public record SlotView(String itemKey, String label, String category, String description, String rarity, int count, int durabilityLeft, int maxDurability, String toolTypeLabel, int toolLevel, int foodValue, int healValue, int comfortValue, boolean placeable) {
        private static SlotView empty() {
            return new SlotView("", "Empty", "", "", "", 0, 0, 0, "", 0, 0, 0, 0, false);
        }

        public boolean isEmpty() {
            return count <= 0 || itemKey.isBlank();
        }

        public boolean hasDurability() {
            return maxDurability > 0;
        }
    }
}
