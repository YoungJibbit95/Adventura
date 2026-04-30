package dev.voxelgame.common.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryTest {
    @Test
    void aliasesResolveToCanonicalValuesWithoutIncreasingRegistrySize() {
        Registry<String> registry = new Registry<>("test");
        registry.register((short) 1, "voxel:berries", "berries");

        registry.registerAlias("voxel:wild_berries", "voxel:berries");

        assertEquals("berries", registry.requireByKey("voxel:wild_berries"));
        assertEquals("voxel:berries", registry.canonicalKey("voxel:wild_berries").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void aliasTargetsMustAlreadyExist() {
        Registry<String> registry = new Registry<>("test");

        assertThrows(IllegalArgumentException.class, () -> registry.registerAlias("voxel:old", "voxel:new"));
    }
}
