package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProgressionRouteReport(
        long seed,
        int originX,
        int originZ,
        List<Route> routes,
        List<String> warnings
) {
    private static final int SAMPLE_STEP_BLOCKS = 16;
    private static final int ACTUAL_RESOURCE_SCAN_RADIUS_BLOCKS = 72;
    private static final List<RouteDefinition> ROUTES = List.of(
            new RouteDefinition(
                    "starter_supplies",
                    "Starter supplies",
                    48,
                    Set.of(),
                    blocks(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    Set.of(),
                    "twig/stone/food/herb"
            ),
            new RouteDefinition(
                    "first_food",
                    "First food",
                    72,
                    Set.of(),
                    blocks(Blocks.BERRY_BUSH, Blocks.RED_MUSHROOM, Blocks.MUSHROOM_CLUSTER),
                    Set.of(),
                    "berries/mushrooms"
            ),
            new RouteDefinition(
                    "campfire_storage",
                    "Campfire and storage",
                    96,
                    Set.of(),
                    blocks(Blocks.CAMPFIRE, Blocks.STORAGE_CRATE),
                    Set.of("voxel:campsite", "voxel:compact_village"),
                    "camp/storage"
            ),
            new RouteDefinition(
                    "pine_workbench",
                    "Pine workbench route",
                    280,
                    Set.of("voxel:pine_forest"),
                    blocks(Blocks.PINE_LOG, Blocks.TREE_STUMP, Blocks.RED_MUSHROOM),
                    Set.of("voxel:simple_house", "voxel:small_ruin"),
                    "wood/stumps"
            ),
            new RouteDefinition(
                    "lakeside_cooking",
                    "Lakeside cooking route",
                    300,
                    Set.of("voxel:lakeside"),
                    blocks(Blocks.CLAY, Blocks.CLAY_DEPOSIT, Blocks.REEDS, Blocks.BERRY_BUSH),
                    Set.of("voxel:campsite", "voxel:simple_house"),
                    "clay/reeds"
            ),
            new RouteDefinition(
                    "highlands_forge",
                    "Highlands forge route",
                    440,
                    Set.of("voxel:highlands", "voxel:old_ruins"),
                    blocks(Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE),
                    Set.of("voxel:watchtower", "voxel:small_ruin"),
                    "stone/copper/iron"
            ),
            new RouteDefinition(
                    "ruin_adventure",
                    "Ruin or mushroom adventure route",
                    580,
                    Set.of("voxel:old_ruins", "voxel:mushroom_grove"),
                    blocks(Blocks.GLOW_CRYSTAL_NODE, Blocks.GLOW_MUSHROOM, Blocks.MUSHROOM_CLUSTER, Blocks.ANCIENT_LANTERN),
                    Set.of("voxel:small_ruin", "voxel:watchtower", "voxel:mushroom_circle"),
                    "ruin/glow"
            )
    );

    public ProgressionRouteReport {
        routes = List.copyOf(routes);
        warnings = List.copyOf(warnings);
    }

    public static ProgressionRouteReport aroundSpawn(long seed) {
        OverworldGenerator generator = new OverworldGenerator(seed);
        OverworldGenerator.SpawnPoint spawn = generator.safeSpawnPoint();
        return around(seed, spawn.blockX(), spawn.blockZ());
    }

    public static ProgressionRouteReport around(long seed, int originX, int originZ) {
        OverworldGenerator generator = new OverworldGenerator(seed);
        List<RouteState> states = ROUTES.stream().map(RouteState::new).toList();
        int maxDistance = ROUTES.stream()
                .mapToInt(RouteDefinition::maxDistanceBlocks)
                .max()
                .orElse(0);

        scanBiomeAndProfileResources(generator, originX, originZ, maxDistance, states);
        scanActualSurfaceResources(generator, originX, originZ, states);
        scanGuaranteedStructures(generator, originX, originZ, states);

        List<Route> routes = new ArrayList<>(states.size());
        List<String> warnings = new ArrayList<>();
        for (RouteState state : states) {
            Route route = state.toRoute();
            routes.add(route);
            warnings.addAll(route.warnings());
        }
        return new ProgressionRouteReport(seed, originX, originZ, routes, warnings);
    }

    public Optional<Route> route(String key) {
        Objects.requireNonNull(key, "key");
        return routes.stream().filter(route -> route.key().equals(key)).findFirst();
    }

    public List<String> summaryLines() {
        List<String> lines = new ArrayList<>(routes.size() + 1);
        lines.add("Route report seed " + seed + " origin " + originX + " " + originZ + " warnings " + warnings.size());
        for (Route route : routes) {
            lines.add((route.complete() ? "OK " : "WARN ") + route.key() + " - " + route.bestSummary());
        }
        return lines;
    }

    public String format() {
        return String.join(System.lineSeparator(), summaryLines()) + System.lineSeparator();
    }

    private static void scanBiomeAndProfileResources(OverworldGenerator generator, int originX, int originZ, int maxDistance, List<RouteState> states) {
        for (int dz = -maxDistance; dz <= maxDistance; dz += SAMPLE_STEP_BLOCKS) {
            for (int dx = -maxDistance; dx <= maxDistance; dx += SAMPLE_STEP_BLOCKS) {
                double distance = Math.hypot(dx, dz);
                if (distance > maxDistance) {
                    continue;
                }
                int x = originX + dx;
                int z = originZ + dz;
                String biomeKey = generator.biomeAt(x, z).key();
                Optional<BiomeProgressionProfile> profile = BiomeProgressionCatalog.find(biomeKey);
                for (RouteState state : states) {
                    state.considerBiome(biomeKey, x, z, distance);
                    profile.ifPresent(value -> state.considerProfileResources(value, x, z, distance));
                }
            }
        }
    }

    private static void scanActualSurfaceResources(OverworldGenerator generator, int originX, int originZ, List<RouteState> states) {
        Chunk chunk = new Chunk(new ChunkPos(0, 0), DimensionSettings.OVERWORLD);
        generator.generate(chunk);
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int x = baseX + localX;
                int z = baseZ + localZ;
                double distance = Math.hypot(x - originX, z - originZ);
                if (distance > ACTUAL_RESOURCE_SCAN_RADIUS_BLOCKS) {
                    continue;
                }
                String biomeKey = generator.biomeAt(x, z).key();
                int y = generator.terrainHeight(x, z, generator.biomeAt(x, z)) + 1;
                if (!chunk.dimension().containsY(y)) {
                    continue;
                }
                short blockId = chunk.blockId(x, y, z);
                for (RouteState state : states) {
                    state.considerActualResource(blockId, "surface resource", biomeKey, x, z, distance);
                }
            }
        }
    }

    private static void scanGuaranteedStructures(OverworldGenerator generator, int originX, int originZ, List<RouteState> states) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(new ChunkPos(0, 0));
        chunks.add(new ChunkPos(1, 1));
        for (OverworldGenerator.ProgressionBiomeAnchor anchor : OverworldGenerator.progressionBiomeAnchors()) {
            chunks.add(anchor.chunkPos());
        }
        for (ChunkPos pos : chunks) {
            generator.structureAtChunk(pos).ifPresent(structure -> {
                String structureKey = structure.template().key();
                double distance = Math.hypot(structure.originX() - originX, structure.originZ() - originZ);
                for (RouteState state : states) {
                    state.considerStructure(structureKey, structure.originX(), structure.originZ(), distance);
                }
            });
        }
    }

    private static Set<Short> blocks(short... blockIds) {
        LinkedHashSet<Short> blocks = new LinkedHashSet<>();
        for (short blockId : blockIds) {
            blocks.add(blockId);
        }
        return Set.copyOf(blocks);
    }

    public record Route(
            String key,
            String label,
            int maxDistanceBlocks,
            Optional<RouteAnchor> nearestBiome,
            Optional<RouteAnchor> nearestResource,
            Optional<RouteAnchor> nearestStructure,
            List<String> warnings
    ) {
        public Route {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            nearestBiome = nearestBiome == null ? Optional.empty() : nearestBiome;
            nearestResource = nearestResource == null ? Optional.empty() : nearestResource;
            nearestStructure = nearestStructure == null ? Optional.empty() : nearestStructure;
            warnings = List.copyOf(warnings);
        }

        public boolean complete() {
            return warnings.isEmpty();
        }

        public String bestSummary() {
            RouteAnchor best = nearestResource.or(() -> nearestBiome).or(() -> nearestStructure).orElse(null);
            if (best == null) {
                return "missing within " + maxDistanceBlocks + "m";
            }
            return best.label() + " " + Math.round(best.distanceBlocks()) + "m " + best.distanceBand().label();
        }
    }

    public record RouteAnchor(String key, String label, RouteAnchorKind kind, int x, int z, double distanceBlocks) {
        public RouteAnchor {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(kind, "kind");
            if (!Double.isFinite(distanceBlocks) || distanceBlocks < 0.0) {
                throw new IllegalArgumentException("Route anchor distance must be finite and non-negative");
            }
        }

        public DistanceBand distanceBand() {
            return DistanceBand.fromDistance(distanceBlocks);
        }
    }

    public enum RouteAnchorKind {
        BIOME,
        RESOURCE,
        STRUCTURE
    }

    public enum DistanceBand {
        SPAWN("spawn", 64),
        SHORT("short", 128),
        EARLY("early", 300),
        MID("mid", 460),
        ADVENTURE("adventure", 640),
        FAR("far", Integer.MAX_VALUE);

        private final String label;
        private final int maxDistanceBlocks;

        DistanceBand(String label, int maxDistanceBlocks) {
            this.label = label;
            this.maxDistanceBlocks = maxDistanceBlocks;
        }

        public String label() {
            return label;
        }

        public static DistanceBand fromDistance(double distanceBlocks) {
            for (DistanceBand band : values()) {
                if (distanceBlocks <= band.maxDistanceBlocks) {
                    return band;
                }
            }
            return FAR;
        }
    }

    private record RouteDefinition(
            String key,
            String label,
            int maxDistanceBlocks,
            Set<String> biomeKeys,
            Set<Short> resourceBlockIds,
            Set<String> structureKeys,
            String resourceLabel
    ) {
        RouteDefinition {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            biomeKeys = Set.copyOf(biomeKeys);
            resourceBlockIds = Set.copyOf(resourceBlockIds);
            structureKeys = Set.copyOf(structureKeys);
            Objects.requireNonNull(resourceLabel, "resourceLabel");
            if (key.isBlank() || label.isBlank() || maxDistanceBlocks <= 0 || resourceLabel.isBlank()) {
                throw new IllegalArgumentException("Invalid progression route definition");
            }
        }

        private boolean acceptsBiome(String biomeKey) {
            return biomeKeys.contains(biomeKey);
        }

        private boolean acceptsResource(short blockId) {
            return resourceBlockIds.contains(blockId);
        }

        private boolean acceptsStructure(String structureKey) {
            return structureKeys.contains(structureKey);
        }

        private boolean needsBiome() {
            return !biomeKeys.isEmpty();
        }

        private boolean needsResourceOrStructure() {
            return !resourceBlockIds.isEmpty() || !structureKeys.isEmpty();
        }
    }

    private static final class RouteState {
        private final RouteDefinition definition;
        private RouteAnchor nearestBiome;
        private RouteAnchor nearestResource;
        private RouteAnchor nearestStructure;

        private RouteState(RouteDefinition definition) {
            this.definition = definition;
        }

        private void considerBiome(String biomeKey, int x, int z, double distance) {
            if (distance > definition.maxDistanceBlocks() || !definition.acceptsBiome(biomeKey)) {
                return;
            }
            RouteAnchor anchor = new RouteAnchor(biomeKey, biomeKey, RouteAnchorKind.BIOME, x, z, distance);
            nearestBiome = nearest(anchor, nearestBiome);
        }

        private void considerProfileResources(BiomeProgressionProfile profile, int x, int z, double distance) {
            if (distance > definition.maxDistanceBlocks()) {
                return;
            }
            if (definition.needsBiome() && !definition.acceptsBiome(profile.biomeKey())) {
                return;
            }
            for (short blockId : profile.resourceBlockIds()) {
                if (definition.acceptsResource(blockId)) {
                    considerResource(blockId, definition.resourceLabel() + " in " + profile.biomeKey(), x, z, distance);
                    return;
                }
            }
        }

        private void considerActualResource(short blockId, String label, String biomeKey, int x, int z, double distance) {
            if (definition.needsBiome() && !definition.acceptsBiome(biomeKey)) {
                return;
            }
            considerResource(blockId, label, x, z, distance);
        }

        private void considerResource(short blockId, String label, int x, int z, double distance) {
            if (distance > definition.maxDistanceBlocks() || !definition.acceptsResource(blockId)) {
                return;
            }
            RouteAnchor anchor = new RouteAnchor(Short.toUnsignedInt(blockId) + "", label, RouteAnchorKind.RESOURCE, x, z, distance);
            nearestResource = nearest(anchor, nearestResource);
        }

        private void considerStructure(String structureKey, int x, int z, double distance) {
            if (distance > definition.maxDistanceBlocks() || !definition.acceptsStructure(structureKey)) {
                return;
            }
            RouteAnchor anchor = new RouteAnchor(structureKey, structureKey, RouteAnchorKind.STRUCTURE, x, z, distance);
            nearestStructure = nearest(anchor, nearestStructure);
        }

        private Route toRoute() {
            List<String> warnings = new ArrayList<>();
            if (definition.needsBiome() && nearestBiome == null) {
                warnings.add(definition.key() + " missing biome " + String.join("/", definition.biomeKeys()) + " within " + definition.maxDistanceBlocks() + "m");
            }
            if (definition.needsResourceOrStructure() && nearestResource == null && nearestStructure == null) {
                warnings.add(definition.key() + " missing resource or structure within " + definition.maxDistanceBlocks() + "m");
            }
            return new Route(
                    definition.key(),
                    definition.label(),
                    definition.maxDistanceBlocks(),
                    Optional.ofNullable(nearestBiome),
                    Optional.ofNullable(nearestResource),
                    Optional.ofNullable(nearestStructure),
                    warnings
            );
        }

        private static RouteAnchor nearest(RouteAnchor candidate, RouteAnchor current) {
            if (current == null || candidate.distanceBlocks() < current.distanceBlocks()) {
                return candidate;
            }
            return current;
        }
    }
}
