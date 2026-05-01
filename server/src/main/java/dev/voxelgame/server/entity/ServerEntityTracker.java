package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.DamageResult;
import dev.voxelgame.common.entity.DamageSource;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.physics.ProjectilePhysics;
import dev.voxelgame.common.physics.ProjectilePhysicsConfig;
import dev.voxelgame.common.physics.ProjectileState;
import dev.voxelgame.common.physics.PhysicsTickets;
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
    private static final double LOCAL_AVOIDANCE_PADDING = 0.08;
    private static final double AMBIENT_DAMAGE_INVULNERABILITY_SECONDS = 0.28;

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
        Objects.requireNonNull(blockCollision, "blockCollision");
        Objects.requireNonNull(waterQuery, "waterQuery");
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
            ProjectileHit hit = ProjectilePhysics.step(current, deltaSeconds, config, blockCollision, waterQuery, targets);
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
        if (damageAmount <= 0) {
            return DamageResult.rejected(entityId, DamageResult.RejectionReason.INVALID_AMOUNT);
        }
        EntitySnapshot current = ambientEntities.get(entityId);
        if (current == null) {
            return DamageResult.rejected(entityId, DamageResult.RejectionReason.UNKNOWN_TARGET);
        }
        boolean enforceCooldown = Double.isFinite(nowSeconds);
        if (enforceCooldown && nowSeconds < nextAmbientDamageAllowedAt.getOrDefault(entityId, 0.0)) {
            return DamageResult.rejected(entityId, DamageResult.RejectionReason.INVULNERABLE);
        }
        int newHealth = Math.max(0, current.health() - damageAmount);
        int appliedDamage = current.health() - newHealth;
        EntitySnapshot attacker = source.attackerPlayerId() == null ? null : players.get(source.attackerPlayerId());

        double knockbackX = 0.0;
        double knockbackZ = 0.0;
        if (attacker != null) {
            double dx = current.x() - attacker.x();
            double dz = current.z() - attacker.z();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.0001) {
                knockbackX = (dx / distance) * knockbackStrength;
                knockbackZ = (dz / distance) * knockbackStrength;
            }
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
                newHealth,
                newHealth <= 0 ? EntitySnapshot.STATE_IDLE : EntitySnapshot.STATE_FLEE,
                knockbackX,
                0.1,
                knockbackZ
        );

        if (newHealth <= 0) {
            ambientEntities.remove(entityId);
            ambientAnchors.remove(entityId);
            followTargets.remove(entityId);
            nextAmbientDamageAllowedAt.remove(entityId);
        } else {
            ambientEntities.put(entityId, updated);
            if (enforceCooldown) {
                nextAmbientDamageAllowedAt.put(entityId, nowSeconds + AMBIENT_DAMAGE_INVULNERABILITY_SECONDS);
            }
        }

        return DamageResult.accepted(entityId, appliedDamage, newHealth <= 0, updated, knockbackX, 0.1, knockbackZ);
    }

    public List<EntitySnapshot> tickAmbient(long tick) {
        return tickAmbient(tick, (current, candidate) -> true);
    }

    public List<EntitySnapshot> tickAmbient(long tick, MovementValidator movementValidator) {
        Objects.requireNonNull(movementValidator, "movementValidator");
        long startNanos = System.nanoTime();
        if (ambientEntities.isEmpty()) {
            List<EntitySnapshot> itemDropUpdates = tickItemDrops(tick);
            lastAmbientTickStats = new AmbientTickStats(
                    0,
                    0,
                    0,
                    0,
                    itemDropUpdates.size(),
                    itemDropUpdates.size(),
                    System.nanoTime() - startNanos
            );
            return itemDropUpdates;
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

            // Apply knockback velocity
            if (current.velocityX() != 0.0 || current.velocityY() != 0.0 || current.velocityZ() != 0.0) {
                moved = new EntitySnapshot(
                        moved.entityId(),
                        moved.typeKey(),
                        moved.ownerPlayerId(),
                        moved.x() + current.velocityX(),
                        moved.y() + current.velocityY(),
                        moved.z() + current.velocityZ(),
                        moved.yaw(),
                        moved.pitch(),
                        moved.health(),
                        moved.stateKey(),
                        current.velocityX() * 0.9,
                        Math.max(current.velocityY() - 0.05, -0.3),
                        current.velocityZ() * 0.9
                );
            } else {
                moved = moved.withVelocity(0.0, 0.0, 0.0);
            }

            boolean blocked = !movementValidator.canMove(current, moved) || locallyBlocked(current, moved);
            if (blocked) {
                blockedMoves++;
                moved = blockedAmbientMove(current, moved);
            }
            ambientEntities.put(entry.getKey(), moved);
            updated.add(moved);
        }
        List<EntitySnapshot> itemDropUpdates = tickItemDrops(tick);
        updated.addAll(itemDropUpdates);
        lastAmbientTickStats = new AmbientTickStats(
                ambientEntities.size(),
                activeAmbient,
                parkedAmbient,
                blockedMoves,
                itemDropUpdates.size(),
                updated.size(),
                System.nanoTime() - startNanos
        );
        return updated;
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
        List<EntitySnapshot> targets = new ArrayList<>(players.size() + ambientEntities.size());
        targets.addAll(players.values());
        targets.addAll(ambientEntities.values());
        return targets;
    }

    private static ProjectilePhysicsConfig projectileConfig(String typeKey) {
        if (ProjectilePhysicsConfig.arrow().typeKey().equals(typeKey)) {
            return ProjectilePhysicsConfig.arrow();
        }
        return ProjectilePhysicsConfig.arrow();
    }

    private boolean locallyBlocked(EntitySnapshot current, EntitySnapshot candidate) {
        for (EntitySnapshot player : players.values()) {
            if (overlaps(candidate, player, LOCAL_AVOIDANCE_PADDING)) {
                return true;
            }
        }
        for (EntitySnapshot other : ambientEntities.values()) {
            if (other.entityId() != current.entityId() && overlaps(candidate, other, LOCAL_AVOIDANCE_PADDING)) {
                return true;
            }
        }
        return false;
    }

    private static EntitySnapshot blockedAmbientMove(EntitySnapshot current, EntitySnapshot candidate) {
        return new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                current.x(),
                current.y(),
                current.z(),
                candidate.yaw(),
                candidate.pitch(),
                candidate.health(),
                candidate.stateKey(),
                0.0,
                0.0,
                0.0
        );
    }

    private static boolean overlaps(EntitySnapshot first, EntitySnapshot second, double padding) {
        EntityBounds firstBounds = EntityBounds.forType(first.typeKey());
        EntityBounds secondBounds = EntityBounds.forType(second.typeKey());
        double firstBaseY = EntityBounds.baseY(first);
        double secondBaseY = EntityBounds.baseY(second);
        return firstBounds.minX(first.x()) < secondBounds.maxX(second.x()) + padding
                && firstBounds.maxX(first.x()) > secondBounds.minX(second.x()) - padding
                && firstBounds.minY(firstBaseY) < secondBounds.maxY(secondBaseY) + padding
                && firstBounds.maxY(firstBaseY) > secondBounds.minY(secondBaseY) - padding
                && firstBounds.minZ(first.z()) < secondBounds.maxZ(second.z()) + padding
                && firstBounds.maxZ(first.z()) > secondBounds.minZ(second.z()) - padding;
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

    private List<EntitySnapshot> tickItemDrops(long tick) {
        if (itemDrops.isEmpty()) {
            return List.of();
        }
        List<EntitySnapshot> updated = new ArrayList<>(itemDrops.size());
        for (Map.Entry<Long, DroppedItemEntity> entry : itemDrops.entrySet()) {
            DroppedItemEntity moved = entry.getValue().tick(tick);
            itemDrops.put(entry.getKey(), moved);
            updated.add(moved.snapshot());
        }
        return updated;
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
            int emittedSnapshots,
            long durationNanos
    ) {
        public static final AmbientTickStats EMPTY = new AmbientTickStats(0, 0, 0, 0, 0, 0, 0L);

        public AmbientTickStats {
            ambientTotal = Math.max(0, ambientTotal);
            activeAmbient = Math.max(0, activeAmbient);
            parkedAmbient = Math.max(0, parkedAmbient);
            blockedAmbientMoves = Math.max(0, blockedAmbientMoves);
            itemDropUpdates = Math.max(0, itemDropUpdates);
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
