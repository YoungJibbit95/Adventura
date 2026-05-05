package dev.voxelgame.common.world.structure;

import dev.voxelgame.common.gameplay.JournalProgression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StructureCatalog {
    private static final List<StructureCatalogEntry> DEFAULT_ENTRIES = List.of(
            entry(
                    Structures.campsite(),
                    "voxel:palette_campsite",
                    StructureRotation.cardinal(),
                    List.of("voxel:campsite_crate"),
                    "",
                    JournalProgression.SPAWN_CAMPSITE
            ),
            entry(
                    Structures.simpleHouse(),
                    "voxel:palette_skyroot_home",
                    StructureRotation.cardinal(),
                    List.of(),
                    "",
                    JournalProgression.SIMPLE_HOUSE
            ),
            entry(
                    Structures.smallRuin(),
                    "voxel:palette_old_ruin",
                    StructureRotation.cardinal(),
                    List.of("voxel:ruin_crate", "voxel:ruin_rare_crate"),
                    "voxel:little_boar",
                    JournalProgression.SMALL_RUIN
            ),
            entry(
                    Structures.watchtower(),
                    "voxel:palette_watchtower",
                    StructureRotation.cardinal(),
                    List.of(),
                    "voxel:little_boar",
                    JournalProgression.WATCHTOWER
            ),
            entry(
                    Structures.mushroomCircle(),
                    "voxel:palette_mushroom_circle",
                    StructureRotation.noneOnly(),
                    List.of(),
                    "voxel:firefly_swarm",
                    JournalProgression.MUSHROOM_CIRCLE
            ),
            entry(
                    Structures.compactVillage(),
                    "voxel:palette_cozy_village",
                    StructureRotation.cardinal(),
                    List.of("voxel:village_house_crate"),
                    "voxel:villager_spawn",
                    JournalProgression.COMPACT_VILLAGE
            ),
            entry(
                    Structures.desertWell(),
                    "voxel:palette_desert_well",
                    StructureRotation.cardinal(),
                    List.of(),
                    "voxel:dune_crawler",
                    JournalProgression.DESERT_WELL
            )
    );
    private static final Map<String, StructureCatalogEntry> ENTRIES_BY_KEY = entriesByKey(DEFAULT_ENTRIES);

    private StructureCatalog() {
    }

    public static List<StructureCatalogEntry> defaultEntries() {
        return DEFAULT_ENTRIES;
    }

    public static Optional<StructureCatalogEntry> find(String structureKey) {
        return Optional.ofNullable(ENTRIES_BY_KEY.get(structureKey));
    }

    public static Set<String> templateKeys() {
        return ENTRIES_BY_KEY.keySet();
    }

    private static StructureCatalogEntry entry(
            StructureTemplate template,
            String paletteKey,
            List<StructureRotation> allowedRotations,
            List<String> lootTableKeys,
            String encounterTableKey,
            String journalKey
    ) {
        return StructureCatalogEntry.of(template, paletteKey, allowedRotations, lootTableKeys, encounterTableKey, journalKey);
    }

    private static Map<String, StructureCatalogEntry> entriesByKey(List<StructureCatalogEntry> entries) {
        Map<String, StructureCatalogEntry> byKey = new LinkedHashMap<>();
        for (StructureCatalogEntry entry : entries) {
            StructureCatalogEntry duplicate = byKey.put(entry.key(), entry);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate structure catalog entry " + entry.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }
}
