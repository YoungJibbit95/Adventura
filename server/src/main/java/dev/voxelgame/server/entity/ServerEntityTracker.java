package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.DamageResult;
import dev.voxelgame.common.entity.DamageSource;
import dev.voxelgame.common.entity.EntityDamageRules;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.physics.EntityPhysics;
import dev.voxelgame.common.physics.EntityPhysicsProfile;
import dev.voxelgame.common.physics.FluidPhysics;
import dev.voxelgame.common.physics.PartialShapeImpactResolver;
import dev.voxelgame.common.physics.ProjectileBounds;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.physics.ProjectilePhysics;
import dev.voxelgame.common.physics.ProjectilePhysicsConfig;
import dev.voxelgame.common.physics.ProjectileState;
import dev.voxelgame.common.physics.PhysicsTickets;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ServerEntityTracker {
    public static final double SLEEP_DANGER_RADIUS = 8.0;
    private static final double AMBIENT_DAMAGE_INVULNERABILITY_SECONDS = 0.28;
    private static final double AMBIENT_TICK_SECONDS = 0.05;
    private static final double ITEM_MERGE_RADIUS_SQUARED = 0.42 * 0.42;
    private static final Registry<ItemType> ITEMS = Items.createDefaultRegistry();

    private final Map<UUID, EntitySnapshot> players = new ConcurrentHashMap<>();
    private final Map<Long, EntitySnapshot> ambientEntities = new ConcurrentHashMap<>();
    private final Map<Long, EntitySnapshot> ambientAnchors = new ConcurrentHashMap<>();
    private final Map<Long, FollowTarget> followTargets = new ConcurrentHashMap<>();
    private final Map<Long, DroppedItemEntity> itemDrops = new ConcurrentHashMap<>();
    private final Map<Long, ProjectileState> projectiles = new ConcurrentHashMap<>();
    private final Map<Long, Double> nextAmbientDamageAllowedAt = new ConcurrentHashMap<>();
    private volatile AmbientTickStats lastAmbientTickStats = AmbientTickStats.EMPTY;
    private volatile ProjectileTickStats lastProjectileTickStats = ProjectileTickStats.EMPTY;
    private long nextItemDropEntityId = -1L;
    private long nextProjectileEntityId = -1_000_000L;
    private long nextDebugEntityId = -2_000_000L;

    public ServerEntityTracker() {
    }

    public ServerEntityTracker(long seed) {
        this(seed, snapshot -> true);
    }

    public ServerEntityTracker(long seed, Predicate<EntitySnapshot> spawnValidator) {
        Objects.requireNonNull(spawnValidator, "spawnValidator");
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnAroundSpawn(seed, 5)) {
            if (spawnValidator.test(snapshot)) {
                addAmbient(snapshot);
            }
        }
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canMove(EntitySnapshot current, EntitySnapshot candidate);
    }

    public EntitySnapshot registerPlayer(UUID playerId) {
        EntitySnapshot snapshot = new EntitySnapshot(
                entityId(playerId),
                "voxel:player",
                playerId,
                8.0,
                118.0,
                8.0,
                0.0f,
                0.0f,
                20
        );
        players.put(playerId, snapshot);
        return snapshot;
    }

    public EntitySnapshot updatePlayer(UUID playerId, double x, double y, double z, float yaw, float pitch) {
        EntitySnapshot snapshot = new EntitySnapshot(entityId(playerId), "voxel:player", playerId, x, y, z, yaw, pitch, 20);
        players.put(playerId, snapshot);
        followTargets.replaceAll((entityId, target) -> target.playerId().equals(playerId) ? new FollowTarget(playerId, x, y, z) : target);
        return snapshot;
    }

    public void removePlayer(UUID playerId) {
        if (playerId != null) {
            players.remove(playerId);
            followTargets.entrySet().removeIf(entry -> entry.getValue().playerId().equals(playerId));
        }
    }

    public List<EntitySnapshot> snapshots() {
        List<EntitySnapshot> snapshots = new ArrayList<>(players.values());
        snapshots.addAll(ambientEntities.values());
        itemDrops.values().stream()
                .map(DroppedItemEntity::snapshot)
                .forEach(snapshots::add);
        projectiles.values().stream()
                .map(ProjectileState::snapshot)
                .forEach(snapshots::add);
        return snapshots;
    }

    public Optional<EntitySnapshot> snapshot(long entityId) {
        EntitySnapshot player = players.values().stream()
                .filter(snapshot -> snapshot.entityId() == entityId)
                .findFirst()
                .orElse(null);
        if (player != null) {
            return Optional.of(player);
        }
        EntitySnapshot ambient = ambientEntities.get(entityId);
        if (ambient != null) {
            return Optional.of(ambient);
        }
        EntitySnapshot itemDrop = Optional.ofNullable(itemDrops.get(entityId)).map(DroppedItemEntity::snapshot).orElse(null);
        if (itemDrop != null) {
            return Optional.of(itemDrop);
        }
        return Optional.ofNullable(projectiles.get(entityId)).map(ProjectileState::snapshot);
    }

    public DroppedItemEntity spawnItemDrop(String itemKey, ItemStack stack, double x, double y, double z, long tick) {
        if (itemKey == null || itemKey.isBlank()) {
            throw new IllegalArgumentException("Dropped item key is required");
        }
        DroppedItemEntity drop = new DroppedItemEntity(
                nextItemDropEntityId--,
                itemKey,
                stack,
                x,
                y + 0.18,
                z,
                y + 0.05,
                launchVelocity(itemKey, 0),
                0.26,
                launchVelocity(itemKey, 1),
                tick
        );
        itemDrops.put(drop.entityId(), drop);
        return drop;
    }

    public List<DroppedItemEntity> itemDropsNear(double x, double y, double z, double radius, long tick) {
        double maxDistanceSquared = Math.max(0.0, radius) * Math.max(0.0, radius);
        return itemDrops.values().stream()
                .filter(drop -> drop.canPickup(tick))
                .filter(drop -> drop.distanceSquared(x, y, z) <= maxDistanceSquared)
                .sorted(Comparator.comparingDouble(drop -> drop.distanceSquared(x, y, z)))
                .toList();
    }

    public Optional<DroppedItemEntity> claimItemDrop(long entityId) {
        return Optional.ofNullable(itemDrops.remove(entityId));
    }

    public int itemDropCount() {
        return itemDrops.size();
    }

    public ProjectileState spawnArrowProjectile(UUID ownerPlayerId, double x, double y, double z, double directionX, double directionY, double directionZ) {
        ProjectilePhysicsConfig config = ProjectilePhysicsConfig.arrow();
        double length = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        if (!Double.isFinite(length) || length <= 0.0001) {
            throw new IllegalArgumentException("Projectile direction must be finite and non-zero");
        }
        ProjectileState projectile = new ProjectileState(
                nextProjectileEntityId--,
                ownerPlayerId,
                config.typeKey(),
                x,
                y,
                z,
                directionX / length * config.initialSpeed(),
                directionY / length * config.initialSpeed(),
                directionZ / length * config.initialSpeed(),
                0
        );
        projectiles.put(projectile.projectileId(), projectile);
        return projectile;
    }

    public EntitySnapshot spawnDebugAmbient(String typeKey, double x, double y, double z) {
        if (typeKey == null || typeKey.isBlank() || ItemDropType.isTypeKey(typeKey)) {
            throw new IllegalArgumentException("Debug ambient type key is required");
        }
        EntitySnapshot snapshot = new EntitySnapshot(nextDebugEntityId--, typeKey, null, x, y, z, 0.0f, 0.0f, 10);
        addAmbient(snapshot);
        return snapshot;
    }

    public int projectileCount() {
        return projectiles.size();
    }

    public List<ProjectileState> projectileStates() {
        return projectiles.values().stream()
                .sorted(Comparator.comparingLong(ProjectileState::projectileId))
                .toList();
    }

    public List<ProjectileHit> tickProjectiles(
            double deltaSeconds,
            ProjectilePhysics.BlockCollisionQuery blockCollision,
            ProjectilePhysics.WaterQuery waterQuery
    ) {
        return tickProjectiles(deltaSeconds, blockCollision, waterQuery, Double.NaN);
    }

    public List<ProjectileHit> tickProjectiles(
            double deltaSeconds,
            ProjectilePhysics.BlockCollisionQuery blockCollision,
            ProjectilePhysics.WaterQuery waterQuery,
            double nowSeconds
    ) {
        Objects.requireNonNull(waterQuery, "waterQuery");
        FluidPhysics.FluidQuery fluidQuery = (x, y, z) -> waterQuery.inWater(x, y, z) ? FluidPhysics.stillWater() : FluidPhysics.air();
        return tickProjectiles(deltaSeconds, blockCollision, fluidQuery, nowSeconds);
    }

    public List<ProjectileHit> tickProjectiles(
            double deltaSeconds,
            ProjectilePhysics.BlockImpactQuery blockImpact,
            FluidPhysics.FluidQuery fluidQuery,
            double nowSeconds
    ) {
        Objects.requireNonNull(blockImpact, "blockImpact");
        return tickProjectilesInternal(deltaSeconds, blockImpact, fluidQuery, nowSeconds);
    }

    public List<ProjectileHit> tickProjectiles(
            double deltaSeconds,
            ProjectilePhysics.BlockCollisionQuery blockCollision,
            FluidPhysics.FluidQuery fluidQuery,
            double nowSeconds
    ) {
        Objects.requireNonNull(blockCollision, "blockCollision");
        return tickProjectilesInternal(deltaSeconds, blockCollision, fluidQuery, nowSeconds);
    }

    private List<ProjectileHit> tickProjectilesInternal(
            double deltaSeconds,
            ProjectilePhysics.BlockCollisionQuery blockCollision,
            FluidPhysics.FluidQuery fluidQuery,
            double nowSeconds
    ) {
        return tickProjectilesInternal(
                deltaSeconds,
                (fromX, fromY, fromZ, toX, toY, toZ, bounds) -> blockingImpactFromCollision(blockCollision, fromX, fromY, fromZ, toX, toY, toZ, bounds),
                fluidQuery,
                nowSeconds
        );
    }

    private List<ProjectileHit> tickProjectilesInternal(
            double deltaSeconds,
            ProjectilePhysics.BlockImpactQuery blockImpact,
            FluidPhysics.FluidQuery fluidQuery,
            double nowSeconds
    ) {
        Objects.requireNonNull(fluidQuery, "fluidQuery");
        long startNanos = System.nanoTime();
        if (projectiles.isEmpty()) {
            lastProjectileTickStats = new ProjectileTickStats(0, 0, 0, 0, 0, 0, System.nanoTime() - startNanos);
            return List.of();
        }
        List<ProjectileHit> hits = new ArrayList<>(projectiles.size());
        List<EntitySnapshot> targets = projectileTargets();
        int blockHits = 0;
        int entityHits = 0;
        int expired = 0;
        for (Map.Entry<Long, ProjectileState> entry : projectiles.entrySet()) {
            ProjectileState current = entry.getValue();
            ProjectilePhysicsConfig config = projectileConfig(current.typeKey());
            ProjectileHit hit = ProjectilePhysics.step(current, deltaSeconds, config, blockImpact, fluidQuery, targets);
            hits.add(hit);
            if (hit.type() == ProjectileHit.Type.MISS) {
                projectiles.put(entry.getKey(), hit.state());
                continue;
            }
            projectiles.remove(entry.getKey());
            if (hit.type() == ProjectileHit.Type.ENTITY) {
                entityHits++;
                damageAmbient(
                        hit.entityId(),
                        config.damage(),
                        DamageSource.projectile(current.ownerPlayerId(), current.projectileId(), current.typeKey()),
                        nowSeconds,
                        0.12
                );
            } else if (hit.type() == ProjectileHit.Type.BLOCK) {
                blockHits++;
            } else if (hit.type() == ProjectileHit.Type.EXPIRED) {
                expired++;
            }
        }
        lastProjectileTickStats = new ProjectileTickStats(
                hits.size(),
                projectiles.size(),
                hits.size(),
                blockHits,
                entityHits,
                expired,
                System.nanoTime() - startNanos
        );
        return hits;
    }

    private static Optional<PartialShapeImpactResolver.ImpactResult> blockingImpactFromCollision(
            ProjectilePhysics.BlockCollisionQuery blockCollision,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            ProjectileBounds bounds
    ) {
        if (!blockCollision.collides(toX, toY, toZ, bounds)) {
            return Optional.empty();
        }
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double radius = bounds.radius();
        ProjectileHit.BlockFace face = blockFaceForDelta(dx, dy, dz);
        return Optional.of(PartialShapeImpactResolver.blocking(
                floor(toX + travelOffset(dx, radius)),
                floor(toY + travelOffset(dy, radius)),
                floor(toZ + travelOffset(dz, radius)),
                toX,
                toY,
                toZ,
                face,
                1.0
        ));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double travelOffset(double delta, double radius) {
        if (Math.abs(delta) < 0.0000001) {
            return 0.0;
        }
        return Math.copySign(radius, delta);
    }

    private static ProjectileHit.BlockFace blockFaceForDelta(double dx, double dy, double dz) {
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absX >= absY && absX >= absZ && absX > 0.0000001) {
            return dx > 0.0 ? ProjectileHit.BlockFace.WEST : ProjectileHit.BlockFace.EAST;
        }
        if (absY >= absZ && absY > 0.0000001) {
            return dy > 0.0 ? ProjectileHit.BlockFace.DOWN : ProjectileHit.BlockFace.UP;
        }
        if (absZ > 0.0000001) {
            return dz > 0.0 ? ProjectileHit.BlockFace.NORTH : ProjectileHit.BlockFace.SOUTH;
        }
        return ProjectileHit.BlockFace.NONE;
    }

    public Optional<EntitySnapshot> feedAmbient(long entityId, int healAmount) {
        return feedAmbient(entityId, healAmount, null);
    }

    public Optional<EntitySnapshot> feedAmbient(long entityId, int healAmount, UUID targetPlayerId) {
        if (healAmount <= 0) {
            return Optional.empty();
        }
        EntitySnapshot current = ambientEntities.get(entityId);
        if (current == null) {
            return Optional.empty();
        }
        EntitySnapshot updated = new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                current.x(),
                current.y(),
                current.z(),
                current.yaw(),
                current.pitch(),
                Math.min(20, current.health() + healAmount),
                targetPlayerId == null ? current.stateKey() : EntitySnapshot.STATE_FOLLOW,
                0.0,
                0.0,
                0.0
        );
        ambientEntities.put(entityId, updated);
        if (targetPlayerId != null) {
            EntitySnapshot player = players.get(targetPlayerId);
            if (player != null) {
                followTargets.put(entityId, new FollowTarget(targetPlayerId, player.x(), player.y(), player.z()));
            }
        }
        return Optional.of(updated);
    }

    public Optional<EntitySnapshot> damageAmbient(long entityId, int damageAmount, UUID attackerPlayerId) {
        return damageAmbient(entityId, damageAmount, attackerPlayerId, 0.3);
    }

    public Optional<EntitySnapshot> damageAmbient(long entityId, int damageAmount, UUID attackerPlayerId, double knockbackStrength) {
        return damageAmbient(entityId, damageAmount, DamageSource.playerMelee(attackerPlayerId), Double.NaN, knockbackStrength).snapshot();
    }

    public DamageResult damageAmbient(long entityId, int damageAmount, DamageSource source, double nowSeconds) {
        return damageAmbient(entityId, damageAmount, source, nowSeconds, 0.3);
    }

    public DamageResult damageAmbient(long entityId, int damageAmount, DamageSource source, double nowSeconds, double knockbackStrength) {
        Objects.requireNonNull(source, "source");
        EntitySnapshot current = ambientEntities.get(entityId);
        boolean enforceCooldown = Double.isFinite(nowSeconds);
        EntitySnapshot attacker = source.attackerPlayerId() == null ? null : players.get(source.attackerPlayerId());
        EntityDamageRules.DamageResolution resolution = EntityDamageRules.resolveAmbientDamage(
                entityId,
                current,
                damageAmount,
                source,
                nowSeconds,
                nextAmbientDamageAllowedAt.getOrDefault(entityId, 0.0),
                AMBIENT_DAMAGE_INVULNERABILITY_SECONDS,
                knockbackStrength,
                attacker == null ? null : EntityDamageRules.originFrom(attacker)
        );
        DamageResult result = resolution.result();
        if (!result.accepted()) {
            return result;
        }

        EntitySnapshot updated = result.updatedSnapshot();
        if (result.killed()) {
            ambientEntities.remove(entityId);
            ambientAnchors.remove(entityId);
            followTargets.remove(entityId);
            nextAmbientDamageAllowedAt.remove(entityId);
        } else {
            ambientEntities.put(entityId, updated);
            if (enforceCooldown) {
                nextAmbientDamageAllowedAt.put(entityId, resolution.nextDamageAllowedAt());
            }
        }

        return result;
    }

    public List<EntitySnapshot> tickAmbient(long tick) {
        return tickAmbient(tick, (current, candidate) -> true);
    }

    public List<EntitySnapshot> tickAmbient(long tick, MovementValidator movementValidator) {
        Objects.requireNonNull(movementValidator, "movementValidator");
        return tickAmbient(tick, movementValidator, (x, y, z) -> FluidPhysics.air());
    }

    public List<EntitySnapshot> tickAmbient(long tick, MovementValidator movementValidator, FluidPhysics.FluidQuery fluidQuery) {
        Objects.requireNonNull(movementValidator, "movementValidator");
        Objects.requireNonNull(fluidQuery, "fluidQuery");
        long startNanos = System.nanoTime();
        if (ambientEntities.isEmpty()) {
            ItemDropTickResult itemDropTick = tickItemDrops(tick, fluidQuery);
            lastAmbientTickStats = new AmbientTickStats(
                    0,
                    0,
                    0,
                    0,
                    itemDropTick.updated().size(),
                    itemDropTick.expired(),
                    itemDropTick.updated().size(),
                    System.nanoTime() - startNanos
            );
            return itemDropTick.updated();
        }
        List<EntitySnapshot> updated = new ArrayList<>(ambientEntities.size() + itemDrops.size());
        int activeAmbient = 0;
        int parkedAmbient = 0;
        int blockedMoves = 0;
        for (Map.Entry<Long, EntitySnapshot> entry : ambientEntities.entrySet()) {
            EntitySnapshot current = entry.getValue();
            EntitySnapshot anchor = ambientAnchors.getOrDefault(entry.getKey(), current);
            FollowTarget followTarget = followTargets.get(entry.getKey());
            if (followTarget == null && parkedOutsideActiveChunks(current)) {
                parkedAmbient++;
                continue;
            }
            activeAmbient++;
            FleeThreat fleeThreat = followTarget == null ? fleeThreatFor(current).orElse(null) : null;
            EntitySnapshot moved = moveAmbient(anchor, current, followTarget, fleeThreat, tick);
            EntityPhysicsProfile profile = EntityPhysicsProfile.forType(current.typeKey());
            EntitySnapshot fluidCurrent = EntityPhysics.applyFluidForces(
                    current,
                    profile,
                    fluidSampleFor(current, fluidQuery),
                    AMBIENT_TICK_SECONDS
            );
            List<EntitySnapshot> neighbors = separationNeighbors(current.entityId());
            moved = EntityPhysics.applyImpulseMotion(fluidCurrent, moved, profile);
            moved = EntityPhysics.applySeparation(current, moved, neighbors);

            EntityPhysics.MoveResult moveResult = EntityPhysics.sweepWithSlide(
                    current,
                    moved,
                    (from, candidate) -> movementValidator.canMove(from, candidate)
                            && !EntityPhysics.overlapsAny(candidate, neighbors, profile.separationPadding())
            );
            if (moveResult.blocked()) {
                blockedMoves++;
            }
            moved = moveResult.snapshot();
            ambientEntities.put(entry.getKey(), moved);
            updated.add(moved);
        }
        ItemDropTickResult itemDropTick = tickItemDrops(tick, fluidQuery);
        updated.addAll(itemDropTick.updated());
        lastAmbientTickStats = new AmbientTickStats(
                ambientEntities.size(),
                activeAmbient,
                parkedAmbient,
                blockedMoves,
                itemDropTick.updated().size(),
                itemDropTick.expired(),
                updated.size(),
                System.nanoTime() - startNanos
        );
        return updated;
    }

    private static FluidPhysics.FluidSample fluidSampleFor(EntitySnapshot snapshot, FluidPhysics.FluidQuery fluidQuery) {
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        double sampleY = EntityBounds.baseY(snapshot) + bounds.height() * 0.5;
        FluidPhysics.FluidSample sample = fluidQuery.sample(snapshot.x(), sampleY, snapshot.z());
        return sample == null ? FluidPhysics.air() : sample;
    }

    private boolean parkedOutsideActiveChunks(EntitySnapshot current) {
        if (players.isEmpty()) {
            return false;
        }
        ChunkPos entityChunk = chunkFor(current);
        for (EntitySnapshot player : players.values()) {
            ChunkPos playerChunk = chunkFor(player);
            if (PhysicsTickets.insideTicket(entityChunk, playerChunk, PhysicsTickets.PLAYER_SIMULATION_RADIUS_CHUNKS)) {
                return false;
            }
        }
        return true;
    }

    private static ChunkPos chunkFor(EntitySnapshot snapshot) {
        return ChunkPos.fromBlock((int) Math.floor(snapshot.x()), (int) Math.floor(snapshot.z()));
    }

    private List<EntitySnapshot> projectileTargets() {
        List<EntitySnapshot> targets = new ArrayList<>(players.size() + ambientEntities.size() + projectiles.size());
        targets.addAll(players.values());
        targets.addAll(ambientEntities.values());
        projectiles.values().stream()
                .map(ProjectileState::snapshot)
                .forEach(targets::add);
        return targets;
    }

    private List<EntitySnapshot> separationNeighbors(long entityId) {
        List<EntitySnapshot> neighbors = new ArrayList<>(players.size() + ambientEntities.size() + itemDrops.size());
        neighbors.addAll(players.values());
        for (EntitySnapshot other : ambientEntities.values()) {
            if (other.entityId() != entityId) {
                neighbors.add(other);
            }
        }
        itemDrops.values().stream()
                .map(DroppedItemEntity::snapshot)
                .filter(snapshot -> snapshot.entityId() != entityId)
                .forEach(neighbors::add);
        return neighbors;
    }

    private static ProjectilePhysicsConfig projectileConfig(String typeKey) {
        if (ProjectilePhysicsConfig.arrow().typeKey().equals(typeKey)) {
            return ProjectilePhysicsConfig.arrow();
        }
        return ProjectilePhysicsConfig.arrow();
    }

    public int playerCount() {
        return players.size();
    }

    public AmbientTickStats lastAmbientTickStats() {
        return lastAmbientTickStats;
    }

    public ProjectileTickStats lastProjectileTickStats() {
        return lastProjectileTickStats;
    }

    public boolean hasDangerNear(double x, double y, double z, double radius) {
        double maxDistanceSquared = Math.max(0.0, radius) * Math.max(0.0, radius);
        for (EntitySnapshot snapshot : ambientEntities.values()) {
            if (!isDangerous(snapshot.typeKey())) {
                continue;
            }
            double dx = snapshot.x() - x;
            double dy = snapshot.y() - y;
            double dz = snapshot.z() - z;
            if (dx * dx + dy * dy + dz * dz <= maxDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    public void addAmbient(EntitySnapshot snapshot) {
        if (ItemDropType.isTypeKey(snapshot.typeKey())) {
            throw new IllegalArgumentException("Use spawnItemDrop for item drop entities");
        }
        ambientEntities.put(snapshot.entityId(), snapshot);
        ambientAnchors.put(snapshot.entityId(), snapshot);
    }

    private ItemDropTickResult tickItemDrops(long tick) {
        return tickItemDrops(tick, (x, y, z) -> FluidPhysics.air());
    }

    private ItemDropTickResult tickItemDrops(long tick, FluidPhysics.FluidQuery fluidQuery) {
        if (itemDrops.isEmpty()) {
            return ItemDropTickResult.EMPTY;
        }
        int expired = 0;
        for (Long entityId : itemDropIds()) {
            DroppedItemEntity drop = itemDrops.get(entityId);
            if (drop != null) {
                if (drop.expired(tick)) {
                    itemDrops.remove(entityId);
                    expired++;
                    continue;
                }
                itemDrops.put(entityId, drop.tick(tick, fluidQuery));
            }
        }
        separateItemDrops();
        mergeItemDrops(tick);

        List<EntitySnapshot> updated = new ArrayList<>(itemDrops.size());
        for (Long entityId : itemDropIds()) {
            DroppedItemEntity drop = itemDrops.get(entityId);
            if (drop != null) {
                updated.add(drop.snapshot());
            }
        }
        return new ItemDropTickResult(List.copyOf(updated), expired);
    }

    private void separateItemDrops() {
        for (Long entityId : itemDropIds()) {
            DroppedItemEntity drop = itemDrops.get(entityId);
            if (drop == null) {
                continue;
            }
            EntitySnapshot current = drop.snapshot();
            EntitySnapshot separated = EntityPhysics.applySeparation(current, current, separationNeighbors(entityId));
            if (Math.abs(separated.x() - current.x()) > 0.0001
                    || Math.abs(separated.y() - current.y()) > 0.0001
                    || Math.abs(separated.z() - current.z()) > 0.0001) {
                itemDrops.put(entityId, drop.withPosition(separated.x(), separated.y(), separated.z()));
            }
        }
    }

    private void mergeItemDrops(long tick) {
        for (Long targetId : itemDropIds()) {
            DroppedItemEntity target = itemDrops.get(targetId);
            if (target == null) {
                continue;
            }
            int maxStackSize = maxStackSize(target.itemKey());
            if (maxStackSize <= 1 || target.stack().count() >= maxStackSize) {
                continue;
            }
            for (Long sourceId : itemDropIds()) {
                if (targetId.equals(sourceId)) {
                    continue;
                }
                DroppedItemEntity source = itemDrops.get(sourceId);
                if (source == null || !canMerge(target, source, maxStackSize)) {
                    continue;
                }
                if (target.distanceSquared(source.x(), source.y(), source.z()) > ITEM_MERGE_RADIUS_SQUARED) {
                    continue;
                }
                int room = maxStackSize - target.stack().count();
                int moved = Math.min(room, source.stack().count());
                if (moved <= 0) {
                    continue;
                }
                target = target.withStack(
                        new ItemStack(target.stack().itemId(), target.stack().count() + moved, target.stack().damage()),
                        Math.max(target.createdTick(), Math.max(source.createdTick(), tick - 8L))
                );
                if (moved >= source.stack().count()) {
                    itemDrops.remove(sourceId);
                } else {
                    itemDrops.put(sourceId, source.withStack(
                            new ItemStack(source.stack().itemId(), source.stack().count() - moved, source.stack().damage()),
                            source.createdTick()
                    ));
                }
                itemDrops.put(targetId, target);
                if (target.stack().count() >= maxStackSize) {
                    break;
                }
            }
        }
    }

    private static boolean canMerge(DroppedItemEntity target, DroppedItemEntity source, int maxStackSize) {
        return target.itemKey().equals(source.itemKey())
                && target.stack().itemId() == source.stack().itemId()
                && target.stack().damage() == source.stack().damage()
                && target.stack().count() < maxStackSize
                && source.stack().count() > 0;
    }

    private static int maxStackSize(String itemKey) {
        return ITEMS.findByKey(itemKey)
                .map(ItemType::maxStackSize)
                .orElse(64);
    }

    private List<Long> itemDropIds() {
        return itemDrops.keySet().stream()
                .sorted()
                .toList();
    }

    private record ItemDropTickResult(List<EntitySnapshot> updated, int expired) {
        private static final ItemDropTickResult EMPTY = new ItemDropTickResult(List.of(), 0);
    }

    private static double launchVelocity(String itemKey, int axis) {
        long hash = itemKey.hashCode() * 31L + axis * 17L;
        double normalized = Math.floorMod(hash, 200L) / 100.0 - 1.0;
        return normalized * 0.38;
    }

    private static EntitySnapshot moveAmbient(EntitySnapshot anchor, EntitySnapshot current, long tick) {
        return moveAmbient(anchor, current, null, null, tick);
    }

    private static EntitySnapshot moveAmbient(EntitySnapshot anchor, EntitySnapshot current, FollowTarget followTarget, FleeThreat fleeThreat, long tick) {
        if (followTarget != null) {
            return followAmbient(current, followTarget);
        }
        if (fleeThreat != null) {
            return fleeAmbient(anchor, current, fleeThreat);
        }
        double radius = wanderRadius(anchor.typeKey());
        double phase = tick * phaseSpeed(anchor.typeKey()) + (anchor.entityId() & 0xFFFFL) * 0.013;
        double x = anchor.x() + Math.cos(phase) * radius;
        double z = anchor.z() + Math.sin(phase * 0.83) * radius;
        double y = anchor.y() + verticalOffset(anchor.typeKey(), phase);
        float yaw = (float) Math.toDegrees(Math.atan2(z - current.z(), x - current.x()));
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                x,
                y,
                z,
                yaw,
                current.pitch(),
                current.health(),
                stateFor(anchor.typeKey(), tick, anchor.entityId()),
                0.0,
                0.0,
                0.0
        );
    }

    private Optional<FleeThreat> fleeThreatFor(EntitySnapshot current) {
        double radius = fleeRadius(current.typeKey());
        if (radius <= 0.0 || players.isEmpty()) {
            return Optional.empty();
        }
        double maxDistanceSquared = radius * radius;
        FleeThreat nearest = null;
        for (EntitySnapshot player : players.values()) {
            double dx = current.x() - player.x();
            double dz = current.z() - player.z();
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared <= maxDistanceSquared
                    && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                nearest = new FleeThreat(player.x(), player.z(), distanceSquared);
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static EntitySnapshot followAmbient(EntitySnapshot current, FollowTarget target) {
        double dx = target.x() - current.x();
        double dz = target.z() - current.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double step = distance <= 1.6 ? 0.0 : Math.min(followSpeed(current.typeKey()), distance - 1.6);
        double x = distance <= 0.0001 ? current.x() : current.x() + dx / distance * step;
        double z = distance <= 0.0001 ? current.z() : current.z() + dz / distance * step;
        float yaw = distance <= 0.0001 ? current.yaw() : (float) Math.toDegrees(Math.atan2(dz, dx));
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                x,
                current.y(),
                z,
                yaw,
                current.pitch(),
                current.health(),
                EntitySnapshot.STATE_FOLLOW,
                0.0,
                0.0,
                0.0
        );
    }

    private static EntitySnapshot fleeAmbient(EntitySnapshot anchor, EntitySnapshot current, FleeThreat threat) {
        double dx = current.x() - threat.x();
        double dz = current.z() - threat.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double yawRadians = Math.toRadians(current.yaw());
        double awayX = distance <= 0.0001 ? Math.cos(yawRadians) : dx / distance;
        double awayZ = distance <= 0.0001 ? Math.sin(yawRadians) : dz / distance;
        double step = fleeSpeed(current.typeKey());
        double x = current.x() + awayX * step;
        double z = current.z() + awayZ * step;
        double maxAnchorDistance = fleeMaxDistance(current.typeKey());
        double ax = x - anchor.x();
        double az = z - anchor.z();
        double anchorDistance = Math.sqrt(ax * ax + az * az);
        if (anchorDistance > maxAnchorDistance && anchorDistance > 0.0001) {
            x = anchor.x() + ax / anchorDistance * maxAnchorDistance;
            z = anchor.z() + az / anchorDistance * maxAnchorDistance;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(awayZ, awayX));
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                x,
                current.y(),
                z,
                yaw,
                current.pitch(),
                current.health(),
                EntitySnapshot.STATE_FLEE,
                0.0,
                0.0,
                0.0
        );
    }

    private static double wanderRadius(String typeKey) {
        return switch (typeKey) {
            case "voxel:firefly_swarm" -> 1.6;
            case "voxel:forest_bunny" -> 1.25;
            case "voxel:little_boar" -> 1.1;
            case "voxel:moss_snail" -> 0.45;
            default -> 0.9;
        };
    }

    private static double phaseSpeed(String typeKey) {
        return switch (typeKey) {
            case "voxel:firefly_swarm" -> 0.055;
            case "voxel:forest_bunny" -> 0.034;
            case "voxel:little_boar" -> 0.021;
            case "voxel:moss_snail" -> 0.008;
            default -> 0.016;
        };
    }

    private static double fleeRadius(String typeKey) {
        return switch (typeKey) {
            case "voxel:forest_bunny" -> 4.0;
            case "voxel:cozy_sheep" -> 3.2;
            case "voxel:little_boar" -> 2.6;
            default -> 0.0;
        };
    }

    private static double fleeSpeed(String typeKey) {
        return switch (typeKey) {
            case "voxel:forest_bunny" -> 0.28;
            case "voxel:cozy_sheep" -> 0.18;
            case "voxel:little_boar" -> 0.14;
            default -> 0.1;
        };
    }

    private static double fleeMaxDistance(String typeKey) {
        return wanderRadius(typeKey) + fleeRadius(typeKey);
    }

    private static double followSpeed(String typeKey) {
        return switch (typeKey) {
            case "voxel:forest_bunny" -> 0.16;
            case "voxel:little_boar" -> 0.11;
            case "voxel:moss_snail" -> 0.03;
            default -> 0.08;
        };
    }

    private static double verticalOffset(String typeKey, double phase) {
        if ("voxel:firefly_swarm".equals(typeKey)) {
            return Math.sin(phase * 1.7) * 0.28;
        }
        if ("voxel:forest_bunny".equals(typeKey)) {
            return Math.max(0.0, Math.sin(phase * 2.0)) * 0.12;
        }
        return 0.0;
    }

    private static String stateFor(String typeKey, long tick, long entityId) {
        if ("voxel:cozy_sheep".equals(typeKey) && Math.floorMod(tick / 80 + entityId, 4L) == 0L) {
            return EntitySnapshot.STATE_GRAZE;
        }
        return EntitySnapshot.STATE_WANDER;
    }

    private static boolean isDangerous(String typeKey) {
        return switch (typeKey) {
            case "voxel:dune_crawler", "voxel:little_boar" -> true;
            default -> false;
        };
    }

    private static long entityId(UUID playerId) {
        return playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
    }

    private record FollowTarget(UUID playerId, double x, double y, double z) {
    }

    private record FleeThreat(double x, double z, double distanceSquared) {
    }

    public record AmbientTickStats(
            int ambientTotal,
            int activeAmbient,
            int parkedAmbient,
            int blockedAmbientMoves,
            int itemDropUpdates,
            int expiredItemDrops,
            int emittedSnapshots,
            long durationNanos
    ) {
        public static final AmbientTickStats EMPTY = new AmbientTickStats(0, 0, 0, 0, 0, 0, 0, 0L);

        public AmbientTickStats {
            ambientTotal = Math.max(0, ambientTotal);
            activeAmbient = Math.max(0, activeAmbient);
            parkedAmbient = Math.max(0, parkedAmbient);
            blockedAmbientMoves = Math.max(0, blockedAmbientMoves);
            itemDropUpdates = Math.max(0, itemDropUpdates);
            expiredItemDrops = Math.max(0, expiredItemDrops);
            emittedSnapshots = Math.max(0, emittedSnapshots);
            durationNanos = Math.max(0L, durationNanos);
        }
    }

    public record ProjectileTickStats(
            int projectilesBeforeTick,
            int projectilesAfterTick,
            int emittedHits,
            int blockHits,
            int entityHits,
            int expired,
            long durationNanos
    ) {
        public static final ProjectileTickStats EMPTY = new ProjectileTickStats(0, 0, 0, 0, 0, 0, 0L);

        public ProjectileTickStats {
            projectilesBeforeTick = Math.max(0, projectilesBeforeTick);
            projectilesAfterTick = Math.max(0, projectilesAfterTick);
            emittedHits = Math.max(0, emittedHits);
            blockHits = Math.max(0, blockHits);
            entityHits = Math.max(0, entityHits);
            expired = Math.max(0, expired);
            durationNanos = Math.max(0L, durationNanos);
        }
    }
}
