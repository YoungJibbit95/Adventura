package dev.voxelgame.common.gameplay;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CozyLifeProgression {
    public static final String COZY_SHEEP = "voxel:cozy_sheep";
    public static final String FOREST_BUNNY = "voxel:forest_bunny";
    public static final String MOSS_SNAIL = "voxel:moss_snail";
    public static final String FIREFLY_SWARM = "voxel:firefly_swarm";
    public static final String LITTLE_BOAR = "voxel:little_boar";

    private static final List<CreatureDesign> DEFAULT_CREATURES = List.of(
            creature(
                    COZY_SHEEP,
                    1,
                    "Cozy Sheep",
                    CreatureDisposition.FRIENDLY,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.FRIENDSHIP, CreatureRole.BASE_COMFORT, CreatureRole.RESOURCE),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:berries", "voxel:wild_herbs"),
                    List.of("voxel:cloth", "voxel:woven_rug"),
                    List.of("voxel:cloth"),
                    2,
                    2,
                    "Feed berries or herbs once per short server cooldown; no forced breeding loop in Alpha.",
                    "Two soft trust steps unlock observation/comfort feedback, not a required quest chain.",
                    "Pet/observe/shear hooks stay server-authoritative and never require killing.",
                    "Idle, graze, look-at-player and gentle flee blend are the minimum animation states.",
                    "Entity snapshot state uses idle/graze/flee plus optional trust tier and cooldown.",
                    "Persist per-player trust tier and last-feed timestamp only when friendship is touched.",
                    "Show a small cozy-source hint near camp or meadow, never a chore prompt."
            ),
            creature(
                    FOREST_BUNNY,
                    2,
                    "Forest Bunny",
                    CreatureDisposition.SKITTISH,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.HINT_GIVER, CreatureRole.FRIENDSHIP),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:pine_forest"),
                    List.of("voxel:berries", "voxel:wild_herbs"),
                    List.of("voxel:wild_herbs", "voxel:dry_grass"),
                    List.of(),
                    0,
                    1,
                    "Feed only as a small lure; repeated feeding should be ignored during cooldown.",
                    "One trust step may reduce flee distance and point toward herbs.",
                    "Observe/follow-hint interaction, no drops and no combat reward.",
                    "Idle, hop, flee and nibble states need distinct readable silhouettes.",
                    "Entity snapshot state uses idle/flee plus optional hint target marker.",
                    "Persist discovered/trusted flag only; no population or breeding save burden.",
                    "Use a quick rustle/hint ping toward herbs without tutorial copy."
            ),
            creature(
                    FIREFLY_SWARM,
                    3,
                    "Firefly Swarm",
                    CreatureDisposition.AMBIENT_SWARM,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.HINT_GIVER, CreatureRole.BASE_COMFORT),
                    List.of("voxel:cozy_meadow", "voxel:lakeside", "voxel:mushroom_grove", "voxel:mire"),
                    List.of(),
                    List.of("voxel:glow_crystal", "voxel:glow_mushroom_cap"),
                    List.of(),
                    1,
                    0,
                    "Not feedable; attraction can later come from placed lights or flowers.",
                    "No friendship track; discovery is enough.",
                    "Observe-only night cue that can point at glow resources or safe camp light.",
                    "Swarm orbit, pulse and drift states must remain readable at night.",
                    "Entity snapshot state uses wander plus optional glow intensity bucket.",
                    "Persist only journal discovery or structure-linked spawned markers if needed.",
                    "Show a one-shot night-visible journal hint when first discovered."
            ),
            creature(
                    MOSS_SNAIL,
                    4,
                    "Moss Snail",
                    CreatureDisposition.FRIENDLY,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.RESOURCE, CreatureRole.FRIENDSHIP),
                    List.of("voxel:lakeside", "voxel:mushroom_grove", "voxel:mire"),
                    List.of("voxel:mushroom", "voxel:glow_mushroom_cap"),
                    List.of("voxel:moss_clump", "voxel:slime_drop"),
                    List.of("voxel:moss_clump", "voxel:slime_drop"),
                    0,
                    2,
                    "Feed mushrooms slowly; server cooldown prevents farming loops.",
                    "Two trust steps can enable gentle harvest hints, capped per snail.",
                    "Resource collection must prefer peaceful harvest; death drops stay low and deterministic.",
                    "Idle, crawl, retract and content states are enough for Alpha readability.",
                    "Entity snapshot state uses idle plus optional retract/content state and cooldown.",
                    "Persist trust/cooldown for named or nearby base snails only.",
                    "Show resource as a cozy curiosity, not a grindable dispenser."
            ),
            creature(
                    LITTLE_BOAR,
                    5,
                    "Little Boar",
                    CreatureDisposition.NEUTRAL,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.HINT_GIVER, CreatureRole.RESOURCE, CreatureRole.RARE_DANGER),
                    List.of("voxel:pine_forest", "voxel:mushroom_grove"),
                    List.of("voxel:mushroom"),
                    List.of("voxel:mushroom", "voxel:leather_strip"),
                    List.of("voxel:leather_strip"),
                    0,
                    1,
                    "Mushrooms lure briefly; feeding must not convert boars into livestock.",
                    "One respect/calm step may reduce startle radius, not grant ownership.",
                    "Neutral shove/flee behavior, optional leather source only through careful balance.",
                    "Root, sniff, startle and short charge tells are required before any damage.",
                    "Entity snapshot state uses idle/wander/flee plus optional agitated flag.",
                    "Persist only rare calm/cooldown facts if the server promotes an individual.",
                    "Warn with body language first; combat is never the main reward."
            ),
            creature(
                    "voxel:snow_hare",
                    6,
                    "Snow Hare",
                    CreatureDisposition.SKITTISH,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.HINT_GIVER, CreatureRole.FRIENDSHIP),
                    List.of("voxel:frost_peaks", "voxel:highlands"),
                    List.of("voxel:wild_herbs", "voxel:dry_grass"),
                    List.of("voxel:wild_herbs", "voxel:raw_iron"),
                    List.of(),
                    0,
                    1,
                    "Tiny lure interaction only; cold-biome pacing should stay exploration-first.",
                    "One trust step can make the hare pause near useful terrain.",
                    "Observe/flee hint behavior, no drops.",
                    "Idle, hop, snow-pause and flee states need clear direction changes.",
                    "Entity snapshot state uses idle/flee plus optional hint target marker.",
                    "Persist discovered/trusted flag only.",
                    "Use a light footprint/hint cue in cold regions."
            ),
            creature(
                    "voxel:mire_wisp",
                    7,
                    "Mire Wisp",
                    CreatureDisposition.CAUTIOUS,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.HINT_GIVER, CreatureRole.BASE_COMFORT),
                    List.of("voxel:mire", "voxel:mushroom_grove", "voxel:lakeside"),
                    List.of(),
                    List.of("voxel:glow_crystal", "voxel:spore_blossom"),
                    List.of(),
                    1,
                    0,
                    "Not feedable; future attraction can come from ancient lanterns.",
                    "No friendship track in Alpha.",
                    "Observe from distance; may lead toward risky terrain but should not ambush.",
                    "Pulse, hover, drift-away and flare-warning states are required.",
                    "Entity snapshot state uses wander plus warning/glow bucket.",
                    "Persist only discovery and any structure-linked marker state.",
                    "Give a soft warning tint before the player follows it into risk."
            ),
            creature(
                    "voxel:dune_crawler",
                    8,
                    "Dune Crawler",
                    CreatureDisposition.HOSTILE,
                    roles(CreatureRole.RARE_DANGER, CreatureRole.RESOURCE),
                    List.of("voxel:sun_dunes"),
                    List.of(),
                    List.of("voxel:raw_copper", "voxel:leather_strip"),
                    List.of("voxel:leather_strip"),
                    0,
                    0,
                    "Not feedable.",
                    "No friendship track.",
                    "Rare telegraphed danger for later adventure biomes; avoid spawn and cozy bases.",
                    "Burrow, emerge, crawl and attack-windup states need long readable tells.",
                    "Entity snapshot state uses idle/wander/attack plus aggro target id server-side.",
                    "Persist only if spawned from a saved encounter marker.",
                    "Use warning sound/particles before contact damage."
            ),
            creature(
                    "voxel:forest_grazer",
                    9,
                    "Forest Grazer",
                    CreatureDisposition.FRIENDLY,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.BASE_COMFORT, CreatureRole.RESOURCE, CreatureRole.FRIENDSHIP),
                    List.of("voxel:pine_forest", "voxel:skyroot_forest"),
                    List.of("voxel:berries", "voxel:wild_herbs"),
                    List.of("voxel:cloth", "voxel:resin"),
                    List.of("voxel:cloth"),
                    1,
                    2,
                    "Feed herbs/berries as a calm interaction with server cooldown.",
                    "Two trust steps can make grazers linger near a base edge.",
                    "Peaceful resource hook only; no kill incentive.",
                    "Graze, walk, look and calm states are sufficient.",
                    "Entity snapshot state uses graze/wander plus optional trust tier.",
                    "Persist per-player trust tier and last-feed timestamp only when touched.",
                    "Surface as a comfort presence near forest bases."
            ),
            creature(
                    "voxel:meadow_grazer",
                    10,
                    "Meadow Grazer",
                    CreatureDisposition.FRIENDLY,
                    roles(CreatureRole.ATMOSPHERE, CreatureRole.BASE_COMFORT, CreatureRole.FRIENDSHIP),
                    List.of("voxel:meadow", "voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:berries", "voxel:wild_herbs"),
                    List.of("voxel:berries", "voxel:wild_herbs"),
                    List.of(),
                    2,
                    2,
                    "Feed herbs/berries only for calm/follow moments; avoid livestock loops.",
                    "Two trust steps can improve base ambience feedback.",
                    "Pet/observe/follow-near-base hooks, no drops.",
                    "Graze, idle, follow-softly and flee states are enough.",
                    "Entity snapshot state uses graze/wander/flee plus optional trust tier.",
                    "Persist per-player trust tier and last-feed timestamp only when touched.",
                    "Let the player read meadow safety through behavior, not instructions."
            )
    );

    private static final Map<String, CreatureDesign> CREATURES_BY_KEY = creaturesByKey(DEFAULT_CREATURES);
    private static final Map<CreatureRole, List<CreatureDesign>> CREATURES_BY_ROLE = creaturesByRole(DEFAULT_CREATURES);

    private CozyLifeProgression() {
    }

    public static List<CreatureDesign> defaultCreatures() {
        return DEFAULT_CREATURES;
    }

    public static Optional<CreatureDesign> findCreature(String entityKey) {
        return Optional.ofNullable(CREATURES_BY_KEY.get(entityKey));
    }

    public static List<CreatureDesign> creaturesWithRole(CreatureRole role) {
        return CREATURES_BY_ROLE.getOrDefault(role, List.of());
    }

    public static Set<String> coreAlphaCreatureKeys() {
        return Set.of(COZY_SHEEP, FOREST_BUNNY, MOSS_SNAIL, FIREFLY_SWARM, LITTLE_BOAR);
    }

    public static Set<String> feedableCreatureKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (CreatureDesign creature : DEFAULT_CREATURES) {
            if (creature.feedable()) {
                keys.add(creature.entityKey());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    private static CreatureDesign creature(
            String entityKey,
            int order,
            String displayName,
            CreatureDisposition disposition,
            Set<CreatureRole> roles,
            List<String> biomeKeys,
            List<String> favoriteItemKeys,
            List<String> hintTargetKeys,
            List<String> resourceItemKeys,
            int comfortContribution,
            int friendshipSteps,
            String feedingContract,
            String friendshipContract,
            String interactionContract,
            String animationContract,
            String networkStateContract,
            String saveStateContract,
            String uiFeedback
    ) {
        return new CreatureDesign(
                entityKey,
                order,
                displayName,
                disposition,
                roles,
                biomeKeys,
                favoriteItemKeys,
                hintTargetKeys,
                resourceItemKeys,
                comfortContribution,
                friendshipSteps,
                feedingContract,
                friendshipContract,
                interactionContract,
                animationContract,
                networkStateContract,
                saveStateContract,
                uiFeedback
        );
    }

    private static Set<CreatureRole> roles(CreatureRole first, CreatureRole... rest) {
        EnumSet<CreatureRole> roles = EnumSet.of(first, rest);
        return Collections.unmodifiableSet(roles);
    }

    private static Map<String, CreatureDesign> creaturesByKey(List<CreatureDesign> creatures) {
        Map<String, CreatureDesign> byKey = new LinkedHashMap<>();
        for (CreatureDesign creature : creatures) {
            CreatureDesign duplicate = byKey.put(creature.entityKey(), creature);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate creature design key " + creature.entityKey());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }

    private static Map<CreatureRole, List<CreatureDesign>> creaturesByRole(List<CreatureDesign> creatures) {
        Map<CreatureRole, List<CreatureDesign>> byRole = new EnumMap<>(CreatureRole.class);
        for (CreatureRole role : CreatureRole.values()) {
            byRole.put(role, creatures.stream()
                    .filter(creature -> creature.roles().contains(role))
                    .toList());
        }
        return Collections.unmodifiableMap(byRole);
    }
}
