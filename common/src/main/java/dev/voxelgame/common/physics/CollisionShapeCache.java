package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.DimensionSettings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CollisionShapeCache {
    private final Source source;
    private final Map<ShapeSet, Map<SectionKey, SectionShapes>> sections = new EnumMap<>(ShapeSet.class);

    public CollisionShapeCache(Source source) {
        this.source = Objects.requireNonNull(source, "source");
        for (ShapeSet set : ShapeSet.values()) {
            sections.put(set, new LinkedHashMap<>());
        }
    }

    public synchronized CollisionCheck collidesPlayer(double eyeX, double eyeY, double eyeZ, PlayerBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (!PhysicsNumericGuard.allFinite(eyeX, eyeY, eyeZ)) {
            return CollisionCheck.colliding();
        }
        double minX = bounds.minX(eyeX);
        double maxX = bounds.maxX(eyeX);
        double minY = bounds.minY(eyeY);
        double maxY = bounds.maxY(eyeY);
        double minZ = bounds.minZ(eyeZ);
        double maxZ = bounds.maxZ(eyeZ);
        if (outsideDimension(minY, maxY)) {
            return CollisionCheck.colliding();
        }
        return firstMatching(ShapeSet.MOVEMENT, minX, minY, minZ, maxX, maxY, maxZ, entry ->
                bounds.intersectsBlock(eyeX, eyeY, eyeZ, entry.x(), entry.y(), entry.z())
                        && entry.shape().intersectsPlayer(bounds, eyeX, eyeY, eyeZ, entry.x(), entry.y(), entry.z()));
    }

    public synchronized CollisionCheck collidesEntity(
            EntityBounds bounds,
            double centerX,
            double baseY,
            double centerZ
    ) {
        Objects.requireNonNull(bounds, "bounds");
        if (!PhysicsNumericGuard.allFinite(centerX, baseY, centerZ)) {
            return CollisionCheck.colliding();
        }
        double minX = bounds.minX(centerX);
        double maxX = bounds.maxX(centerX);
        double minY = bounds.minY(baseY);
        double maxY = bounds.maxY(baseY);
        double minZ = bounds.minZ(centerZ);
        double maxZ = bounds.maxZ(centerZ);
        if (outsideDimension(minY, maxY)) {
            return CollisionCheck.colliding();
        }
        return firstMatching(ShapeSet.MOVEMENT, minX, minY, minZ, maxX, maxY, maxZ, entry ->
                bounds.intersectsBlock(centerX, baseY, centerZ, entry.x(), entry.y(), entry.z())
                        && entry.shape().intersectsAabb(
                        bounds.minX(centerX),
                        bounds.minY(baseY),
                        bounds.minZ(centerZ),
                        bounds.maxX(centerX),
                        bounds.maxY(baseY),
                        bounds.maxZ(centerZ),
                        entry.x(),
                        entry.y(),
                        entry.z()));
    }

    public synchronized CollisionCheck collidesProjectile(double x, double y, double z, ProjectileBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (!PhysicsNumericGuard.allFinite(x, y, z)) {
            return CollisionCheck.colliding();
        }
        double radius = bounds.radius();
        double minX = x - radius;
        double maxX = x + radius;
        double minY = y - radius;
        double maxY = y + radius;
        double minZ = z - radius;
        double maxZ = z + radius;
        if (outsideDimension(minY, maxY)) {
            return CollisionCheck.colliding();
        }
        return firstMatching(ShapeSet.PROJECTILE, minX, minY, minZ, maxX, maxY, maxZ, entry ->
                bounds.intersectsBlock(x, y, z, entry.x(), entry.y(), entry.z())
                        && entry.shape().intersectsProjectile(bounds, x, y, z, entry.x(), entry.y(), entry.z()));
    }

    public synchronized Optional<PartialShapeImpactResolver.ImpactResult> projectileImpact(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            ProjectileBounds bounds
    ) {
        Objects.requireNonNull(bounds, "bounds");
        if (!PhysicsNumericGuard.allFinite(fromX, fromY, fromZ, toX, toY, toZ)) {
            return Optional.of(PartialShapeImpactResolver.blocking(0, 0, 0, 0.0, 0.0, 0.0, ProjectileHit.BlockFace.NONE, 0.0));
        }
        double radius = bounds.radius();
        double minX = Math.min(fromX, toX) - radius;
        double maxX = Math.max(fromX, toX) + radius;
        double minY = Math.min(fromY, toY) - radius;
        double maxY = Math.max(fromY, toY) + radius;
        double minZ = Math.min(fromZ, toZ) - radius;
        double maxZ = Math.max(fromZ, toZ) + radius;
        if (outsideDimension(minY, maxY)) {
            return Optional.of(PartialShapeImpactResolver.blocking(
                    floor(toX),
                    clampY(floor(toY)),
                    floor(toZ),
                    toX,
                    toY,
                    toZ,
                    ProjectileHit.BlockFace.NONE,
                    1.0
            ));
        }

        ProjectileImpactSearch search = new ProjectileImpactSearch();
        CollisionCheck check = forEachEntry(ShapeSet.PROJECTILE, minX, minY, minZ, maxX, maxY, maxZ, entry -> {
            Optional<PartialShapeImpactResolver.ImpactResult> hit = entry.shape()
                    .raycastProjectile(fromX, fromY, fromZ, toX, toY, toZ, bounds, entry.x(), entry.y(), entry.z());
            hit.ifPresent(search::accept);
            return false;
        });
        if (check.blockedByMissingChunk()) {
            return Optional.of(PartialShapeImpactResolver.blocking(floor(toX), floor(toY), floor(toZ), toX, toY, toZ, ProjectileHit.BlockFace.NONE, 1.0));
        }
        return search.best();
    }

    public synchronized List<ShapeBounds> partialMovementShapeBoundsAround(int centerX, int centerY, int centerZ, int radiusBlocks) {
        int radius = Math.max(0, radiusBlocks);
        List<ShapeBounds> bounds = new ArrayList<>();
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minY = Math.max(source.dimension().minY(), centerY - radius);
        int maxY = Math.min(source.dimension().maxYExclusive() - 1, centerY + radius);
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        int minChunkX = Math.floorDiv(minX, ChunkPos.SIZE);
        int maxChunkX = Math.floorDiv(maxX, ChunkPos.SIZE);
        int minChunkZ = Math.floorDiv(minZ, ChunkPos.SIZE);
        int maxChunkZ = Math.floorDiv(maxZ, ChunkPos.SIZE);
        int minSectionY = Math.floorDiv(minY, ChunkSection.SIZE);
        int maxSectionY = Math.floorDiv(maxY, ChunkSection.SIZE);

        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                if (!source.ensureChunkAvailable(pos)) {
                    continue;
                }
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    if (!sectionOverlapsDimension(sectionY)) {
                        continue;
                    }
                    for (Entry entry : section(ShapeSet.MOVEMENT, pos, sectionY).entries()) {
                        if (entry.x() < minX || entry.x() > maxX
                                || entry.y() < minY || entry.y() > maxY
                                || entry.z() < minZ || entry.z() > maxZ
                                || entry.shape().empty()
                                || entry.shape() == BlockCollisionShape.FULL) {
                            continue;
                        }
                        for (BlockCollisionShape.Box box : entry.shape().boxes()) {
                            bounds.add(new ShapeBounds(
                                    entry.x() + box.minX(),
                                    entry.y() + box.minY(),
                                    entry.z() + box.minZ(),
                                    entry.x() + box.maxX(),
                                    entry.y() + box.maxY(),
                                    entry.z() + box.maxZ()
                            ));
                        }
                    }
                }
            }
        }
        return bounds;
    }

    public synchronized void invalidateBlock(int x, int y, int z) {
        if (!source.dimension().containsY(y)) {
            return;
        }
        ChunkPos pos = ChunkPos.fromBlock(x, z);
        int sectionY = Math.floorDiv(y, ChunkSection.SIZE);
        for (Map<SectionKey, SectionShapes> cache : sections.values()) {
            cache.remove(new SectionKey(pos, sectionY));
        }
    }

    public synchronized void invalidateChunk(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos");
        for (Map<SectionKey, SectionShapes> cache : sections.values()) {
            cache.keySet().removeIf(key -> key.pos().equals(pos));
        }
    }

    public synchronized CacheStats stats() {
        int sectionCount = 0;
        int shapeCount = 0;
        for (Map<SectionKey, SectionShapes> cache : sections.values()) {
            sectionCount += cache.size();
            for (SectionShapes section : cache.values()) {
                shapeCount += section.entries().size();
            }
        }
        return new CacheStats(sectionCount, shapeCount);
    }

    private CollisionCheck firstMatching(
            ShapeSet set,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            EntryPredicate predicate
    ) {
        return forEachEntry(set, minX, minY, minZ, maxX, maxY, maxZ, predicate);
    }

    private CollisionCheck forEachEntry(
            ShapeSet set,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            EntryPredicate predicate
    ) {
        int minBlockX = floor(minX);
        int maxBlockX = floor(maxX);
        int minBlockY = floor(minY);
        int maxBlockY = floor(maxY);
        int minBlockZ = floor(minZ);
        int maxBlockZ = floor(maxZ);
        int minChunkX = Math.floorDiv(minBlockX, ChunkPos.SIZE);
        int maxChunkX = Math.floorDiv(maxBlockX, ChunkPos.SIZE);
        int minChunkZ = Math.floorDiv(minBlockZ, ChunkPos.SIZE);
        int maxChunkZ = Math.floorDiv(maxBlockZ, ChunkPos.SIZE);
        int minSectionY = Math.floorDiv(minBlockY, ChunkSection.SIZE);
        int maxSectionY = Math.floorDiv(maxBlockY, ChunkSection.SIZE);

        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                if (!source.ensureChunkAvailable(pos)) {
                    return CollisionCheck.missingChunk();
                }
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    if (!sectionOverlapsDimension(sectionY)) {
                        continue;
                    }
                    for (Entry entry : section(set, pos, sectionY).entries()) {
                        if (entry.x() < minBlockX || entry.x() > maxBlockX
                                || entry.y() < minBlockY || entry.y() > maxBlockY
                                || entry.z() < minBlockZ || entry.z() > maxBlockZ) {
                            continue;
                        }
                        if (predicate.matches(entry)) {
                            return CollisionCheck.colliding();
                        }
                    }
                }
            }
        }
        return CollisionCheck.clear();
    }

    private SectionShapes section(ShapeSet set, ChunkPos pos, int sectionY) {
        SectionKey key = new SectionKey(pos, sectionY);
        return sections.get(set).computeIfAbsent(key, ignored -> buildSection(set, pos, sectionY));
    }

    private SectionShapes buildSection(ShapeSet set, ChunkPos pos, int sectionY) {
        int baseX = pos.x() * ChunkPos.SIZE;
        int baseY = sectionY * ChunkSection.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        List<Entry> entries = new ArrayList<>();
        for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
            int y = baseY + localY;
            if (!source.dimension().containsY(y)) {
                continue;
            }
            for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
                int z = baseZ + localZ;
                for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                    int x = baseX + localX;
                    short blockId = source.blockIdAt(x, y, z);
                    BlockCollisionShape shape = shapeFor(set, blockId);
                    if (!shape.empty()) {
                        entries.add(new Entry(x, y, z, blockId, shape));
                    }
                }
            }
        }
        return new SectionShapes(List.copyOf(entries));
    }

    private BlockCollisionShape shapeFor(ShapeSet set, short blockId) {
        return switch (set) {
            case MOVEMENT -> {
                BlockType block = source.blockType(blockId);
                yield block.collidable() ? BlockCollisionShapes.collisionShape(blockId) : BlockCollisionShape.NONE;
            }
            case PROJECTILE -> BlockCollisionShapes.projectileShape(blockId);
        };
    }

    private boolean outsideDimension(double minY, double maxY) {
        DimensionSettings dimension = source.dimension();
        return floor(minY) < dimension.minY() || floor(maxY) >= dimension.maxYExclusive();
    }

    private boolean sectionOverlapsDimension(int sectionY) {
        int minY = sectionY * ChunkSection.SIZE;
        int maxY = minY + ChunkSection.SIZE - 1;
        DimensionSettings dimension = source.dimension();
        return maxY >= dimension.minY() && minY < dimension.maxYExclusive();
    }

    private int clampY(int y) {
        DimensionSettings dimension = source.dimension();
        return Math.max(dimension.minY(), Math.min(dimension.maxYExclusive() - 1, y));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public enum ShapeSet {
        MOVEMENT,
        PROJECTILE
    }

    @FunctionalInterface
    private interface EntryPredicate {
        boolean matches(Entry entry);
    }

    public interface Source {
        DimensionSettings dimension();

        boolean ensureChunkAvailable(ChunkPos pos);

        short blockIdAt(int x, int y, int z);

        BlockType blockType(short blockId);
    }

    private record SectionKey(ChunkPos pos, int sectionY) {
    }

    private record SectionShapes(List<Entry> entries) {
    }

    private record Entry(int x, int y, int z, short blockId, BlockCollisionShape shape) {
    }

    private static final class ProjectileImpactSearch {
        private PartialShapeImpactResolver.ImpactResult best;

        private void accept(PartialShapeImpactResolver.ImpactResult hit) {
            if (best == null || hit.fraction() < best.fraction()) {
                best = hit;
            }
        }

        private Optional<PartialShapeImpactResolver.ImpactResult> best() {
            return Optional.ofNullable(best);
        }
    }

    public record CollisionCheck(boolean collides, boolean blockedByMissingChunk) {
        public static CollisionCheck colliding() {
            return new CollisionCheck(true, false);
        }

        public static CollisionCheck missingChunk() {
            return new CollisionCheck(true, true);
        }

        public static CollisionCheck clear() {
            return new CollisionCheck(false, false);
        }
    }

    public record ShapeBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }

    public record CacheStats(int sections, int shapes) {
        public int estimatedBytes() {
            return Math.max(0, sections) * 96 + Math.max(0, shapes) * 48;
        }
    }
}
