package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.gameplay.GameplayEventBatch;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.world.ChunkPos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public sealed interface GamePacket permits
        GamePacket.Handshake,
        GamePacket.LoginRequest,
        GamePacket.LoginAccepted,
        GamePacket.LoginRejected,
        GamePacket.ChunkData,
        GamePacket.BlockUpdate,
        GamePacket.BlockAction,
        GamePacket.BlockInteract,
        GamePacket.PlayerMove,
        GamePacket.PlayerPositionSnapshot,
        GamePacket.EntitySnapshots,
        GamePacket.EntityInteract,
        GamePacket.ProjectileShoot,
        GamePacket.ProjectileImpact,
        GamePacket.GameplayEvents,
        GamePacket.InventorySnapshot,
        GamePacket.PlayerStatsSnapshot,
        GamePacket.ServerStatsSnapshot,
        GamePacket.StorageOpenRequest,
        GamePacket.SleepRequest,
        GamePacket.CookRequest,
        GamePacket.CampfireStatus,
        GamePacket.StorageOpen,
        GamePacket.StorageClose,
        GamePacket.StorageTransfer,
        GamePacket.CraftRequest,
        GamePacket.Chat {

    int PROTOCOL_VERSION = 25;
    int MAX_CLIENT_NAME_LENGTH = 64;
    int MAX_USERNAME_LENGTH = 32;
    int MAX_AUTH_TOKEN_LENGTH = 128;
    int MAX_LOGIN_REJECTED_REASON_LENGTH = 160;
    int MAX_RECIPE_KEY_LENGTH = 96;
    int MAX_CHAT_MESSAGE_LENGTH = 192;
    int MAX_PROJECTILE_TYPE_KEY_LENGTH = 96;

    PacketType type();

    record Handshake(int protocolVersion, String clientName) implements GamePacket {
        public Handshake {
            clientName = requireText(clientName, "clientName", MAX_CLIENT_NAME_LENGTH);
        }

        @Override
        public PacketType type() {
            return PacketType.HANDSHAKE;
        }
    }

    record LoginRequest(String username, String authToken) implements GamePacket {
        public LoginRequest {
            username = requireText(username, "username", MAX_USERNAME_LENGTH);
            authToken = requireText(authToken, "authToken", MAX_AUTH_TOKEN_LENGTH);
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_REQUEST;
        }
    }

    record LoginAccepted(UUID playerId, long worldSeed, int minY, int maxYExclusive) implements GamePacket {
        public LoginAccepted {
            Objects.requireNonNull(playerId, "playerId");
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_ACCEPTED;
        }
    }

    record LoginRejected(String reason) implements GamePacket {
        public LoginRejected {
            reason = requireText(reason, "reason", MAX_LOGIN_REJECTED_REASON_LENGTH);
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_REJECTED;
        }
    }

    record ChunkData(ChunkPos pos, int minY, short[] blockIds, byte[] skyLight, byte[] blockLight) implements GamePacket {
        public ChunkData {
            Objects.requireNonNull(pos, "pos");
            blockIds = blockIds.clone();
            skyLight = skyLight.clone();
            blockLight = blockLight.clone();
        }

        public short[] blockIds() {
            return blockIds.clone();
        }

        public byte[] skyLight() {
            return skyLight.clone();
        }

        public byte[] blockLight() {
            return blockLight.clone();
        }

        @Override
        public PacketType type() {
            return PacketType.CHUNK_DATA;
        }
    }

    record BlockUpdate(int x, int y, int z, short blockId) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.BLOCK_UPDATE;
        }
    }

    record BlockAction(
            Action action,
            int selectedSlot,
            int targetX,
            int targetY,
            int targetZ,
            int placeX,
            int placeY,
            int placeZ,
            short blockId
    ) implements GamePacket {
        public BlockAction {
            Objects.requireNonNull(action, "action");
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.BLOCK_ACTION;
        }

        public enum Action {
            BREAK,
            PLACE
        }
    }

    record PlayerMove(
            long sequence,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean onGround,
            boolean feetInWater,
            boolean bodyInWater,
            boolean headUnderwater
    ) implements GamePacket {
        public PlayerMove {
            requireSequence(sequence);
        }

        public PlayerMove(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this(0L, x, y, z, yaw, pitch, onGround, false, false, false);
        }

        public PlayerMove(long sequence, double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this(sequence, x, y, z, yaw, pitch, onGround, false, false, false);
        }

        public PlayerMove(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean feetInWater, boolean bodyInWater, boolean headUnderwater) {
            this(0L, x, y, z, yaw, pitch, onGround, feetInWater, bodyInWater, headUnderwater);
        }

        public PlayerMove(double x, double y, double z, float yaw, float pitch, boolean onGround, PlayerWaterState waterState) {
            this(
                    0L,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    onGround,
                    Objects.requireNonNull(waterState, "waterState").feetInWater(),
                    waterState.bodyInWater(),
                    waterState.headUnderwater()
            );
        }

        public PlayerMove(long sequence, double x, double y, double z, float yaw, float pitch, boolean onGround, PlayerWaterState waterState) {
            this(
                    sequence,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    onGround,
                    Objects.requireNonNull(waterState, "waterState").feetInWater(),
                    waterState.bodyInWater(),
                    waterState.headUnderwater()
            );
        }

        public PlayerWaterState waterState() {
            return new PlayerWaterState(feetInWater, bodyInWater, headUnderwater);
        }

        @Override
        public PacketType type() {
            return PacketType.PLAYER_MOVE;
        }
    }

    record PlayerPositionSnapshot(
            long sequence,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean onGround,
            boolean feetInWater,
            boolean bodyInWater,
            boolean headUnderwater,
            MovementCorrection correction
    ) implements GamePacket {
        public PlayerPositionSnapshot {
            requireSequence(sequence);
            correction = Objects.requireNonNull(correction, "correction");
        }

        public PlayerPositionSnapshot(
                long sequence,
                double x,
                double y,
                double z,
                float yaw,
                float pitch,
                boolean onGround,
                boolean feetInWater,
                boolean bodyInWater,
                boolean headUnderwater
        ) {
            this(
                    sequence,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    onGround,
                    feetInWater,
                    bodyInWater,
                    headUnderwater,
                    MovementCorrection.SOFT
            );
        }

        public PlayerPositionSnapshot(long sequence, double x, double y, double z, float yaw, float pitch, boolean onGround, PlayerWaterState waterState) {
            this(sequence, x, y, z, yaw, pitch, onGround, waterState, MovementCorrection.SOFT);
        }

        public PlayerPositionSnapshot(
                long sequence,
                double x,
                double y,
                double z,
                float yaw,
                float pitch,
                boolean onGround,
                PlayerWaterState waterState,
                MovementCorrection correction
        ) {
            this(
                    sequence,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    onGround,
                    Objects.requireNonNull(waterState, "waterState").feetInWater(),
                    waterState.bodyInWater(),
                    waterState.headUnderwater(),
                    correction
            );
        }

        public PlayerWaterState waterState() {
            return new PlayerWaterState(feetInWater, bodyInWater, headUnderwater);
        }

        @Override
        public PacketType type() {
            return PacketType.PLAYER_POSITION_SNAPSHOT;
        }
    }

    enum MovementCorrection {
        SOFT,
        HARD,
        RESPAWN_TELEPORT
    }

    record BlockInteract(int selectedSlot, int targetX, int targetY, int targetZ) implements GamePacket {
        public BlockInteract {
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.BLOCK_INTERACT;
        }
    }

    record EntitySnapshots(List<EntitySnapshot> snapshots) implements GamePacket {
        public EntitySnapshots {
            snapshots = List.copyOf(snapshots);
        }

        @Override
        public PacketType type() {
            return PacketType.ENTITY_SNAPSHOT;
        }
    }

    record EntityInteract(long entityId, int selectedSlot, Action action) implements GamePacket {
        public EntityInteract {
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
            Objects.requireNonNull(action, "action");
        }

        @Override
        public PacketType type() {
            return PacketType.ENTITY_INTERACT;
        }

        public enum Action {
            OBSERVE,
            FEED,
            ATTACK
        }
    }

    record ProjectileShoot(int selectedSlot, long sequence) implements GamePacket {
        public ProjectileShoot {
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
            requireSequence(sequence);
        }

        @Override
        public PacketType type() {
            return PacketType.PROJECTILE_SHOOT;
        }
    }

    record ProjectileImpact(
            long projectileId,
            String projectileTypeKey,
            ProjectileHit.Type hitType,
            double x,
            double y,
            double z,
            int blockX,
            int blockY,
            int blockZ,
            ProjectileHit.BlockFace blockFace,
            long entityId,
            boolean stuck,
            long serverTick
    ) implements GamePacket {
        public ProjectileImpact {
            projectileTypeKey = requireText(projectileTypeKey, "projectileTypeKey", MAX_PROJECTILE_TYPE_KEY_LENGTH);
            Objects.requireNonNull(hitType, "hitType");
            Objects.requireNonNull(blockFace, "blockFace");
            if (hitType == ProjectileHit.Type.MISS) {
                throw new IllegalArgumentException("ProjectileImpact cannot carry MISS hits");
            }
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Projectile impact coordinates must be finite");
            }
            if (serverTick < 0L) {
                throw new IllegalArgumentException("serverTick must be >= 0");
            }
        }

        public static ProjectileImpact fromHit(ProjectileHit hit, long serverTick) {
            Objects.requireNonNull(hit, "hit");
            return new ProjectileImpact(
                    hit.state().projectileId(),
                    hit.state().typeKey(),
                    hit.type(),
                    hit.impactX(),
                    hit.impactY(),
                    hit.impactZ(),
                    hit.blockX(),
                    hit.blockY(),
                    hit.blockZ(),
                    hit.blockFace(),
                    hit.entityId(),
                    hit.type() == ProjectileHit.Type.BLOCK,
                    serverTick
            );
        }

        @Override
        public PacketType type() {
            return PacketType.PROJECTILE_IMPACT;
        }
    }

    record GameplayEvents(GameplayEventBatch batch) implements GamePacket {
        public GameplayEvents(List<GameplayEvent> events) {
            this(GameplayEventBatch.of(events));
        }

        public GameplayEvents {
            Objects.requireNonNull(batch, "batch");
        }

        public int schemaVersion() {
            return batch.schemaVersion();
        }

        public List<GameplayEvent> events() {
            return batch.events();
        }

        @Override
        public PacketType type() {
            return PacketType.GAMEPLAY_EVENTS;
        }
    }

    record InventorySnapshot(List<ItemStack> slots) implements GamePacket {
        public InventorySnapshot {
            slots = List.copyOf(slots);
        }

        @Override
        public PacketType type() {
            return PacketType.INVENTORY_SNAPSHOT;
        }
    }

    record PlayerStatsSnapshot(int health, int hunger, int stamina, int breath, int armor, int comfort) implements GamePacket {
        public PlayerStatsSnapshot {
            if (health < 0 || hunger < 0 || stamina < 0 || breath < 0 || armor < 0 || comfort < 0) {
                throw new IllegalArgumentException("Player stats cannot be negative");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.PLAYER_STATS_SNAPSHOT;
        }
    }

    record ServerStatsSnapshot(
            int chunkSubscriptions,
            long sentEntitySnapshotPackets,
            long sentEntitySnapshots,
            long sentChunkPackets,
            long sentBlockUpdates,
            long discardedUpdatesOutsideInterest,
            long rejectedChunkRequests,
            long failedChunkRequests,
            long sentPackets,
            long estimatedPacketBytes,
            long averagePacketBytes,
            double packetRatePerSecond,
            int savePendingWrites,
            int saveRunningWrites,
            long saveQueuedWrites,
            long saveCompletedWrites,
            long saveFailedWrites,
            long saveRejectedWrites,
            long saveWrittenBytes,
            long saveWriteMilliseconds,
            double saveAverageWriteMilliseconds,
            double saveQueuedWritesPerSecond,
            double saveWrittenBytesPerSecond,
            double saveWriteMillisecondsPerSecond,
            double saveFailedWritesPerSecond
    ) implements GamePacket {
        public ServerStatsSnapshot(
                int chunkSubscriptions,
                long sentEntitySnapshotPackets,
                long sentEntitySnapshots,
                long sentChunkPackets,
                long sentBlockUpdates,
                long discardedUpdatesOutsideInterest,
                long rejectedChunkRequests,
                long failedChunkRequests,
                long sentPackets,
                long estimatedPacketBytes,
                long averagePacketBytes,
                double packetRatePerSecond
        ) {
            this(
                    chunkSubscriptions,
                    sentEntitySnapshotPackets,
                    sentEntitySnapshots,
                    sentChunkPackets,
                    sentBlockUpdates,
                    discardedUpdatesOutsideInterest,
                    rejectedChunkRequests,
                    failedChunkRequests,
                    sentPackets,
                    estimatedPacketBytes,
                    averagePacketBytes,
                    packetRatePerSecond,
                    0,
                    0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        public ServerStatsSnapshot {
            if (chunkSubscriptions < 0
                    || sentEntitySnapshotPackets < 0L
                    || sentEntitySnapshots < 0L
                    || sentChunkPackets < 0L
                    || sentBlockUpdates < 0L
                    || discardedUpdatesOutsideInterest < 0L
                    || rejectedChunkRequests < 0L
                    || failedChunkRequests < 0L
                    || sentPackets < 0L
                    || estimatedPacketBytes < 0L
                    || averagePacketBytes < 0L
                    || !Double.isFinite(packetRatePerSecond)
                    || packetRatePerSecond < 0.0
                    || savePendingWrites < 0
                    || saveRunningWrites < 0
                    || saveQueuedWrites < 0L
                    || saveCompletedWrites < 0L
                    || saveFailedWrites < 0L
                    || saveRejectedWrites < 0L
                    || saveWrittenBytes < 0L
                    || saveWriteMilliseconds < 0L
                    || !Double.isFinite(saveAverageWriteMilliseconds)
                    || saveAverageWriteMilliseconds < 0.0
                    || !Double.isFinite(saveQueuedWritesPerSecond)
                    || saveQueuedWritesPerSecond < 0.0
                    || !Double.isFinite(saveWrittenBytesPerSecond)
                    || saveWrittenBytesPerSecond < 0.0
                    || !Double.isFinite(saveWriteMillisecondsPerSecond)
                    || saveWriteMillisecondsPerSecond < 0.0
                    || !Double.isFinite(saveFailedWritesPerSecond)
                    || saveFailedWritesPerSecond < 0.0) {
                throw new IllegalArgumentException("Server stats cannot contain negative or non-finite values");
            }
        }

        public static ServerStatsSnapshot empty() {
            return new ServerStatsSnapshot(0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0);
        }

        @Override
        public PacketType type() {
            return PacketType.SERVER_STATS_SNAPSHOT;
        }
    }

    record StorageOpenRequest(int x, int y, int z, int transactionId) implements GamePacket {
        public StorageOpenRequest(int x, int y, int z) {
            this(x, y, z, 1);
        }

        public StorageOpenRequest {
            requireTransactionId(transactionId);
        }

        @Override
        public PacketType type() {
            return PacketType.STORAGE_OPEN_REQUEST;
        }
    }

    record SleepRequest(int x, int y, int z) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.SLEEP_REQUEST;
        }
    }

    record CookRequest(int stationX, int stationY, int stationZ, String recipeKey, List<Integer> inputSlots, int transactionId) implements GamePacket {
        public CookRequest(int stationX, int stationY, int stationZ, String recipeKey, List<Integer> inputSlots) {
            this(stationX, stationY, stationZ, recipeKey, inputSlots, 1);
        }

        public CookRequest {
            recipeKey = requireText(recipeKey, "recipeKey", MAX_RECIPE_KEY_LENGTH);
            Objects.requireNonNull(inputSlots, "inputSlots");
            requireTransactionId(transactionId);
            inputSlots = List.copyOf(inputSlots);
            if (inputSlots.isEmpty() || inputSlots.size() > 9) {
                throw new IllegalArgumentException("inputSlots must contain 1..9 slots");
            }
            for (int slot : inputSlots) {
                if (slot < 0) {
                    throw new IllegalArgumentException("inputSlots cannot contain negative slots");
                }
            }
        }

        @Override
        public PacketType type() {
            return PacketType.COOK_REQUEST;
        }
    }

    record CampfireStatus(
            int x,
            int y,
            int z,
            boolean active,
            double fuelSecondsRemaining,
            String cookingRecipeKey,
            double cookTotalSeconds,
            double cookSecondsRemaining
    ) implements GamePacket {
        public CampfireStatus {
            cookingRecipeKey = optionalText(cookingRecipeKey, "cookingRecipeKey", MAX_RECIPE_KEY_LENGTH);
            if (!Double.isFinite(fuelSecondsRemaining) || fuelSecondsRemaining < 0.0) {
                throw new IllegalArgumentException("fuelSecondsRemaining must be finite and >= 0");
            }
            if (!Double.isFinite(cookTotalSeconds) || cookTotalSeconds < 0.0
                    || !Double.isFinite(cookSecondsRemaining) || cookSecondsRemaining < 0.0) {
                throw new IllegalArgumentException("cook timing must be finite and >= 0");
            }
            if (cookingRecipeKey.isBlank() && (cookTotalSeconds > 0.0 || cookSecondsRemaining > 0.0)) {
                throw new IllegalArgumentException("cook timing needs a cooking recipe key");
            }
            if (!cookingRecipeKey.isBlank() && cookTotalSeconds <= 0.0) {
                throw new IllegalArgumentException("cooking recipe needs a positive cook total");
            }
            if (cookSecondsRemaining > cookTotalSeconds) {
                throw new IllegalArgumentException("cookSecondsRemaining cannot exceed cookTotalSeconds");
            }
        }

        public boolean cooking() {
            return !cookingRecipeKey.isBlank();
        }

        @Override
        public PacketType type() {
            return PacketType.CAMPFIRE_STATUS;
        }
    }

    record StorageOpen(int x, int y, int z, List<ItemStack> slots) implements GamePacket {
        public StorageOpen {
            slots = List.copyOf(slots);
        }

        @Override
        public PacketType type() {
            return PacketType.STORAGE_OPEN;
        }
    }

    record StorageClose(int x, int y, int z) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.STORAGE_CLOSE;
        }
    }

    record StorageTransfer(int x, int y, int z, boolean fromStorage, int sourceSlot, int targetSlot, int count, int transactionId) implements GamePacket {
        public static final int AUTO_TARGET_SLOT = -1;

        public StorageTransfer(int x, int y, int z, boolean fromStorage, int slot) {
            this(x, y, z, fromStorage, slot, AUTO_TARGET_SLOT, Integer.MAX_VALUE, 1);
        }

        public StorageTransfer {
            if (sourceSlot < 0) {
                throw new IllegalArgumentException("sourceSlot must be >= 0");
            }
            if (targetSlot < AUTO_TARGET_SLOT) {
                throw new IllegalArgumentException("targetSlot must be >= -1");
            }
            if (count < 1) {
                throw new IllegalArgumentException("count must be >= 1");
            }
            requireTransactionId(transactionId);
        }

        public int slot() {
            return sourceSlot;
        }

        @Override
        public PacketType type() {
            return PacketType.STORAGE_TRANSFER;
        }
    }

    record CraftRequest(String recipeKey, int count, boolean hasStation, int stationX, int stationY, int stationZ, int transactionId) implements GamePacket {
        public CraftRequest {
            recipeKey = requireText(recipeKey, "recipeKey", MAX_RECIPE_KEY_LENGTH);
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException("count must be in 1..64");
            }
            requireTransactionId(transactionId);
        }

        public CraftRequest(String recipeKey) {
            this(recipeKey, 1, false, 0, 0, 0, 1);
        }

        public CraftRequest(String recipeKey, int count, boolean hasStation, int stationX, int stationY, int stationZ) {
            this(recipeKey, count, hasStation, stationX, stationY, stationZ, 1);
        }

        public static CraftRequest atStation(String recipeKey, int count, int stationX, int stationY, int stationZ) {
            return atStation(recipeKey, count, stationX, stationY, stationZ, 1);
        }

        public static CraftRequest atStation(String recipeKey, int count, int stationX, int stationY, int stationZ, int transactionId) {
            return new CraftRequest(recipeKey, count, true, stationX, stationY, stationZ, transactionId);
        }

        @Override
        public PacketType type() {
            return PacketType.CRAFT_REQUEST;
        }
    }

    record Chat(String message) implements GamePacket {
        public Chat {
            message = requireText(message, "message", MAX_CHAT_MESSAGE_LENGTH);
        }

        @Override
        public PacketType type() {
            return PacketType.CHAT;
        }
    }

    private static String requireText(String value, String label, int maxLength) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " length must be <= " + maxLength);
        }
        return value;
    }

    private static String optionalText(String value, String label, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " length must be <= " + maxLength);
        }
        return value;
    }

    private static void requireTransactionId(int transactionId) {
        if (transactionId < 1) {
            throw new IllegalArgumentException("transactionId must be >= 1");
        }
    }

    private static void requireSequence(long sequence) {
        if (sequence < 0L) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }
    }
}
