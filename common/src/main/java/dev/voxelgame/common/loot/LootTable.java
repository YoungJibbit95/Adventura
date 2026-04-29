package dev.voxelgame.common.loot;

import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public record LootTable(String key, int rolls, List<LootEntry> entries) {
    public LootTable {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Loot table key must not be blank");
        }
        if (rolls < 1) {
            throw new IllegalArgumentException("Loot table rolls must be >= 1");
        }
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Loot table must contain at least one entry");
        }
    }

    public List<ItemStack> roll(Registry<ItemType> items, LootContext context) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(context, "context");
        Random random = new Random(seedFor(context));
        List<ItemStack> result = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            for (LootEntry entry : entries) {
                if (random.nextDouble() > entry.chance()) {
                    continue;
                }
                ItemType item = items.requireByKey(entry.itemKey());
                int count = entry.minCount();
                if (entry.maxCount() > entry.minCount()) {
                    count += random.nextInt(entry.maxCount() - entry.minCount() + 1);
                }
                result.add(new ItemStack(item.id(), Math.min(count, item.maxStackSize())));
            }
        }
        return List.copyOf(result);
    }

    private long seedFor(LootContext context) {
        long seed = mix(context.worldSeed());
        seed = mix(seed ^ key.hashCode());
        seed = mix(seed ^ context.structureKey().hashCode());
        seed = mix(seed ^ context.markerKey().hashCode());
        seed = mix(seed ^ context.worldX());
        seed = mix(seed ^ ((long) context.worldY() << 21));
        return mix(seed ^ ((long) context.worldZ() << 42));
    }

    private static long mix(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
