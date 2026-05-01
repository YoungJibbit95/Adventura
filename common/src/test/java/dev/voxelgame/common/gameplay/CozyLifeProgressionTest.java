package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.content.ContentKind;
import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.content.ContentTagRegistry;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.Biomes;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CozyLifeProgressionTest {
    @Test
    void defaultCreaturesCoverP95RolesAndCoreAlphaCreatures() {
        Set<CreatureRole> roles = CozyLifeProgression.defaultCreatures().stream()
                .flatMap(creature -> creature.roles().stream())
                .collect(Collectors.toSet());
        Set<String> creatureKeys = CozyLifeProgression.defaultCreatures().stream()
                .map(CreatureDesign::entityKey)
                .collect(Collectors.toSet());

        assertTrue(roles.containsAll(EnumSet.allOf(CreatureRole.class)));
        assertTrue(creatureKeys.containsAll(CozyLifeProgression.coreAlphaCreatureKeys()));
        for (CreatureRole role : CreatureRole.values()) {
            assertFalse(CozyLifeProgression.creaturesWithRole(role).isEmpty(), role.name());
        }
    }

    @Test
    void creatureDesignsAreUniqueOrderedAndBoundedForAlpha() {
        Set<String> keys = new HashSet<>();

        var creatures = CozyLifeProgression.defaultCreatures();
        for (int i = 0; i < creatures.size(); i++) {
            CreatureDesign creature = creatures.get(i);
            assertEquals(i + 1, creature.order(), creature.entityKey());
            assertTrue(keys.add(creature.entityKey()), creature.entityKey());
            assertTrue(creature.friendshipSteps() <= 3, creature.entityKey());
            assertFalse(creature.feedingContract().isBlank(), creature.entityKey());
            assertFalse(creature.friendshipContract().isBlank(), creature.entityKey());
            assertFalse(creature.interactionContract().isBlank(), creature.entityKey());
            assertFalse(creature.animationContract().isBlank(), creature.entityKey());
            assertFalse(creature.networkStateContract().isBlank(), creature.entityKey());
            assertFalse(creature.saveStateContract().isBlank(), creature.entityKey());
            assertFalse(creature.uiFeedback().isBlank(), creature.entityKey());
        }
    }

    @Test
    void creatureDesignsReferenceRegisteredContentAndEntityTags() {
        var items = Items.createDefaultRegistry();
        var biomes = Biomes.createDefaultRegistry();
        var tags = ContentTagRegistry.createDefault();
        Set<String> entityKeys = ContentTagRegistry.defaultEntityKeys();

        for (CreatureDesign creature : CozyLifeProgression.defaultCreatures()) {
            assertTrue(entityKeys.contains(creature.entityKey()), creature.entityKey());
            assertTrue(tags.hasTag(ContentKind.ENTITY, creature.entityKey(), ContentTag.AMBIENT), creature.entityKey());
            if (creature.roles().contains(CreatureRole.RARE_DANGER)) {
                assertTrue(tags.hasTag(ContentKind.ENTITY, creature.entityKey(), ContentTag.ADVENTURE_DANGER), creature.entityKey());
            }
            if (creature.roles().contains(CreatureRole.FRIENDSHIP)) {
                assertTrue(tags.hasTag(ContentKind.ENTITY, creature.entityKey(), ContentTag.FRIENDLY), creature.entityKey());
                assertFalse(creature.favoriteItemKeys().isEmpty(), creature.entityKey());
            }

            for (String biomeKey : creature.biomeKeys()) {
                assertTrue(biomes.findByKey(biomeKey).isPresent(), creature.entityKey() + " missing biome " + biomeKey);
            }
            for (String itemKey : creature.favoriteItemKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), creature.entityKey() + " missing favorite " + itemKey);
            }
            for (String itemKey : creature.hintTargetKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), creature.entityKey() + " missing hint target " + itemKey);
            }
            for (String itemKey : creature.resourceItemKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), creature.entityKey() + " missing resource " + itemKey);
            }
        }
    }

    @Test
    void cozyIdentityOutweighsDangerAndFeedableCreaturesStaySmall() {
        long dangerCount = CozyLifeProgression.creaturesWithRole(CreatureRole.RARE_DANGER).size();
        long cozyOrFriendlyCount = CozyLifeProgression.defaultCreatures().stream()
                .filter(creature -> creature.roles().contains(CreatureRole.FRIENDSHIP)
                        || creature.roles().contains(CreatureRole.BASE_COMFORT)
                        || creature.disposition() == CreatureDisposition.FRIENDLY)
                .count();

        assertTrue(cozyOrFriendlyCount > dangerCount * 2, "Cozy roles should dominate danger roles");
        assertTrue(CozyLifeProgression.feedableCreatureKeys().containsAll(Set.of(
                CozyLifeProgression.COZY_SHEEP,
                CozyLifeProgression.FOREST_BUNNY,
                CozyLifeProgression.MOSS_SNAIL,
                CozyLifeProgression.LITTLE_BOAR
        )));
        assertFalse(CozyLifeProgression.feedableCreatureKeys().contains(CozyLifeProgression.FIREFLY_SWARM));
    }

    @Test
    void mossSnailDropsStayInsideItsPeacefulResourceContract() {
        Set<String> snailResources = CozyLifeProgression.findCreature(CozyLifeProgression.MOSS_SNAIL)
                .orElseThrow()
                .resourceItemKeys()
                .stream()
                .collect(Collectors.toSet());
        Set<String> dropKeys = EntityDrops.dropsFor(CozyLifeProgression.MOSS_SNAIL, 3L).stream()
                .map(EntityDrops.Drop::itemKey)
                .collect(Collectors.toSet());

        assertFalse(dropKeys.isEmpty());
        assertTrue(snailResources.containsAll(dropKeys));
        assertTrue(EntityDrops.dropsFor(CozyLifeProgression.FOREST_BUNNY, 3L).isEmpty());
    }
}
