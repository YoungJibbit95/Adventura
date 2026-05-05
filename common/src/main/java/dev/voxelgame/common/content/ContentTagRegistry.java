package dev.voxelgame.common.content;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ContentTagRegistry {
    private static final Set<String> STARTER_ITEMS = Set.of(
            "voxel:dirt",
            "voxel:torch",
            "voxel:skyroot_log",
            "voxel:stick",
            "voxel:twig",
            "voxel:fiber",
            "voxel:pebble",
            "voxel:apple",
            "voxel:stone_pickaxe"
    );

    private static final Set<String> FUEL_ITEMS = Set.of(
            "voxel:dry_grass",
            "voxel:bark_strip",
            "voxel:reed_bundle",
            "voxel:twig",
            "voxel:stick",
            "voxel:skyroot_log",
            "voxel:pine_log",
            "voxel:skyroot_planks",
            "voxel:coal",
            "voxel:charcoal"
    );

    private static final Set<String> STATION_BLOCKS = Set.of(
            "voxel:campfire",
            "voxel:campfire_active",
            "voxel:campfire_burned_out",
            "voxel:cooking_pot",
            "voxel:workbench",
            "voxel:forge"
    );

    private static final Set<String> STORAGE_BLOCKS = Set.of("voxel:storage_crate");

    private static final Set<String> COMFORT_BLOCKS = Set.of(
            "voxel:campfire",
            "voxel:campfire_active",
            "voxel:lantern",
            "voxel:ancient_lantern",
            "voxel:sleeping_mat",
            "voxel:woven_rug",
            "voxel:flower_pot",
            "voxel:herb_planter",
            "voxel:small_table",
            "voxel:wooden_chair",
            "voxel:garden_fence",
            "voxel:berry_bush",
            "voxel:mossy_path"
    );

    private static final Set<String> DEFAULT_ENTITY_KEYS = Set.of(
            "voxel:player",
            "voxel:cozy_sheep",
            "voxel:forest_bunny",
            "voxel:moss_snail",
            "voxel:firefly_swarm",
            "voxel:little_boar",
            "voxel:snow_hare",
            "voxel:mire_wisp",
            "voxel:dune_crawler",
            "voxel:forest_grazer",
            "voxel:meadow_grazer",
            "voxel:arrow_projectile"
    );

    private final Map<ContentKey, Set<ContentTag>> tagsByKey;
    private final Map<ContentKey, ContentKey> aliases;

    private ContentTagRegistry(Map<ContentKey, EnumSet<ContentTag>> tagsByKey, Map<ContentKey, ContentKey> aliases) {
        Map<ContentKey, Set<ContentTag>> tagCopy = new LinkedHashMap<>();
        for (Map.Entry<ContentKey, EnumSet<ContentTag>> entry : tagsByKey.entrySet()) {
            EnumSet<ContentTag> tags = entry.getValue().isEmpty()
                    ? EnumSet.noneOf(ContentTag.class)
                    : EnumSet.copyOf(entry.getValue());
            tagCopy.put(entry.getKey(), Collections.unmodifiableSet(tags));
        }
        this.tagsByKey = Collections.unmodifiableMap(tagCopy);
        this.aliases = Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
    }

    public static ContentTagRegistry createDefault() {
        return createDefault(Items.createDefaultRegistry(), Blocks.createDefaultRegistry());
    }

    public static ContentTagRegistry createDefault(Registry<ItemType> items, Registry<BlockType> blocks) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(blocks, "blocks");
        Builder builder = new Builder();

        for (BlockType block : blocks.values()) {
            builder.tag(ContentKey.block(block.key()), deriveBlockTags(block));
        }

        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        Map<String, EnumSet<ContentTag>> recipeTags = recipeTags(items, recipes);
        for (ItemType item : items.values()) {
            EnumSet<ContentTag> tags = deriveItemTags(item, blocks, builder);
            tags.addAll(recipeTags.getOrDefault(item.key(), EnumSet.noneOf(ContentTag.class)));
            builder.tag(ContentKey.item(item.key()), tags);
        }

        for (Map.Entry<String, String> alias : items.aliases().entrySet()) {
            builder.alias(ContentKey.item(alias.getKey()), ContentKey.item(alias.getValue()));
        }
        for (Map.Entry<String, String> alias : blocks.aliases().entrySet()) {
            builder.alias(ContentKey.block(alias.getKey()), ContentKey.block(alias.getValue()));
        }

        for (String entityKey : DEFAULT_ENTITY_KEYS) {
            builder.tag(ContentKey.entity(entityKey), deriveEntityTags(entityKey));
        }
        builder.alias(ContentKey.entity("voxel:bunny"), ContentKey.entity("voxel:forest_bunny"));
        builder.alias(ContentKey.entity("voxel:snail"), ContentKey.entity("voxel:moss_snail"));

        return builder.build();
    }

    public static Set<String> defaultEntityKeys() {
        return Collections.unmodifiableSet(DEFAULT_ENTITY_KEYS);
    }

    public Set<ContentTag> tagsFor(ContentKey key) {
        Objects.requireNonNull(key, "key");
        return resolve(key)
                .map(resolved -> tagsByKey.getOrDefault(resolved, Set.of()))
                .orElseGet(Set::of);
    }

    public Set<ContentTag> tagsFor(ContentKind kind, String key) {
        return tagsFor(new ContentKey(kind, key));
    }

    public boolean hasTag(ContentKey key, ContentTag tag) {
        Objects.requireNonNull(tag, "tag");
        return tagsFor(key).contains(tag);
    }

    public boolean hasTag(ContentKind kind, String key, ContentTag tag) {
        return hasTag(new ContentKey(kind, key), tag);
    }

    public Optional<ContentKey> canonicalKey(ContentKey key) {
        Objects.requireNonNull(key, "key");
        return resolve(key);
    }

    public Set<ContentKey> keys() {
        return tagsByKey.keySet();
    }

    public Map<String, Set<ContentTag>> entries(ContentKind kind) {
        Objects.requireNonNull(kind, "kind");
        Map<String, Set<ContentTag>> entries = new LinkedHashMap<>();
        for (Map.Entry<ContentKey, Set<ContentTag>> entry : tagsByKey.entrySet()) {
            if (entry.getKey().kind() == kind) {
                entries.put(entry.getKey().key(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    public Set<String> keysWithTag(ContentKind kind, ContentTag tag) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tag, "tag");
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<ContentKey, Set<ContentTag>> entry : tagsByKey.entrySet()) {
            if (entry.getKey().kind() == kind && entry.getValue().contains(tag)) {
                keys.add(entry.getKey().key());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    public CoverageReport validateCoverage(Registry<ItemType> items, Registry<BlockType> blocks, Collection<String> entityKeys) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(entityKeys, "entityKeys");
        Set<ContentKey> missing = new LinkedHashSet<>();
        for (ItemType item : items.values()) {
            ContentKey key = ContentKey.item(item.key());
            if (tagsFor(key).isEmpty()) {
                missing.add(key);
            }
        }
        for (BlockType block : blocks.values()) {
            ContentKey key = ContentKey.block(block.key());
            if (tagsFor(key).isEmpty()) {
                missing.add(key);
            }
        }
        for (String entityKey : entityKeys) {
            ContentKey key = ContentKey.entity(entityKey);
            if (tagsFor(key).isEmpty()) {
                missing.add(key);
            }
        }

        Set<ContentKey> danglingAliases = new LinkedHashSet<>();
        for (Map.Entry<ContentKey, ContentKey> alias : aliases.entrySet()) {
            if (!tagsByKey.containsKey(alias.getValue())) {
                danglingAliases.add(alias.getKey());
            }
        }

        return new CoverageReport(missing, danglingAliases);
    }

    private Optional<ContentKey> resolve(ContentKey key) {
        if (tagsByKey.containsKey(key)) {
            return Optional.of(key);
        }
        ContentKey target = aliases.get(key);
        if (target != null && tagsByKey.containsKey(target)) {
            return Optional.of(target);
        }
        return Optional.empty();
    }

    private static Map<String, EnumSet<ContentTag>> recipeTags(Registry<ItemType> items, List<CraftingRecipe> recipes) {
        Map<String, EnumSet<ContentTag>> tags = new LinkedHashMap<>();
        for (CraftingRecipe recipe : recipes) {
            for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
                ItemType item = items.requireById(ingredient.itemId());
                EnumSet<ContentTag> itemTags = tags.computeIfAbsent(item.key(), ignored -> EnumSet.noneOf(ContentTag.class));
                itemTags.add(ContentTag.CRAFTING_INGREDIENT);
                if (recipe.category().name().equals("FOOD") || recipe.stationType() == CraftingStationType.COOKING_POT) {
                    itemTags.add(ContentTag.COOKING_INGREDIENT);
                }
            }
        }
        return tags;
    }

    private static EnumSet<ContentTag> deriveItemTags(ItemType item, Registry<BlockType> blocks, Builder builder) {
        EnumSet<ContentTag> tags = EnumSet.noneOf(ContentTag.class);
        String key = item.key();

        if (STARTER_ITEMS.contains(key)) {
            tags.add(ContentTag.STARTER);
        }
        if (item.placesBlockKey() != null) {
            tags.add(ContentTag.PLACEABLE);
            blocks.canonicalKey(item.placesBlockKey())
                    .map(ContentKey::block)
                    .map(builder::tagsFor)
                    .ifPresent(blockTags -> copyPlaceableTags(tags, blockTags));
        }
        if (item.isFood()) {
            tags.add(ContentTag.FOOD);
            if (isCookedFood(key)) {
                tags.add(ContentTag.COOKED_FOOD);
            } else {
                tags.add(ContentTag.RAW_FOOD);
            }
        }
        if (item.healValue() > 0) {
            tags.add(ContentTag.HEALING);
        }
        if (FUEL_ITEMS.contains(key)) {
            tags.add(ContentTag.FUEL);
            tags.add(ContentTag.FLAMMABLE);
        }
        if (item.isTool()) {
            tags.add(ContentTag.TOOL);
            switch (item.toolType()) {
                case AXE -> tags.add(ContentTag.TOOL_AXE);
                case PICKAXE -> tags.add(ContentTag.TOOL_PICKAXE);
                case SHOVEL -> tags.add(ContentTag.TOOL_SHOVEL);
                case KNIFE -> tags.add(ContentTag.TOOL_KNIFE);
                case NONE -> {
                }
            }
        }
        if (key.contains("sword") || key.endsWith("_knife")) {
            tags.add(ContentTag.WEAPON);
        }
        if (canLaunchProjectile(item)) {
            tags.add(ContentTag.RANGED);
            tags.add(ContentTag.PROJECTILE);
        }
        if (key.contains("arrow") || key.endsWith("_ammo")) {
            tags.add(ContentTag.AMMO);
        }
        tagByKeyShape(tags, key);
        if (tags.isEmpty()) {
            tags.add(ContentTag.CRAFTING_INGREDIENT);
        }
        return tags;
    }

    private static void copyPlaceableTags(EnumSet<ContentTag> target, Set<ContentTag> blockTags) {
        for (ContentTag tag : List.of(
                ContentTag.STATION,
                ContentTag.STORAGE,
                ContentTag.COMFORT_SOURCE,
                ContentTag.LIGHT_SOURCE,
                ContentTag.EMISSIVE,
                ContentTag.TRANSPARENT,
                ContentTag.WOOD,
                ContentTag.STONE,
                ContentTag.CLAY,
                ContentTag.METAL,
                ContentTag.COPPER,
                ContentTag.IRON,
                ContentTag.CRYSTAL,
                ContentTag.ANCIENT,
                ContentTag.DECOR,
                ContentTag.COZY,
                ContentTag.COLD,
                ContentTag.HOT,
                ContentTag.HEAVY
        )) {
            if (blockTags.contains(tag)) {
                target.add(tag);
            }
        }
    }

    private static EnumSet<ContentTag> deriveBlockTags(BlockType block) {
        EnumSet<ContentTag> tags = EnumSet.noneOf(ContentTag.class);
        String key = block.key();

        if (block.renderLayer() != BlockRenderLayer.SOLID || !block.opaque()) {
            tags.add(ContentTag.TRANSPARENT);
        }
        if (block.emitsLight()) {
            tags.add(ContentTag.LIGHT_SOURCE);
            tags.add(ContentTag.EMISSIVE);
        }
        if (block.dropItemKey() != null && !block.dropItemKey().equals(key)) {
            tags.add(ContentTag.RESOURCE_NODE);
        }
        if (STATION_BLOCKS.contains(key)) {
            tags.add(ContentTag.STATION);
        }
        if (STORAGE_BLOCKS.contains(key)) {
            tags.add(ContentTag.STORAGE);
        }
        if (COMFORT_BLOCKS.contains(key)) {
            tags.add(ContentTag.COMFORT_SOURCE);
            tags.add(ContentTag.COZY);
            tags.add(ContentTag.DECOR);
        }
        if (isPlantBlock(key)) {
            tags.add(ContentTag.PLANT);
            tags.add(ContentTag.FLAMMABLE);
        }
        if (isWoodKey(key)) {
            tags.add(ContentTag.WOOD);
            tags.add(ContentTag.FLAMMABLE);
        }
        if (key.contains("ore") || key.contains("crystal_node") || key.contains("deposit")) {
            tags.add(ContentTag.ORE);
            tags.add(ContentTag.RESOURCE_NODE);
        }
        if (key.contains("snow") || key.contains("ice")) {
            tags.add(ContentTag.COLD);
        }
        if (key.contains("campfire") || key.contains("forge") || key.contains("lava")) {
            tags.add(ContentTag.HOT);
        }
        if (block.solid() && (block.hardness() >= 1.0f || key.contains("stone") || key.contains("ore"))) {
            tags.add(ContentTag.HEAVY);
        }
        tagByKeyShape(tags, key);
        if (tags.isEmpty()) {
            tags.add(block.solid() ? ContentTag.HEAVY : ContentTag.TRANSPARENT);
        }
        return tags;
    }

    private static EnumSet<ContentTag> deriveEntityTags(String key) {
        EnumSet<ContentTag> tags = EnumSet.noneOf(ContentTag.class);
        if ("voxel:player".equals(key)) {
            tags.add(ContentTag.FRIENDLY);
            return tags;
        }
        if ("voxel:arrow_projectile".equals(key)) {
            tags.add(ContentTag.PROJECTILE);
            tags.add(ContentTag.RANGED);
            return tags;
        }
        tags.add(ContentTag.AMBIENT);
        if (key.contains("firefly") || key.contains("wisp")) {
            tags.add(ContentTag.FLOATY);
            tags.add(ContentTag.NIGHT_VISIBLE);
            tags.add(ContentTag.EMISSIVE);
            tags.add(ContentTag.LIGHT_SOURCE);
        }
        if (key.contains("sheep") || key.contains("bunny") || key.contains("hare") || key.contains("grazer")) {
            tags.add(ContentTag.FRIENDLY);
            tags.add(ContentTag.COZY);
            tags.add(ContentTag.NO_KILL);
            tags.add(ContentTag.CREATURE_RESOURCE);
        }
        if (key.contains("snail")) {
            tags.add(ContentTag.FRIENDLY);
            tags.add(ContentTag.CREATURE_RESOURCE);
        }
        if (key.contains("boar") || key.contains("crawler")) {
            tags.add(ContentTag.ADVENTURE_DANGER);
            tags.add(ContentTag.HEAVY);
        }
        if (key.contains("snow")) {
            tags.add(ContentTag.COLD);
        }
        return tags;
    }

    private static void tagByKeyShape(EnumSet<ContentTag> tags, String key) {
        if (isWoodKey(key)) {
            tags.add(ContentTag.WOOD);
        }
        if (key.contains("stone") || key.contains("pebble") || key.contains("gravel")) {
            tags.add(ContentTag.STONE);
        }
        if (key.contains("clay")) {
            tags.add(ContentTag.CLAY);
        }
        if (key.contains("copper")) {
            tags.add(ContentTag.METAL);
            tags.add(ContentTag.COPPER);
        }
        if (key.contains("iron")) {
            tags.add(ContentTag.METAL);
            tags.add(ContentTag.IRON);
        }
        if (key.contains("crystal")) {
            tags.add(ContentTag.CRYSTAL);
            tags.add(ContentTag.RARE_LOOT);
        }
        if (key.contains("ancient") || key.contains("ruin")) {
            tags.add(ContentTag.ANCIENT);
            tags.add(ContentTag.RUIN_PROGRESSION);
        }
        if (key.contains("bowl") || key.contains("pot")) {
            tags.add(ContentTag.POTTERY);
            tags.add(ContentTag.CONTAINER);
        }
        if (key.contains("water_container")) {
            tags.add(ContentTag.WATER_CONTAINER);
            tags.add(ContentTag.CONTAINER);
        }
        if (key.contains("fiber") || key.contains("rope") || key.contains("cloth") || key.contains("rug") || key.contains("leather")) {
            tags.add(ContentTag.TEXTILE);
        }
        if (key.contains("lantern") || key.contains("rug") || key.contains("table") || key.contains("chair") || key.contains("flower_pot")) {
            tags.add(ContentTag.DECOR);
            tags.add(ContentTag.COZY);
        }
        if (key.contains("moss_clump") || key.contains("slime_drop") || key.contains("feather")) {
            tags.add(ContentTag.CREATURE_RESOURCE);
        }
        if (key.contains("lore") || key.contains("lost_charm")) {
            tags.add(ContentTag.LORE);
        }
    }

    private static boolean isCookedFood(String key) {
        return key.startsWith("voxel:cooked_")
                || key.contains("roasted")
                || key.contains("stew")
                || key.contains("soup")
                || key.contains("jam")
                || key.contains("tea")
                || key.contains("honey");
    }

    private static boolean canLaunchProjectile(ItemType item) {
        String key = item.key();
        return key.contains("bow") || item.toolType() == ToolType.KNIFE || key.endsWith("_knife");
    }

    private static boolean isPlantBlock(String key) {
        return key.contains("grass")
                || key.contains("bloom")
                || key.contains("mushroom")
                || key.contains("cactus")
                || key.contains("leaves")
                || key.contains("bush")
                || key.contains("planter")
                || key.contains("reeds")
                || key.contains("spore");
    }

    private static boolean isWoodKey(String key) {
        return key.contains("log")
                || key.contains("planks")
                || key.contains("wooden")
                || key.contains("stick")
                || key.contains("twig")
                || key.contains("bark")
                || key.contains("stump")
                || key.contains("table")
                || key.contains("chair")
                || key.contains("fence");
    }

    public record CoverageReport(Set<ContentKey> missingKeys, Set<ContentKey> danglingAliases) {
        public CoverageReport {
            missingKeys = Collections.unmodifiableSet(new LinkedHashSet<>(missingKeys));
            danglingAliases = Collections.unmodifiableSet(new LinkedHashSet<>(danglingAliases));
        }

        public boolean ok() {
            return missingKeys.isEmpty() && danglingAliases.isEmpty();
        }
    }

    private static final class Builder {
        private final Map<ContentKey, EnumSet<ContentTag>> tagsByKey = new LinkedHashMap<>();
        private final Map<ContentKey, ContentKey> aliases = new LinkedHashMap<>();

        void tag(ContentKey key, EnumSet<ContentTag> tags) {
            tagsByKey.computeIfAbsent(key, ignored -> EnumSet.noneOf(ContentTag.class)).addAll(tags);
        }

        Set<ContentTag> tagsFor(ContentKey key) {
            EnumSet<ContentTag> tags = tagsByKey.get(key);
            return tags == null ? Set.of() : Collections.unmodifiableSet(tags);
        }

        void alias(ContentKey alias, ContentKey target) {
            aliases.put(alias, target);
        }

        ContentTagRegistry build() {
            List<ContentKey> danglingAliases = new ArrayList<>();
            for (Map.Entry<ContentKey, ContentKey> alias : aliases.entrySet()) {
                if (!tagsByKey.containsKey(alias.getValue())) {
                    danglingAliases.add(alias.getKey());
                }
            }
            if (!danglingAliases.isEmpty()) {
                throw new IllegalStateException("Content tag aliases target unknown keys: " + danglingAliases);
            }
            return new ContentTagRegistry(tagsByKey, aliases);
        }
    }
}
