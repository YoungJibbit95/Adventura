package dev.voxelgame.common.world.structure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record StructureCatalogEntry(
        String key,
        StructureTemplate template,
        StructureBounds bounds,
        String paletteKey,
        List<StructureRotation> allowedRotations,
        List<String> lootTableKeys,
        String encounterTableKey,
        String journalKey
) {
    public StructureCatalogEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(paletteKey, "paletteKey");
        Objects.requireNonNull(allowedRotations, "allowedRotations");
        Objects.requireNonNull(lootTableKeys, "lootTableKeys");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Structure key must not be blank");
        }
        if (!key.equals(template.key())) {
            throw new IllegalArgumentException("Structure catalog key must match template key: " + key);
        }
        if (paletteKey.isBlank()) {
            throw new IllegalArgumentException("Structure palette key must not be blank: " + key);
        }
        allowedRotations = List.copyOf(allowedRotations);
        if (allowedRotations.isEmpty()) {
            throw new IllegalArgumentException("Structure needs at least one allowed rotation: " + key);
        }
        lootTableKeys = normalizeKeys(lootTableKeys, "loot table", key);
        encounterTableKey = encounterTableKey == null ? "" : encounterTableKey;
        journalKey = journalKey == null ? "" : journalKey;

        for (BlockPlacement block : template.blocks()) {
            if (!bounds.contains(block)) {
                throw new IllegalArgumentException("Structure block outside bounds: " + key);
            }
        }
        Set<String> markerIds = new LinkedHashSet<>();
        for (StructureMarker marker : template.markers()) {
            if (!bounds.contains(marker)) {
                throw new IllegalArgumentException("Structure marker outside bounds: " + key);
            }
            if (!markerIds.add(markerId(marker))) {
                throw new IllegalArgumentException("Duplicate structure marker id in " + key + ": " + markerId(marker));
            }
        }
        for (StructureMarker marker : template.lootMarkers()) {
            if (!lootTableKeys.contains(marker.key())) {
                throw new IllegalArgumentException("Loot marker lacks catalog loot table in " + key + ": " + marker.key());
            }
        }
    }

    public static StructureCatalogEntry of(
            StructureTemplate template,
            String paletteKey,
            List<StructureRotation> allowedRotations,
            List<String> lootTableKeys,
            String encounterTableKey,
            String journalKey
    ) {
        return new StructureCatalogEntry(
                template.key(),
                template,
                StructureBounds.fromTemplate(template),
                paletteKey,
                allowedRotations,
                lootTableKeys,
                encounterTableKey,
                journalKey
        );
    }

    public String primaryLootTableKey() {
        return lootTableKeys.isEmpty() ? "" : lootTableKeys.get(0);
    }

    public Set<String> markerIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (StructureMarker marker : template.markers()) {
            ids.add(markerId(marker));
        }
        return Set.copyOf(ids);
    }

    public static String markerId(StructureMarker marker) {
        Objects.requireNonNull(marker, "marker");
        return marker.type() + ":" + marker.key() + "@" + marker.x() + "," + marker.y() + "," + marker.z();
    }

    private static List<String> normalizeKeys(List<String> keys, String label, String structureKey) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Blank " + label + " key in " + structureKey);
            }
            normalized.add(key);
        }
        return List.copyOf(normalized);
    }
}
