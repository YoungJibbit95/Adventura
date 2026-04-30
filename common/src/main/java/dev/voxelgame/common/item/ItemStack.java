package dev.voxelgame.common.item;

public record ItemStack(short itemId, int count, int damage) {
    public static final ItemStack EMPTY = new ItemStack((short) 0, 0, 0);

    public ItemStack(short itemId, int count) {
        this(itemId, count, 0);
    }

    public ItemStack {
        if (itemId < 0) {
            throw new IllegalArgumentException("Item id must be >= 0");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Item count must be >= 0");
        }
        if (damage < 0) {
            throw new IllegalArgumentException("Item damage must be >= 0");
        }
        if (itemId == 0 && count != 0) {
            throw new IllegalArgumentException("Empty item id must have count 0");
        }
        if (itemId == 0 && damage != 0) {
            throw new IllegalArgumentException("Empty item id must have damage 0");
        }
    }

    public boolean isEmpty() {
        return itemId == 0 || count == 0;
    }
}
