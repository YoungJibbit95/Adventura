package dev.voxelgame.common.content;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.AlphaMilestones;
import dev.voxelgame.common.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTagRegistryTest {
    @Test
    void aliasesResolveToCanonicalContentTags() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:wild_berries", ContentTag.FOOD));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:active_campfire", ContentTag.STATION));
        assertTrue(tags.hasTag(ContentKind.ENTITY, "voxel:bunny", ContentTag.AMBIENT));
    }

    @Test
    void defaultRegistryTagsCriticalAlphaContracts() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:twig", ContentTag.STARTER));
        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:charcoal", ContentTag.FUEL));
        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:stone_pickaxe", ContentTag.TOOL_PICKAXE));
        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:cooking_pot", ContentTag.STATION));
        assertTrue(tags.hasTag(ContentKind.ITEM, "voxel:ancient_fragment", ContentTag.RUIN_PROGRESSION));
        assertTrue(tags.hasTag(ContentKey.item("voxel:stone_knife"), ContentTag.RANGED));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:glow_crystal_node", ContentTag.ORE));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:storage_crate", ContentTag.STORAGE));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:campfire_active", ContentTag.LIGHT_SOURCE));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:snow", ContentTag.COLD));
        assertTrue(tags.hasTag(ContentKind.BLOCK, "voxel:forge", ContentTag.HEAVY));
        assertTrue(tags.hasTag(ContentKind.ENTITY, "voxel:firefly_swarm", ContentTag.NIGHT_VISIBLE));
        assertTrue(tags.hasTag(ContentKind.ENTITY, "voxel:firefly_swarm", ContentTag.FLOATY));
        assertTrue(tags.hasTag(ContentKind.ENTITY, "voxel:arrow_projectile", ContentTag.PROJECTILE));
    }

    @Test
    void taggedItemAndBlockKeysExistInDefaultRegistries() {
        var items = Items.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        ContentTagRegistry tags = ContentTagRegistry.createDefault(items, blocks);

        for (String key : tags.entries(ContentKind.ITEM).keySet()) {
            assertTrue(items.findByKey(key).isPresent(), key);
        }
        for (String key : tags.entries(ContentKind.BLOCK).keySet()) {
            assertTrue(blocks.findByKey(key).isPresent(), key);
        }
    }

    @Test
    void unknownKeysReturnEmptyTagsAndNoCanonicalKey() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        assertTrue(tags.tagsFor(ContentKey.item("voxel:not_real")).isEmpty());
        assertFalse(tags.hasTag(ContentKind.BLOCK, "voxel:not_real", ContentTag.STORAGE));
        assertTrue(tags.canonicalKey(ContentKey.entity("voxel:not_real")).isEmpty());
    }

    @Test
    void defaultCoverageHasNoMissingKeysOrDanglingAliases() {
        var items = Items.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        ContentTagRegistry tags = ContentTagRegistry.createDefault(items, blocks);

        ContentTagRegistry.CoverageReport report = tags.validateCoverage(items, blocks, ContentTagRegistry.defaultEntityKeys());

        assertTrue(report.ok(), () -> "missing=" + report.missingKeys() + " danglingAliases=" + report.danglingAliases());
    }

    @Test
    void queryViewsAreImmutable() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        assertThrows(UnsupportedOperationException.class, () -> tags.tagsFor(ContentKey.item("voxel:twig")).add(ContentTag.HEAVY));
        assertThrows(UnsupportedOperationException.class, () -> tags.entries(ContentKind.ITEM).put("voxel:test", Set.of(ContentTag.FOOD)));
        assertThrows(UnsupportedOperationException.class, () -> tags.keysWithTag(ContentKind.BLOCK, ContentTag.STORAGE).add("voxel:test"));
    }

    @Test
    void contentKeyFactoryKeepsKindWithKey() {
        assertEquals(new ContentKey(ContentKind.ITEM, "voxel:twig"), ContentKey.item("voxel:twig"));
        assertEquals(new ContentKey(ContentKind.BLOCK, "voxel:campfire"), ContentKey.block("voxel:campfire"));
        assertEquals(new ContentKey(ContentKind.ENTITY, "voxel:forest_bunny"), ContentKey.entity("voxel:forest_bunny"));
        assertThrows(IllegalArgumentException.class, () -> ContentKey.item(" "));
    }

    @Test
    void everyMilestoneRequiredItemHasContentTags() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        for (String itemKey : AlphaMilestones.requiredItemKeys()) {
            assertFalse(tags.tagsFor(ContentKind.ITEM, itemKey).isEmpty(), itemKey);
        }
    }
}
