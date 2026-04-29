package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

import java.util.Arrays;
import java.util.List;

public final class Inventory {
    private final ItemStack[] slots;

    public Inventory(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Inventory size must be > 0");
        }
        this.slots = new ItemStack[size];
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    private Inventory(ItemStack[] slots) {
        this.slots = slots.clone();
    }

    public int size() {
        return slots.length;
    }

    public ItemStack slot(int index) {
        return slots[index];
    }

    public List<ItemStack> slots() {
        return List.of(slots.clone());
    }

    public void replaceSlots(List<ItemStack> newSlots) {
        if (newSlots.size() != slots.length) {
            throw new IllegalArgumentException("Expected " + slots.length + " inventory slots but got " + newSlots.size());
        }
        for (int i = 0; i < slots.length; i++) {
            slots[i] = newSlots.get(i);
        }
    }

    public void clear() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public int count(short itemId) {
        int total = 0;
        for (ItemStack slot : slots) {
            if (slot.itemId() == itemId) {
                total += slot.count();
            }
        }
        return total;
    }

    public boolean has(short itemId, int count) {
        return count(itemId) >= count;
    }

    public boolean canAdd(short itemId, int count, Registry<ItemType> items) {
        return copy().add(itemId, count, items) == 0;
    }

    public int add(short itemId, int count, Registry<ItemType> items) {
        if (itemId == 0 || count <= 0) {
            return count;
        }
        int remaining = count;
        ItemType type = items.requireById(itemId);
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.itemId() == itemId && slot.count() < type.maxStackSize()) {
                int moved = Math.min(remaining, type.maxStackSize() - slot.count());
                slots[i] = new ItemStack(itemId, slot.count() + moved);
                remaining -= moved;
            }
        }
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i].isEmpty()) {
                int moved = Math.min(remaining, type.maxStackSize());
                slots[i] = new ItemStack(itemId, moved);
                remaining -= moved;
            }
        }
        return remaining;
    }

    public boolean remove(short itemId, int count) {
        if (!has(itemId, count)) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.itemId() == itemId) {
                int removed = Math.min(remaining, slot.count());
                int left = slot.count() - removed;
                slots[i] = left == 0 ? ItemStack.EMPTY : new ItemStack(itemId, left);
                remaining -= removed;
            }
        }
        return true;
    }

    public boolean removeFromSlot(int index, int count) {
        ItemStack slot = slots[index];
        if (slot.isEmpty() || count <= 0 || slot.count() < count) {
            return false;
        }
        int left = slot.count() - count;
        slots[index] = left == 0 ? ItemStack.EMPTY : new ItemStack(slot.itemId(), left);
        return true;
    }

    public Inventory copy() {
        return new Inventory(slots);
    }
}
