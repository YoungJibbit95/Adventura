package dev.voxelgame.server.net;

import dev.voxelgame.common.actions.ActionTarget;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.DamageResult;
import dev.voxelgame.common.entity.DamageSource;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.CozyLifeProgression;
import dev.voxelgame.common.gameplay.CreatureDesign;
import dev.voxelgame.common.gameplay.CreatureFriendshipRules;
import dev.voxelgame.common.gameplay.CraftingStationRules;
import dev.voxelgame.common.gameplay.EntityDrops;
import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.gameplay.status.StatusEffectEnvironmentRules;
import dev.voxelgame.common.gameplay.status.StatusEffectType;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.item.RecipeUnlock;
import dev.voxelgame.common.item.StarterInventory;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.EnvironmentHazardRules;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerMovementRules;
import dev.voxelgame.common.physics.PlayerPhysicsConfig;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkStreamingRings;
import dev.voxelgame.server.action.ServerProjectileShootAction;
import dev.voxelgame.server.auth.AuthProvider;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.DroppedItemEntity;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.player.ServerPlayerSurvivalState;
import dev.voxelgame.server.save.PlayerSave;
import dev.voxelgame.server.save.PlayerSaveStore;
import dev.voxelgame.server.save.SaveQueue;
import dev.voxelgame.server.save.SaveMetadata;
import dev.voxelgame.server.world.BlockEntityType;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private static final int STREAM_RADIUS_CHUNKS = 4;
    private static final double ENTITY_INTERACT_RANGE = 7.5;
    private static final double ENTITY_SNAPSHOT_RADIUS = 96.0;
    private static final double EVENT_INTEREST_RADIUS = ENTITY_SNAPSHOT_RADIUS;
    private static final double INITIAL_MOVE_SYNC_RADIUS = 128.0;
    private static final double MOVE_RATE_WINDOW_SECONDS = 1.0;
    private static final int MAX_MOVES_PER_RATE_WINDOW = 30;
    private static final double INTENT_RATE_WINDOW_SECONDS = 1.0;
    private static final int INTEREST_DEBUG_LOG_LIMIT = 32;
    private static final long FRIENDSHIP_FEED_COOLDOWN_TICKS = Math.max(
            1L,
            CreatureFriendshipRules.FEEDING_COOLDOWN_SECONDS * 20L
    );
    private static final PlayerPhysicsConfig PLAYER_PHYSICS = PlayerPhysicsConfig.defaults();
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Set<ServerConnectionHandler> ACTIVE_HANDLERS = ConcurrentHashMap.newKeySet();
    private static final Set<ServerWorld> DIRTY_ENTITY_SNAPSHOT_WORLDS = ConcurrentHashMap.newKeySet();

    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private final ServerProjectileShootAction projectileShootAction = new ServerProjectileShootAction();
    private final int streamRadiusChunks;
    private final Path playerSaveDirectory;
    private final ServerChunkStreamer chunkStreamer;
    private final SaveQueue saveQueue;
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final List<CraftingRecipe> recipes;
    private final Inventory inventory = new Inventory(36);
    private final ServerPlayerSurvivalState survivalState = new ServerPlayerSurvivalState();
    private final Set<ChunkPos> sentChunks = ConcurrentHashMap.newKeySet();
    private final AtomicLong sentEntitySnapshotPackets = new AtomicLong();
    private final AtomicLong sentEntitySnapshots = new AtomicLong();
    private final AtomicLong sentChunkPackets = new AtomicLong();
    private final AtomicLong sentBlockUpdates = new AtomicLong();
    private final AtomicLong discardedUpdatesOutsideInterest = new AtomicLong();
    private final AtomicLong rejectedChunkRequests = new AtomicLong();
    private final AtomicLong failedChunkRequests = new AtomicLong();
    private final AtomicLong sentPackets = new AtomicLong();
    private final AtomicLong estimatedPacketBytes = new AtomicLong();
    private final Deque<String> interestDebugLog = new ArrayDeque<>();
    private final long packetMetricsStartedAtNanos = System.nanoTime();
    private volatile ChannelHandlerContext context;
    private PendingCook pendingCook;
    private OpenStorage openStorage;
    private volatile boolean sleepReady;
    private volatile boolean loggedIn;
    private UUID playerId;
    private String playerName = "Player";
    private double playerX = 8.5;
    private double playerY = 120.0;
    private double playerZ = 8.5;
    private float playerYaw;
    private float playerPitch;
    private int hotbarSelection;
    private PlayerSave.SpawnPoint spawnPoint = PlayerSave.SpawnPoint.empty();
    private String gameMode = "survival";
    private List<String> discoveredRecipes = List.of();
    private List<String> discoveredBiomes = List.of();
    private List<String> journalEntries = List.of();
    private List<String> achievedMilestones = List.of();
    private List<String> completedGoals = List.of();
    private final Map<String, PlayerSave.CreatureFriendshipState> creatureFriendships = new ConcurrentHashMap<>();
    private String lastWorldKey = "overworld";
    private double nextBlockActionTime;
    private double nextEntityInteractTime;
    private double nextProjectileShootTime;
    private double nextEnvironmentDamageTime;
    private double nextStatsSyncTime;
    private double lastAcceptedMoveTime = Double.NaN;
    private boolean lastAcceptedGrounded;
    private double lastAcceptedMoveDeltaY;
    private double lastAcceptedMoveDeltaX;
    private double lastAcceptedMoveDeltaZ;
    private double lastAcceptedMoveDeltaSeconds;
    private boolean hasAcceptedMovementSample;
    private long lastAcceptedMoveSequence;
    private double moveRateWindowStart = Double.NaN;
    private int moveRateWindowCount;
    private final EnumMap<MovementRejectReason, Integer> movementRejectCounts = new EnumMap<>(MovementRejectReason.class);
    private final EnumMap<ClientIntent, IntentRateWindow> intentRateWindows = new EnumMap<>(ClientIntent.class);
    private final EnumMap<ClientIntent, Integer> intentRateRejectCounts = new EnumMap<>(ClientIntent.class);
    private int movementRejectStrikes;
    private MovementRejectReason lastMovementRejectReason = MovementRejectReason.NONE;
    private AuthoritativeCorrectionClass lastMovementCorrectionClass = AuthoritativeCorrectionClass.SOFT;
    private double fallStartY = Double.NaN;
    private boolean fallTouchedWater;
    private double lastSurvivalUpdateTime = Double.NaN;
    private long survivalTick;
    private long gameplayEventSequence;
    private int lastSyncedComfort = -1;
    private int lastClientTransactionId;

    public enum MovementRejectReason {
        NONE,
        NON_FINITE,
        RATE,
        VERTICAL_BOUNDS,
        COLLISION,
        GROUND,
        WATER,
        INITIAL_SYNC,
        SEQUENCE,
        PATH,
        UPWARD,
        SPEED,
        ACCELERATION
    }

    public enum AuthoritativeCorrectionClass {
        SOFT,
        HARD,
        RESPAWN_TELEPORT
    }

    private enum ClientIntent {
        BLOCK_ACTION,
        BLOCK_INTERACT,
        STORAGE_OPEN,
        STORAGE_TRANSFER,
        CRAFT,
        COOK,
        PROJECTILE_SHOOT,
        SLEEP,
        ENTITY_INTERACT,
        CHAT
    }

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, null, ServerChunkStreamer.direct());
    }

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker, Path playerSaveDirectory) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, playerSaveDirectory, ServerChunkStreamer.direct());
    }

    ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker, int streamRadiusChunks) {
        this(world, authProvider, entityTracker, streamRadiusChunks, null, ServerChunkStreamer.direct());
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            int streamRadiusChunks,
            Path playerSaveDirectory
    ) {
        this(world, authProvider, entityTracker, streamRadiusChunks, playerSaveDirectory, ServerChunkStreamer.direct());
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            Path playerSaveDirectory,
            ServerChunkStreamer chunkStreamer
    ) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, playerSaveDirectory, chunkStreamer);
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            int streamRadiusChunks,
            Path playerSaveDirectory,
            ServerChunkStreamer chunkStreamer
    ) {
        this(world, authProvider, entityTracker, streamRadiusChunks, playerSaveDirectory, chunkStreamer, null, null);
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            Path playerSaveDirectory,
            ServerChunkStreamer chunkStreamer,
            SaveQueue saveQueue
    ) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, playerSaveDirectory, chunkStreamer, null, saveQueue);
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            int streamRadiusChunks,
            Path playerSaveDirectory,
            ServerChunkStreamer chunkStreamer,
            List<CraftingRecipe> recipes
    ) {
        this(world, authProvider, entityTracker, streamRadiusChunks, playerSaveDirectory, chunkStreamer, recipes, null);
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            int streamRadiusChunks,
            Path playerSaveDirectory,
            ServerChunkStreamer chunkStreamer,
            List<CraftingRecipe> recipes,
            SaveQueue saveQueue
    ) {
        if (streamRadiusChunks < 0) {
            throw new IllegalArgumentException("streamRadiusChunks must be >= 0");
        }
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = entityTracker;
        this.streamRadiusChunks = streamRadiusChunks;
        this.playerSaveDirectory = playerSaveDirectory;
        this.chunkStreamer = chunkStreamer;
        this.saveQueue = saveQueue;
        this.recipes = recipes == null ? CraftingRecipes.createDefaultRecipes(items) : List.copyOf(recipes);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        context = ctx;
        ACTIVE_HANDLERS.add(this);
        CHANNELS.add(ctx.channel());
        ctx.writeAndFlush(new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-server"));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ACTIVE_HANDLERS.remove(this);
        context = null;
        CHANNELS.remove(ctx.channel());
        if (playerId != null) {
            savePlayerQuietly();
            entityTracker.removePlayer(playerId);
            markEntitySnapshotsDirty(world);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        context = ctx;
        switch (packet) {
            case GamePacket.Handshake handshake -> handleHandshake(ctx, handshake);
            case GamePacket.LoginRequest login -> handleLogin(ctx, login);
            case GamePacket.BlockAction action -> handleBlockAction(ctx, action);
            case GamePacket.BlockInteract interact -> handleBlockInteract(ctx, interact);
            case GamePacket.EntityInteract interact -> handleEntityInteract(ctx, interact);
            case GamePacket.StorageOpenRequest open -> handleStorageOpenRequest(ctx, open);
            case GamePacket.SleepRequest sleep -> handleSleepRequest(ctx, sleep);
            case GamePacket.CookRequest cook -> handleCookRequest(ctx, cook);
            case GamePacket.StorageTransfer transfer -> handleStorageTransfer(ctx, transfer);
            case GamePacket.CraftRequest craft -> handleCraftRequest(ctx, craft);
            case GamePacket.ProjectileShoot shoot -> handleProjectileShoot(ctx, shoot);
            case GamePacket.PlayerMove move -> {
                if (!loggedIn) {
                    ctx.close();
                } else {
                    handlePlayerMove(ctx, move);
                }
            }
            case GamePacket.Chat chat -> {
                if (loggedIn
                        && acceptIntentRate(ClientIntent.CHAT, System.nanoTime() / 1_000_000_000.0)
                        && !handlePhysicsDebugCommand(ctx, chat.message())) {
                    broadcastToLoggedIn(chat);
                }
            }
            default -> {
            }
        }
    }

    private void handleProjectileShoot(ChannelHandlerContext ctx, GamePacket.ProjectileShoot shoot) {
        projectileShootAction.execute(projectileShootContext(ctx), shoot, System.nanoTime() / 1_000_000_000.0);
    }

    private ServerProjectileShootAction.Context projectileShootContext(ChannelHandlerContext ctx) {
        return new ServerProjectileShootAction.Context() {
            @Override
            public UUID playerId() {
                return playerId;
            }

            @Override
            public boolean loggedIn() {
                return loggedIn;
            }

            @Override
            public void closeConnection() {
                ctx.close();
            }

            @Override
            public boolean acceptProjectileShootRate(double nowSeconds) {
                return acceptIntentRate(ClientIntent.PROJECTILE_SHOOT, nowSeconds);
            }

            @Override
            public int inventorySize() {
                return inventory.size();
            }

            @Override
            public void sendInventory() {
                ServerConnectionHandler.this.sendInventory(ctx);
            }

            @Override
            public void setHotbarSelection(int selectedSlot) {
                hotbarSelection = selectedSlot;
            }

            @Override
            public ItemStack inventorySlot(int slot) {
                return inventory.slot(slot);
            }

            @Override
            public Registry<ItemType> items() {
                return items;
            }

            @Override
            public ActionTarget projectileTarget() {
                double[] direction = lookDirection(playerYaw, playerPitch);
                return new ActionTarget.Direction(playerX, playerY - 0.18, playerZ, direction[0], direction[1], direction[2]);
            }

            @Override
            public double nextProjectileShootTime() {
                return nextProjectileShootTime;
            }

            @Override
            public void spawnProjectileFromCurrentLook() {
                double[] direction = lookDirection(playerYaw, playerPitch);
                entityTracker.spawnArrowProjectile(playerId, playerX, playerY - 0.18, playerZ, direction[0], direction[1], direction[2]);
            }

            @Override
            public void damageInventorySlot(int slot, int durabilityDamage) {
                inventory.damageSlot(slot, durabilityDamage, items);
            }

            @Override
            public void setNextProjectileShootTime(double nextProjectileShootTime) {
                ServerConnectionHandler.this.nextProjectileShootTime = nextProjectileShootTime;
            }

            @Override
            public void markEntitySnapshotsDirty() {
                ServerConnectionHandler.markEntitySnapshotsDirty(world);
            }

            @Override
            public void sendEntitySnapshots() {
                ServerConnectionHandler.this.sendEntitySnapshots(ctx);
            }
        };
    }

    private void handleHandshake(ChannelHandlerContext ctx, GamePacket.Handshake handshake) {
        if (handshake.protocolVersion() != GamePacket.PROTOCOL_VERSION) {
            String reason = "Protocol mismatch: server requires version "
                    + GamePacket.PROTOCOL_VERSION
                    + ", client sent "
                    + handshake.protocolVersion();
            ctx.writeAndFlush(new GamePacket.LoginRejected(reason)).addListener(future -> ctx.close());
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, GamePacket.LoginRequest login) {
        if (loggedIn) {
            ctx.writeAndFlush(new GamePacket.LoginRejected("Already logged in")).addListener(future -> ctx.close());
            return;
        }
        AuthResult result = authProvider.authenticate(login.username(), login.authToken());
        if (!result.accepted()) {
            ctx.writeAndFlush(new GamePacket.LoginRejected(result.message())).addListener(future -> ctx.close());
            return;
        }

        loggedIn = true;
        playerId = result.playerId();
        playerName = login.username();
        if (!loadPlayerSave()) {
            StarterInventory.apply(inventory, items);
        }
        entityTracker.registerPlayer(playerId);
        entityTracker.updatePlayer(playerId, playerX, playerY, playerZ, playerYaw, playerPitch);
        ctx.writeAndFlush(new GamePacket.LoginAccepted(playerId, world.seed(), world.dimension().minY(), world.dimension().maxYExclusive()));
        sendInventory(ctx);
        updateSurvival(System.nanoTime() / 1_000_000_000.0, false);
        sendPlayerStats(ctx, 0.0, true);
        sendAuthoritativePlayerPosition(ctx, 0L, AuthoritativeCorrectionClass.RESPAWN_TELEPORT);
        streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(playerX), (int) Math.floor(playerZ)));
        sendEntitySnapshots(ctx);
        markEntitySnapshotsDirty(world);
    }

    private void handleBlockAction(ChannelHandlerContext ctx, GamePacket.BlockAction action) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.BLOCK_ACTION, now)) {
            return;
        }
        if (!InteractionRules.isHotbarSlot(action.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = action.selectedSlot();
        if (now < nextBlockActionTime || !canReach(action)) {
            sendInventory(ctx);
            return;
        }
        switch (action.action()) {
            case BREAK -> handleBreakAction(ctx, action, now);
            case PLACE -> handlePlaceAction(ctx, action, now);
        }
    }

    private void handleBreakAction(ChannelHandlerContext ctx, GamePacket.BlockAction action, double now) {
        Optional<BlockType> targetBlock = world.blockAt(action.targetX(), action.targetY(), action.targetZ());
        if (targetBlock.isEmpty() || targetBlock.get().id() == Blocks.AIR || targetBlock.get().id() == Blocks.WATER) {
            sendInventory(ctx);
            return;
        }
        BlockType target = targetBlock.get();
        ItemStack selected = inventory.slot(action.selectedSlot());
        if (!InteractionRules.canHarvest(selected, items, target)) {
            sendInventory(ctx);
            return;
        }
        float multiplier = InteractionRules.breakMultiplier(selected, items, target);
        int dropCount = InteractionRules.dropCount(target, multiplier);
        Optional<String> dropKey = Optional.ofNullable(target.dropItemKey());
        Optional<Short> dropItemId = dropKey.flatMap(this::itemIdForKey);
        if (dropKey.isPresent() && dropItemId.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        if (dropItemId.isPresent() && !inventory.canAdd(dropItemId.get(), dropCount, items)) {
            sendInventory(ctx);
            return;
        }

        world.applyBlockAction(action).ifPresentOrElse(update -> {
            dropItemId.ifPresent(itemId -> inventory.add(itemId, dropCount, items));
            inventory.damageSlot(action.selectedSlot(), InteractionRules.toolDamage(selected, items, target), items);
            nextBlockActionTime = now + InteractionRules.breakDelaySeconds(target, multiplier);
            broadcastToLoggedInWorld(update);
            invalidateStorageUiForWorld(world, action.targetX(), action.targetY(), action.targetZ());
            sendInventory(ctx);
        }, () -> sendInventory(ctx));
    }

    private void handlePlaceAction(ChannelHandlerContext ctx, GamePacket.BlockAction action, double now) {
        ItemStack selected = inventory.slot(action.selectedSlot());
        if (selected.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        ItemType placeItem = items.requireById(selected.itemId());
        Optional<String> placedBlockKey = world.blockKey(action.blockId());
        if (placeItem.placesBlockKey() == null || placedBlockKey.isEmpty() || !placedBlockKey.get().equals(placeItem.placesBlockKey())) {
            sendInventory(ctx);
            return;
        }
        if (InteractionRules.placementIntersectsPlayer(playerX, playerY, playerZ, action.placeX(), action.placeY(), action.placeZ(), action.blockId())
                || placementIntersectsTrackedEntity(action.placeX(), action.placeY(), action.placeZ(), action.blockId())) {
            sendInventory(ctx);
            return;
        }

        world.applyBlockAction(action).ifPresentOrElse(update -> {
            inventory.removeFromSlot(action.selectedSlot(), 1);
            nextBlockActionTime = now + 0.12;
            broadcastToLoggedInWorld(update);
            sendInventory(ctx);
        }, () -> sendInventory(ctx));
    }

    private boolean canReach(GamePacket.BlockAction action) {
        if (!canInteractWithBlock(action.targetX(), action.targetY(), action.targetZ())) {
            return false;
        }
        return action.action() == GamePacket.BlockAction.Action.BREAK
                || InteractionRules.canReachBlock(playerX, playerY, playerZ, action.placeX(), action.placeY(), action.placeZ());
    }

    private boolean canInteractWithBlock(int x, int y, int z) {
        return InteractionRules.canReachBlock(playerX, playerY, playerZ, x, y, z)
                && world.hasBlockLineOfSight(playerX, playerY, playerZ, x, y, z);
    }

    private boolean placementIntersectsTrackedEntity(int blockX, int blockY, int blockZ, short blockId) {
        for (EntitySnapshot snapshot : entityTracker.snapshots()) {
            if (playerId != null && playerId.equals(snapshot.ownerPlayerId())) {
                continue;
            }
            if (InteractionRules.placementIntersectsEntity(snapshot, blockX, blockY, blockZ, blockId)) {
                return true;
            }
        }
        return false;
    }

    private void handleBlockInteract(ChannelHandlerContext ctx, GamePacket.BlockInteract interact) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.BLOCK_INTERACT, now)) {
            return;
        }
        if (!InteractionRules.isHotbarSlot(interact.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = interact.selectedSlot();
        if (now < nextBlockActionTime || !canInteractWithBlock(interact.targetX(), interact.targetY(), interact.targetZ())) {
            sendInventory(ctx);
            return;
        }
        Optional<BlockType> targetBlock = world.blockAt(interact.targetX(), interact.targetY(), interact.targetZ());
        if (targetBlock.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        if (targetBlock.get().id() == Blocks.STORAGE_CRATE) {
            openStorage(ctx, interact.targetX(), interact.targetY(), interact.targetZ(), now);
            return;
        }
        if (tryFuelCampfire(ctx, interact, targetBlock.get(), now)) {
            return;
        }
        Optional<InteractionRules.BlockInteraction> interaction = InteractionRules.blockInteraction(targetBlock.get());
        if (interaction.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        InteractionRules.BlockInteraction result = interaction.get();
        Optional<Short> itemId = itemIdForKey(result.itemKey());
        if (itemId.isEmpty() || !inventory.canAdd(itemId.get(), result.count(), items)) {
            sendInventory(ctx);
            return;
        }
        inventory.add(itemId.get(), result.count(), items);
        nextBlockActionTime = now + result.cooldownSeconds();
        sendInventory(ctx);
    }

    private void handleStorageOpenRequest(ChannelHandlerContext ctx, GamePacket.StorageOpenRequest open) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.STORAGE_OPEN, now)) {
            return;
        }
        if (!acceptClientTransaction(open.transactionId())) {
            sendInventory(ctx);
            return;
        }
        if (now < nextBlockActionTime || !canInteractWithBlock(open.x(), open.y(), open.z())) {
            recordInterestDebug("reject storage_open outside_ui_interest " + open.x() + " " + open.y() + " " + open.z());
            sendInventory(ctx);
            return;
        }
        openStorage(ctx, open.x(), open.y(), open.z(), now);
    }

    private void openStorage(ChannelHandlerContext ctx, int x, int y, int z, double now) {
        world.openStorageCrate(x, y, z).ifPresentOrElse(slots -> {
            openStorage = new OpenStorage(x, y, z);
            ctx.writeAndFlush(new GamePacket.StorageOpen(x, y, z, slots));
            recordSentPacket(24L + slots.size() * 8L);
            nextBlockActionTime = now + 0.12;
        }, () -> {
            recordInterestDebug("reject storage_open missing_block_entity " + x + " " + y + " " + z);
            sendInventory(ctx);
        });
    }

    private void handleSleepRequest(ChannelHandlerContext ctx, GamePacket.SleepRequest sleep) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.SLEEP, now)) {
            return;
        }
        if (now < nextBlockActionTime || !canInteractWithBlock(sleep.x(), sleep.y(), sleep.z())) {
            sendInventory(ctx);
            return;
        }
        if (entityTracker.hasDangerNear(
                sleep.x() + 0.5,
                sleep.y() + 0.5,
                sleep.z() + 0.5,
                ServerEntityTracker.SLEEP_DANGER_RADIUS
        )) {
            sendInventory(ctx);
            return;
        }
        if (!world.canSleepAt(sleep.x(), sleep.y(), sleep.z())) {
            sendInventory(ctx);
            return;
        }
        sleepReady = true;
        if (loggedInHandlerCountForWorld() > 1 && !allLoggedInHandlersForWorldSleepReady()) {
            nextBlockActionTime = now + 0.5;
            sendPlayerStats(ctx, now, true);
            return;
        }
        if (!world.trySleepAt(sleep.x(), sleep.y(), sleep.z())) {
            sleepReady = false;
            sendInventory(ctx);
            return;
        }
        wakeLoggedInHandlersForWorld(now);
    }

    private int loggedInHandlerCountForWorld() {
        int count = 0;
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world == world && handler.loggedIn && handler.context != null) {
                count++;
            }
        }
        return count;
    }

    private boolean allLoggedInHandlersForWorldSleepReady() {
        boolean sawPlayer = false;
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world != world || !handler.loggedIn || handler.context == null) {
                continue;
            }
            sawPlayer = true;
            if (!handler.sleepReady) {
                return false;
            }
        }
        return sawPlayer;
    }

    private void wakeLoggedInHandlersForWorld(double now) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world != world || !handler.loggedIn || handler.context == null) {
                continue;
            }
            handler.sleepReady = false;
            handler.nextBlockActionTime = now + 0.5;
            handler.updateSurvival(now, false);
            handler.emitStatusEffectChange(
                    handler.survivalState.applyStatusEffect(StatusEffectType.RESTED),
                    StatusEffectType.RESTED,
                    now,
                    true
            );
            handler.sendPlayerStats(handler.context, now, true);
        }
    }

    private void handleCookRequest(ChannelHandlerContext ctx, GamePacket.CookRequest cook) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.COOK, now)) {
            return;
        }
        if (!acceptClientTransaction(cook.transactionId())) {
            sendInventory(ctx);
            return;
        }
        if (pendingCook != null || now < nextBlockActionTime
                || !canInteractWithBlock(cook.stationX(), cook.stationY(), cook.stationZ())) {
            sendInventory(ctx);
            return;
        }
        Optional<CraftingRecipe> recipe = CraftingRecipes.findByKey(recipes, items, cook.recipeKey());
        if (recipe.isEmpty() || recipe.get().stationType() == CraftingStationType.INVENTORY) {
            sendInventory(ctx);
            return;
        }
        if (recipe.get().stationType() == CraftingStationType.CAMPFIRE) {
            world.tickCampfires(now).forEach(this::broadcastToLoggedInWorld);
        }
        Optional<BlockType> station = world.blockAt(cook.stationX(), cook.stationY(), cook.stationZ());
        if (station.isEmpty()
                || !CraftingStationRules.accepts(recipe.get().stationType(), station.get().id())
                || !stationBlockEntityMatches(recipe.get().stationType(), cook.stationX(), cook.stationY(), cook.stationZ())) {
            if (recipe.get().stationType() == CraftingStationType.CAMPFIRE) {
                sendCampfireStatus(ctx, cook.stationX(), cook.stationY(), cook.stationZ(), now);
            }
            sendInventory(ctx);
            return;
        }
        if (!recipeUnlocked(recipe.get(), Optional.of(recipe.get().stationType()))) {
            sendInventory(ctx);
            return;
        }
        List<Integer> inputSlots = distinctSlots(cook.inputSlots());
        if (!validInventorySlots(inputSlots)) {
            sendInventory(ctx);
            return;
        }
        Inventory simulated = inventory.copy();
        if (!consumeIngredientsFromSlots(simulated, recipe.get(), inputSlots) || simulated.addStack(recipe.get().result(), items) != 0) {
            sendInventory(ctx);
            return;
        }
        if (!consumeIngredientsFromSlots(inventory, recipe.get(), inputSlots)) {
            sendInventory(ctx);
            return;
        }
        double cookSeconds = Math.max(1, recipe.get().craftingTimeTicks()) / 20.0;
        pendingCook = new PendingCook(cook.stationX(), cook.stationY(), cook.stationZ(), recipe.get(), now + cookSeconds);
        nextBlockActionTime = now + 0.25;
        if (recipe.get().stationType() == CraftingStationType.CAMPFIRE) {
            sendCampfireStatus(ctx, cook.stationX(), cook.stationY(), cook.stationZ(), now);
        }
        sendInventory(ctx);
    }

    private void handleStorageTransfer(ChannelHandlerContext ctx, GamePacket.StorageTransfer transfer) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.STORAGE_TRANSFER, now)) {
            return;
        }
        if (!acceptClientTransaction(transfer.transactionId())) {
            sendInventory(ctx);
            return;
        }
        if (!canInteractWithBlock(transfer.x(), transfer.y(), transfer.z())) {
            recordInterestDebug("reject storage_transfer outside_ui_interest " + transfer.x() + " " + transfer.y() + " " + transfer.z());
            sendInventory(ctx);
            return;
        }
        world.transferStorageStack(
                transfer.x(),
                transfer.y(),
                transfer.z(),
                inventory,
                items,
                transfer.fromStorage(),
                transfer.sourceSlot(),
                transfer.targetSlot(),
                transfer.count()
        ).ifPresentOrElse(slots -> {
            openStorage = new OpenStorage(transfer.x(), transfer.y(), transfer.z());
            ctx.writeAndFlush(new GamePacket.StorageOpen(transfer.x(), transfer.y(), transfer.z(), slots));
            recordSentPacket(24L + slots.size() * 8L);
            sendInventory(ctx);
        }, () -> {
            recordInterestDebug("reject storage_transfer missing_block_entity " + transfer.x() + " " + transfer.y() + " " + transfer.z());
            closeOpenStorage(ctx, transfer.x(), transfer.y(), transfer.z());
            sendInventory(ctx);
        });
    }

    private boolean acceptClientTransaction(int transactionId) {
        if (!isNextClientTransaction(transactionId)) {
            return false;
        }
        lastClientTransactionId = transactionId;
        return true;
    }

    private boolean isNextClientTransaction(int transactionId) {
        if (transactionId < 1) {
            return false;
        }
        if (lastClientTransactionId == Integer.MAX_VALUE) {
            return transactionId == 1;
        }
        return transactionId == lastClientTransactionId + 1;
    }

    private boolean tryFuelCampfire(ChannelHandlerContext ctx, GamePacket.BlockInteract interact, BlockType targetBlock, double now) {
        if (!CampfireRules.isCampfire(targetBlock.id())) {
            return false;
        }
        ItemStack selected = inventory.slot(interact.selectedSlot());
        if (selected.isEmpty()) {
            sendCampfireStatus(ctx, interact.targetX(), interact.targetY(), interact.targetZ(), now);
            sendInventory(ctx);
            return true;
        }
        ItemType selectedItem = items.requireById(selected.itemId());
        OptionalDouble fuelSeconds = CampfireRules.fuelSeconds(selectedItem.key());
        if (fuelSeconds.isEmpty()) {
            sendCampfireStatus(ctx, interact.targetX(), interact.targetY(), interact.targetZ(), now);
            sendInventory(ctx);
            return true;
        }
        if (!inventory.removeFromSlot(interact.selectedSlot(), 1)) {
            sendInventory(ctx);
            return true;
        }
        world.fuelCampfire(interact.targetX(), interact.targetY(), interact.targetZ(), now, fuelSeconds.getAsDouble())
                .ifPresent(this::broadcastToLoggedInWorld);
        nextBlockActionTime = now + 0.25;
        sendCampfireStatus(ctx, interact.targetX(), interact.targetY(), interact.targetZ(), now);
        sendInventory(ctx);
        return true;
    }

    private void handleEntityInteract(ChannelHandlerContext ctx, GamePacket.EntityInteract interact) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.ENTITY_INTERACT, now)) {
            return;
        }
        if (!InteractionRules.isHotbarSlot(interact.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = interact.selectedSlot();
        Optional<EntitySnapshot> target = entityTracker.snapshot(interact.entityId());
        if (target.isEmpty() || now < nextEntityInteractTime || !canReachEntity(target.get())) {
            sendInventory(ctx);
            return;
        }
        switch (interact.action()) {
            case FEED -> {
                if (!tryFeedEntity(ctx, interact, target.get())) {
                    return;
                }
            }
            case ATTACK -> {
                if (!tryAttackEntity(ctx, interact, target.get(), now)) {
                    return;
                }
            }
            case OBSERVE -> {
            }
        }
        nextEntityInteractTime = now + 0.35;
        markEntitySnapshotsDirty(world);
        sendInventory(ctx);
    }

    private boolean tryFeedEntity(
            ChannelHandlerContext ctx,
            GamePacket.EntityInteract interact,
            EntitySnapshot target
    ) {
        ItemStack selected = inventory.slot(interact.selectedSlot());
        if (selected.isEmpty()) {
            sendInventory(ctx);
            return false;
        }
        ItemType selectedItem = items.requireById(selected.itemId());
        if (!selectedItem.isFood()) {
            sendInventory(ctx);
            return false;
        }
        Optional<CreatureDesign> creature = CozyLifeProgression.findCreature(target.typeKey());
        boolean favoriteFeed = creature
                .map(design -> CreatureFriendshipRules.canFeed(design, selectedItem.key()))
                .orElse(false);
        if (favoriteFeed && !canAcceptFavoriteFeed(creature.orElseThrow())) {
            sendInventory(ctx);
            return false;
        }
        Optional<EntitySnapshot> updated = entityTracker.feedAmbient(interact.entityId(), Math.max(1, selectedItem.foodValue() / 2), playerId);
        if (updated.isEmpty()) {
            sendInventory(ctx);
            return false;
        }
        inventory.removeFromSlot(interact.selectedSlot(), 1);
        if (favoriteFeed) {
            recordFavoriteFeed(creature.orElseThrow());
        }
        return true;
    }

    private boolean canAcceptFavoriteFeed(CreatureDesign design) {
        long currentDay = currentWorldDay();
        long currentTick = world.dayTimeTicks();
        PlayerSave.CreatureFriendshipState state = creatureFriendships.get(design.entityKey());
        if (state == null) {
            return true;
        }
        int acceptedFeedsToday = state.feedDay() == currentDay ? state.acceptedFeedsToday() : 0;
        if (!CreatureFriendshipRules.canAcceptFeedToday(acceptedFeedsToday)) {
            return false;
        }
        long lastFeedWorldTick = state.lastFeedWorldTick();
        return currentTick < lastFeedWorldTick
                || currentTick - lastFeedWorldTick >= FRIENDSHIP_FEED_COOLDOWN_TICKS;
    }

    private void recordFavoriteFeed(CreatureDesign design) {
        long currentDay = currentWorldDay();
        long currentTick = world.dayTimeTicks();
        creatureFriendships.compute(design.entityKey(), (entityKey, previous) -> {
            int previousTotal = previous == null ? 0 : previous.acceptedFeedsTotal();
            int previousToday = previous != null && previous.feedDay() == currentDay
                    ? previous.acceptedFeedsToday()
                    : 0;
            return new PlayerSave.CreatureFriendshipState(
                    entityKey,
                    previousTotal + 1,
                    previousToday + 1,
                    currentDay,
                    currentTick
            );
        });
    }

    private long currentWorldDay() {
        return world.dayTimeTicks() / ServerWorld.DAY_LENGTH_TICKS;
    }

    private boolean tryAttackEntity(ChannelHandlerContext ctx, GamePacket.EntityInteract interact, EntitySnapshot target, double now) {
        ItemStack selected = inventory.slot(interact.selectedSlot());
        int damage = entityAttackDamage(selected);
        double knockbackMultiplier = selected.isEmpty() ? 0.5 : (1.0 + (items.requireById(selected.itemId()).toolLevel() * 0.3));
        DamageResult result = entityTracker.damageAmbient(
                interact.entityId(),
                damage,
                DamageSource.playerMelee(playerId),
                now,
                knockbackMultiplier
        );
        if (!result.accepted()) {
            sendInventory(ctx);
            return false;
        }
        if (result.killed()) {
            spawnEntityDrops(target);
        }
        inventory.damageSlot(interact.selectedSlot(), 1, items);
        return true;
    }

    private void spawnEntityDrops(EntitySnapshot target) {
        for (EntityDrops.Drop drop : EntityDrops.dropsFor(target)) {
            itemIdForKey(drop.itemKey()).ifPresent(itemId -> entityTracker.spawnItemDrop(
                    drop.itemKey(),
                    new ItemStack(itemId, drop.count()),
                    target.x(),
                    target.y(),
                    target.z(),
                    survivalTick
            ));
        }
    }

    private int entityAttackDamage(ItemStack selected) {
        if (selected.isEmpty()) {
            return 1;
        }
        ItemType item = items.requireById(selected.itemId());
        return item.isTool() ? 2 + item.toolLevel() : 1;
    }

    private boolean canReachEntity(EntitySnapshot snapshot) {
        return InteractionRules.canReachEntity(playerX, playerY, playerZ, snapshot, ENTITY_INTERACT_RANGE);
    }

    private void handleCraftRequest(ChannelHandlerContext ctx, GamePacket.CraftRequest craft) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptIntentRate(ClientIntent.CRAFT, now)) {
            return;
        }
        if (!acceptClientTransaction(craft.transactionId())) {
            sendInventory(ctx);
            return;
        }
        Optional<CraftingRecipe> recipe = CraftingRecipes.findByKey(recipes, items, craft.recipeKey());
        if (recipe.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        if (recipe.get().stationType() != CraftingStationType.INVENTORY && recipe.get().craftingTimeTicks() > 0) {
            sendInventory(ctx);
            return;
        }
        Optional<CraftingStationType> station = craftingStationFor(craft, recipe.get());
        if (station.isPresent() && recipeUnlocked(recipe.get(), station)) {
            recipe.get().craft(inventory, items, station.get(), craft.count());
        }
        sendInventory(ctx);
    }

    private Optional<CraftingStationType> craftingStationFor(GamePacket.CraftRequest craft, CraftingRecipe recipe) {
        if (recipe.stationType() == CraftingStationType.INVENTORY) {
            return Optional.of(CraftingStationType.INVENTORY);
        }
        if (!craft.hasStation() || !canInteractWithBlock(craft.stationX(), craft.stationY(), craft.stationZ())) {
            return Optional.empty();
        }
        if (recipe.stationType() == CraftingStationType.CAMPFIRE) {
            double now = System.nanoTime() / 1_000_000_000.0;
            world.tickCampfires(now).forEach(this::broadcastToLoggedInWorld);
        }
        Optional<BlockType> station = world.blockAt(craft.stationX(), craft.stationY(), craft.stationZ());
        if (station.isPresent()
                && CraftingStationRules.accepts(recipe.stationType(), station.get().id())
                && stationBlockEntityMatches(recipe.stationType(), craft.stationX(), craft.stationY(), craft.stationZ())) {
            return Optional.of(recipe.stationType());
        }
        return Optional.empty();
    }

    private boolean recipeUnlocked(CraftingRecipe recipe, Optional<CraftingStationType> station) {
        RecipeUnlock unlock = recipe.unlockCondition();
        return switch (unlock) {
            case ALWAYS -> true;
            case NEAR_STATION -> station.filter(recipe::isAvailableAt).isPresent();
            case DISCOVERED_ITEM -> discoveredRecipeKey(recipe) || recipeIngredientsPresent(recipe);
            case FOUND_LORE_NOTE, BIOME_DISCOVERED -> discoveredRecipeKey(recipe);
        };
    }

    private boolean discoveredRecipeKey(CraftingRecipe recipe) {
        return discoveredRecipes.contains(recipe.key());
    }

    private boolean recipeIngredientsPresent(CraftingRecipe recipe) {
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            if (inventory.count(ingredient.itemId()) <= 0) {
                return false;
            }
        }
        return true;
    }

    public static void broadcast(GamePacket packet) {
        broadcastToLoggedIn(packet);
    }

    public static void broadcast(ServerWorld world, GamePacket packet) {
        broadcastToLoggedInWorld(world, packet);
    }

    public static void broadcastEntitySnapshots(ServerWorld world, ServerEntityTracker entityTracker) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            ChannelHandlerContext ctx = handler.context;
            if (ctx == null || !handler.loggedIn || handler.world != world) {
                continue;
            }
            handler.sendEntitySnapshots(ctx);
        }
    }

    public static List<InterestStats> interestStats(ServerWorld world) {
        return ACTIVE_HANDLERS.stream()
                .filter(handler -> handler.world == world && handler.loggedIn)
                .map(ServerConnectionHandler::interestStats)
                .toList();
    }

    public static void broadcastServerStats(ServerWorld world) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            ChannelHandlerContext ctx = handler.context;
            if (ctx == null || !handler.loggedIn || handler.world != world) {
                continue;
            }
            handler.sendServerStats(ctx);
        }
    }

    private static void invalidateStorageUiForWorld(ServerWorld world, int x, int y, int z) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            ChannelHandlerContext ctx = handler.context;
            if (ctx == null || !handler.loggedIn || handler.world != world) {
                continue;
            }
            handler.closeOpenStorage(ctx, x, y, z);
        }
    }

    public static MovementRejectStats movementRejectStats(ServerWorld world) {
        int totalRejects = 0;
        int strikes = 0;
        int rateRejects = 0;
        int verticalBoundsRejects = 0;
        int collisionRejects = 0;
        int groundRejects = 0;
        int waterRejects = 0;
        int initialSyncRejects = 0;
        int sequenceRejects = 0;
        int pathRejects = 0;
        int upwardRejects = 0;
        int speedRejects = 0;
        int accelerationRejects = 0;
        int nonFiniteRejects = 0;
        MovementRejectReason lastReason = MovementRejectReason.NONE;
        AuthoritativeCorrectionClass lastCorrectionClass = AuthoritativeCorrectionClass.SOFT;
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world != world || !handler.loggedIn) {
                continue;
            }
            MovementRejectStats stats = handler.movementRejectStats();
            totalRejects += stats.totalRejects();
            strikes += stats.strikes();
            rateRejects += stats.rateRejects();
            verticalBoundsRejects += stats.verticalBoundsRejects();
            collisionRejects += stats.collisionRejects();
            groundRejects += stats.groundRejects();
            waterRejects += stats.waterRejects();
            initialSyncRejects += stats.initialSyncRejects();
            sequenceRejects += stats.sequenceRejects();
            pathRejects += stats.pathRejects();
            upwardRejects += stats.upwardRejects();
            speedRejects += stats.speedRejects();
            accelerationRejects += stats.accelerationRejects();
            nonFiniteRejects += stats.nonFiniteRejects();
            if (stats.lastReason() != MovementRejectReason.NONE) {
                lastReason = stats.lastReason();
                lastCorrectionClass = stats.lastCorrectionClass();
            }
        }
        return new MovementRejectStats(
                totalRejects,
                strikes,
                lastReason,
                lastCorrectionClass,
                rateRejects,
                verticalBoundsRejects,
                collisionRejects,
                groundRejects,
                waterRejects,
                initialSyncRejects,
                sequenceRejects,
                pathRejects,
                upwardRejects,
                speedRejects,
                accelerationRejects,
                nonFiniteRejects
        );
    }

    public InterestStats interestStats() {
        long packetCount = sentPackets.get();
        long byteCount = estimatedPacketBytes.get();
        long elapsedNanos = Math.max(1L, System.nanoTime() - packetMetricsStartedAtNanos);
        return new InterestStats(
                sentChunks.size(),
                sentEntitySnapshotPackets.get(),
                sentEntitySnapshots.get(),
                sentChunkPackets.get(),
                sentBlockUpdates.get(),
                discardedUpdatesOutsideInterest.get(),
                rejectedChunkRequests.get(),
                failedChunkRequests.get(),
                packetCount,
                byteCount,
                packetCount == 0L ? 0L : byteCount / packetCount,
                packetCount * 1_000_000_000.0 / elapsedNanos
        );
    }

    public List<String> interestDebugLog() {
        synchronized (interestDebugLog) {
            return List.copyOf(interestDebugLog);
        }
    }

    public MovementRejectStats movementRejectStats() {
        return new MovementRejectStats(
                movementRejectCounts.values().stream().mapToInt(Integer::intValue).sum(),
                movementRejectStrikes,
                lastMovementRejectReason,
                lastMovementCorrectionClass,
                movementRejectCount(MovementRejectReason.RATE),
                movementRejectCount(MovementRejectReason.VERTICAL_BOUNDS),
                movementRejectCount(MovementRejectReason.COLLISION),
                movementRejectCount(MovementRejectReason.GROUND),
                movementRejectCount(MovementRejectReason.WATER),
                movementRejectCount(MovementRejectReason.INITIAL_SYNC),
                movementRejectCount(MovementRejectReason.SEQUENCE),
                movementRejectCount(MovementRejectReason.PATH),
                movementRejectCount(MovementRejectReason.UPWARD),
                movementRejectCount(MovementRejectReason.SPEED),
                movementRejectCount(MovementRejectReason.ACCELERATION),
                movementRejectCount(MovementRejectReason.NON_FINITE)
        );
    }

    public IntentRejectStats intentRejectStats() {
        return new IntentRejectStats(
                intentRateRejectCounts.values().stream().mapToInt(Integer::intValue).sum(),
                intentRateRejectCount(ClientIntent.BLOCK_ACTION),
                intentRateRejectCount(ClientIntent.BLOCK_INTERACT),
                intentRateRejectCount(ClientIntent.STORAGE_OPEN),
                intentRateRejectCount(ClientIntent.STORAGE_TRANSFER),
                intentRateRejectCount(ClientIntent.CRAFT),
                intentRateRejectCount(ClientIntent.COOK),
                intentRateRejectCount(ClientIntent.PROJECTILE_SHOOT),
                intentRateRejectCount(ClientIntent.SLEEP),
                intentRateRejectCount(ClientIntent.ENTITY_INTERACT),
                intentRateRejectCount(ClientIntent.CHAT)
        );
    }

    public static boolean consumeEntitySnapshotDirty(ServerWorld world) {
        return DIRTY_ENTITY_SNAPSHOT_WORLDS.remove(world);
    }

    public static void tickCookingJobs(double nowSeconds) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            handler.tickCooking(nowSeconds);
        }
    }

    public static void saveActivePlayers(ServerWorld world) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world == world && handler.loggedIn) {
                handler.savePlayerQuietly();
            }
        }
    }

    void tickCooking(double nowSeconds) {
        if (pendingCook == null || context == null || !loggedIn || nowSeconds < pendingCook.completeAtSeconds()) {
            return;
        }
        if (pendingCook.recipe().stationType() == CraftingStationType.CAMPFIRE) {
            world.tickCampfires(nowSeconds).forEach(this::broadcastToLoggedInWorld);
        }
        Optional<BlockType> station = world.blockAt(pendingCook.stationX(), pendingCook.stationY(), pendingCook.stationZ());
        if (station.isEmpty()
                || !CraftingStationRules.accepts(pendingCook.recipe().stationType(), station.get().id())
                || !stationBlockEntityMatches(pendingCook.recipe().stationType(), pendingCook.stationX(), pendingCook.stationY(), pendingCook.stationZ())) {
            int stationX = pendingCook.stationX();
            int stationY = pendingCook.stationY();
            int stationZ = pendingCook.stationZ();
            CraftingStationType stationType = pendingCook.recipe().stationType();
            pendingCook = null;
            if (stationType == CraftingStationType.CAMPFIRE) {
                sendCampfireStatus(context, stationX, stationY, stationZ, nowSeconds);
            }
            sendInventory(context);
            return;
        }
        if (inventory.addStack(pendingCook.recipe().result(), items) != 0) {
            return;
        }
        int stationX = pendingCook.stationX();
        int stationY = pendingCook.stationY();
        int stationZ = pendingCook.stationZ();
        CraftingStationType stationType = pendingCook.recipe().stationType();
        pendingCook = null;
        if (stationType == CraftingStationType.CAMPFIRE) {
            sendCampfireStatus(context, stationX, stationY, stationZ, nowSeconds);
        }
        sendInventory(context);
    }

    private boolean handlePhysicsDebugCommand(ChannelHandlerContext ctx, String message) {
        String trimmed = message == null ? "" : message.trim();
        if (!trimmed.startsWith("!phys") && !trimmed.startsWith("/phys")) {
            return false;
        }
        String[] parts = trimmed.split("\\s+");
        String action = parts.length < 2 ? "help" : parts[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "projectile" -> {
                double[] direction = lookDirection(playerYaw, playerPitch);
                entityTracker.spawnArrowProjectile(playerId, playerX, playerY - 0.18, playerZ, direction[0], direction[1], direction[2]);
                markEntitySnapshotsDirty(world);
                sendEntitySnapshots(ctx);
                ctx.writeAndFlush(new GamePacket.Chat("[server] physics projectile spawned"));
            }
            case "entity" -> {
                String typeKey = parts.length >= 3 ? parts[2] : "voxel:cozy_sheep";
                double[] direction = lookDirection(playerYaw, 0.0f);
                EntitySnapshot snapshot = entityTracker.spawnDebugAmbient(typeKey, playerX + direction[0] * 2.0, playerY - PLAYER_PHYSICS.bounds().eyeHeight(), playerZ + direction[2] * 2.0);
                markEntitySnapshotsDirty(world);
                sendEntitySnapshots(ctx);
                ctx.writeAndFlush(new GamePacket.Chat("[server] physics entity " + snapshot.entityId() + " spawned"));
            }
            case "water" -> {
                int x = (int) Math.floor(playerX);
                int y = (int) Math.floor(PLAYER_PHYSICS.bounds().minY(playerY) + 0.05);
                int z = (int) Math.floor(playerZ);
                world.setBlock(x, y, z, Blocks.WATER);
                broadcastToLoggedInWorld(new GamePacket.BlockUpdate(x, y, z, Blocks.WATER));
                ctx.writeAndFlush(new GamePacket.Chat("[server] physics water forced at " + x + " " + y + " " + z));
            }
            case "unloaded" -> {
                sentChunks.clear();
                streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(playerX), (int) Math.floor(playerZ)));
                ctx.writeAndFlush(new GamePacket.Chat("[server] physics chunk stream reset"));
            }
            case "stats" -> {
                MovementRejectStats movement = movementRejectStats();
                ServerEntityTracker.AmbientTickStats ambient = entityTracker.lastAmbientTickStats();
                ServerEntityTracker.ProjectileTickStats projectiles = entityTracker.lastProjectileTickStats();
                ctx.writeAndFlush(new GamePacket.Chat("[server] phys rejects=" + movement.totalRejects()
                        + " strikes=" + movement.strikes()
                        + " active=" + ambient.activeAmbient()
                        + " parked=" + ambient.parkedAmbient()
                        + " projectiles=" + entityTracker.projectileCount()
                        + " hits=" + projectiles.emittedHits()));
            }
            default -> ctx.writeAndFlush(new GamePacket.Chat("[server] phys: projectile | entity [type] | water | unloaded | stats"));
        }
        return true;
    }

    private void sendCampfireStatus(ChannelHandlerContext ctx, int x, int y, int z, double nowSeconds) {
        ctx.writeAndFlush(campfireStatusPacket(x, y, z, nowSeconds));
        recordSentPacket(48L);
    }

    private void closeOpenStorage(ChannelHandlerContext ctx, int x, int y, int z) {
        if (openStorage == null || !openStorage.matches(x, y, z)) {
            return;
        }
        openStorage = null;
        ctx.writeAndFlush(new GamePacket.StorageClose(x, y, z));
        recordSentPacket(16L);
    }

    private GamePacket.CampfireStatus campfireStatusPacket(int x, int y, int z, double nowSeconds) {
        double fuelRemaining = world.campfireFuelSecondsRemaining(x, y, z, nowSeconds).orElse(0.0);
        Optional<BlockType> block = world.blockAt(x, y, z);
        boolean active = block.isPresent() && CampfireRules.isActiveCampfire(block.get().id());
        if (pendingCook != null && pendingCook.stationX() == x && pendingCook.stationY() == y && pendingCook.stationZ() == z) {
            double totalSeconds = recipeCookSeconds(pendingCook.recipe());
            double remainingSeconds = Math.max(0.0, Math.min(totalSeconds, pendingCook.completeAtSeconds() - nowSeconds));
            return new GamePacket.CampfireStatus(
                    x,
                    y,
                    z,
                    active,
                    fuelRemaining,
                    pendingCook.recipe().key(),
                    totalSeconds,
                    remainingSeconds
            );
        }
        return new GamePacket.CampfireStatus(x, y, z, active, fuelRemaining, "", 0.0, 0.0);
    }

    private static double recipeCookSeconds(CraftingRecipe recipe) {
        return Math.max(1, recipe.craftingTimeTicks()) / 20.0;
    }

    private Optional<Short> itemIdForKey(String itemKey) {
        return items.findByKey(itemKey).map(ItemType::id);
    }

    private boolean stationBlockEntityMatches(CraftingStationType stationType, int x, int y, int z) {
        return world.blockEntityTypeAt(x, y, z)
                .filter(type -> switch (stationType) {
                    case CAMPFIRE -> type == BlockEntityType.CAMPFIRE;
                    case COOKING_POT -> type == BlockEntityType.COOKING_POT;
                    case WORKBENCH -> type == BlockEntityType.WORKBENCH;
                    case FORGE -> type == BlockEntityType.FORGE;
                    case INVENTORY, CRAFTING_TABLE -> false;
                })
                .isPresent();
    }

    private boolean validInventorySlots(List<Integer> slots) {
        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.size()) {
                return false;
            }
        }
        return !slots.isEmpty();
    }

    private boolean consumeIngredientsFromSlots(Inventory targetInventory, CraftingRecipe recipe, List<Integer> inputSlots) {
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            int remaining = ingredient.count();
            for (int slot : inputSlots) {
                ItemStack stack = targetInventory.slot(slot);
                if (stack.itemId() != ingredient.itemId()) {
                    continue;
                }
                int removed = Math.min(remaining, stack.count());
                if (removed > 0 && targetInventory.removeFromSlot(slot, removed)) {
                    remaining -= removed;
                }
                if (remaining == 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> distinctSlots(List<Integer> slots) {
        return slots.stream().distinct().toList();
    }

    private void sendInventory(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new GamePacket.InventorySnapshot(inventory.slots()));
        recordSentPacket(16L + inventory.size() * 8L);
    }

    private boolean loadPlayerSave() {
        if (playerSaveDirectory == null || playerId == null) {
            return false;
        }
        try {
            Optional<PlayerSave> save = PlayerSaveStore.load(playerSaveDirectory, playerId);
            if (save.isEmpty()) {
                return false;
            }
            applyPlayerSave(save.get());
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void applyPlayerSave(PlayerSave save) {
        playerName = save.playerName();
        gameMode = save.gameMode();
        LoadedPosition loadedPosition = safeLoadedPosition(save.x(), save.y(), save.z(), gameMode);
        playerX = loadedPosition.x();
        playerY = loadedPosition.y();
        playerZ = loadedPosition.z();
        playerYaw = save.yaw();
        playerPitch = save.pitch();
        hotbarSelection = Math.max(0, Math.min(inventory.size() - 1, save.hotbarSelection()));
        spawnPoint = save.spawnPoint();
        discoveredRecipes = List.copyOf(save.discoveredRecipes());
        discoveredBiomes = List.copyOf(save.discoveredBiomes());
        journalEntries = List.copyOf(save.journalEntries());
        achievedMilestones = List.copyOf(save.achievedMilestones());
        completedGoals = List.copyOf(save.completedGoals());
        creatureFriendships.clear();
        for (PlayerSave.CreatureFriendshipState friendship : save.creatureFriendships()) {
            creatureFriendships.put(friendship.entityKey(), friendship);
        }
        lastWorldKey = save.lastWorldKey();
        inventory.clear();
        for (int i = 0; i < Math.min(inventory.size(), save.inventory().size()); i++) {
            inventory.setSlot(i, save.inventory().get(i));
        }
        survivalState.loadPersistentStats(
                save.survival().health(),
                save.survival().hunger(),
                save.survival().stamina(),
                save.survival().breath(),
                save.statusEffects()
        );
    }

    private void savePlayerQuietly() {
        if (playerSaveDirectory == null || playerId == null) {
            return;
        }
        PlayerSave snapshot = playerSaveSnapshot();
        if (saveQueue != null) {
            PlayerSaveStore.saveQueued(saveQueue, playerSaveDirectory, snapshot)
                    .exceptionally(exception -> {
                        System.err.println("Failed to save Adventura player " + playerId + ": " + saveFailureMessage(exception));
                        return null;
                    });
            return;
        }
        try {
            PlayerSaveStore.save(playerSaveDirectory, snapshot);
        } catch (IOException exception) {
            System.err.println("Failed to save Adventura player " + playerId + ": " + exception.getMessage());
        }
    }

    private static String saveFailureMessage(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private PlayerSave playerSaveSnapshot() {
        return new PlayerSave(
                SaveMetadata.CURRENT_SAVE_VERSION,
                playerId,
                playerName,
                playerX,
                playerY,
                playerZ,
                playerYaw,
                playerPitch,
                inventory.slots(),
                hotbarSelection,
                new PlayerSave.SurvivalStats(
                        survivalState.health(),
                        survivalState.hunger(),
                        survivalState.stamina(),
                        survivalState.breath()
                ),
                spawnPoint,
                gameMode,
                discoveredRecipes,
                discoveredBiomes,
                journalEntries,
                achievedMilestones,
                completedGoals,
                survivalState.statusEffectSaveStates(),
                creatureFriendshipSnapshots(),
                lastWorldKey
        );
    }

    private List<PlayerSave.CreatureFriendshipState> creatureFriendshipSnapshots() {
        return creatureFriendships.values().stream()
                .sorted(Comparator.comparing(PlayerSave.CreatureFriendshipState::entityKey))
                .toList();
    }

    private LoadedPosition safeLoadedPosition(double x, double y, double z, String modeKey) {
        if (safePlayerLoadPosition(x, y, z, modeKey)) {
            return new LoadedPosition(x, y, z);
        }
        var spawn = world.spawnPoint();
        return new LoadedPosition(spawn.eyeX(), spawn.eyeY(), spawn.eyeZ());
    }

    private boolean safePlayerLoadPosition(double x, double y, double z, String modeKey) {
        if (!PlayerMovementRules.isFinite(x, y, z, 0.0f, 0.0f)
                || !PlayerMovementRules.withinVerticalBounds(y, PLAYER_PHYSICS, world.dimension().minY(), world.dimension().maxYExclusive())) {
            return false;
        }
        PlayerMovementRules.MovementMode mode = movementModeFor(modeKey);
        if (mode != PlayerMovementRules.MovementMode.SPECTATOR
                && world.collidesPlayer(x, y, z, PLAYER_PHYSICS.bounds())) {
            return false;
        }
        PlayerWaterState waterState = world.playerWaterState(x, y, z, PLAYER_PHYSICS.bounds());
        if (mode != PlayerMovementRules.MovementMode.SPECTATOR && waterState.movementAffected()) {
            return false;
        }
        return true;
    }

    private void handlePlayerMove(ChannelHandlerContext ctx, GamePacket.PlayerMove move) {
        if (!PlayerMovementRules.isFinite(move.x(), move.y(), move.z(), move.yaw(), move.pitch())) {
            recordMovementReject(MovementRejectReason.NON_FINITE, move);
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        MovementRejectReason rejectReason = validatePlayerMove(move, now);
        if (rejectReason != MovementRejectReason.NONE) {
            recordMovementReject(rejectReason, move);
            sendAuthoritativePlayerPosition(ctx, move.sequence(), lastMovementCorrectionClass);
            sendPlayerStats(ctx, now, false);
            return;
        }
        movementRejectStrikes = Math.max(0, movementRejectStrikes - 1);
        boolean moving = movedEnough(move.x(), move.y(), move.z());
        if (moving) {
            sleepReady = false;
        }
        boolean hadAcceptedMove = Double.isFinite(lastAcceptedMoveTime);
        double previousX = playerX;
        double previousY = playerY;
        double previousZ = playerZ;
        double moveDeltaSeconds = hadAcceptedMove ? now - lastAcceptedMoveTime : 0.0;
        PlayerMovementRules.MovementMode movementMode = movementMode();
        boolean hasGroundSupport = movementMode != PlayerMovementRules.MovementMode.SPECTATOR
                && hasGroundSupport(move.x(), move.y(), move.z());
        PlayerWaterState waterState = world.playerWaterState(move.x(), move.y(), move.z(), PLAYER_PHYSICS.bounds());
        playerX = move.x();
        playerY = move.y();
        playerZ = move.z();
        playerYaw = move.yaw();
        playerPitch = move.pitch();
        lastAcceptedGrounded = hasGroundSupport;
        lastAcceptedMoveDeltaX = hadAcceptedMove ? move.x() - previousX : 0.0;
        lastAcceptedMoveDeltaY = hadAcceptedMove ? move.y() - previousY : 0.0;
        lastAcceptedMoveDeltaZ = hadAcceptedMove ? move.z() - previousZ : 0.0;
        lastAcceptedMoveDeltaSeconds = moveDeltaSeconds;
        hasAcceptedMovementSample = hadAcceptedMove;
        if (move.sequence() > 0L) {
            lastAcceptedMoveSequence = move.sequence();
        }
        lastAcceptedMoveTime = now;
        updateSurvival(now, moving);
        boolean fallDamaged = movementMode == PlayerMovementRules.MovementMode.SURVIVAL
                && updateFallImpact(previousY, move.y(), hasGroundSupport, waterState);
        entityTracker.updatePlayer(playerId, move.x(), move.y(), move.z(), move.yaw(), move.pitch());
        if (collectNearbyItemDrops()) {
            sendInventory(ctx);
            markEntitySnapshotsDirty(world);
        }
        streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(move.x()), (int) Math.floor(move.z())));
        sendAuthoritativePlayerPosition(ctx, move.sequence(), waterState);
        sendPlayerStats(ctx, now, fallDamaged);
        markEntitySnapshotsDirty(world);
    }

    private MovementRejectReason validatePlayerMove(GamePacket.PlayerMove move, double now) {
        if (!acceptMovementRate(now)) {
            return MovementRejectReason.RATE;
        }
        PlayerMovementRules.MovementMode movementMode = movementMode();
        if (!PlayerMovementRules.withinVerticalBounds(
                move.y(),
                PLAYER_PHYSICS,
                world.dimension().minY(),
                world.dimension().maxYExclusive()
        )) {
            return MovementRejectReason.VERTICAL_BOUNDS;
        }
        if (movementMode != PlayerMovementRules.MovementMode.SPECTATOR
                && world.collidesPlayer(move.x(), move.y(), move.z(), PLAYER_PHYSICS.bounds())) {
            return MovementRejectReason.COLLISION;
        }
        boolean hasGroundSupport = movementMode != PlayerMovementRules.MovementMode.SPECTATOR
                && hasGroundSupport(move.x(), move.y(), move.z());
        if (movementMode == PlayerMovementRules.MovementMode.SURVIVAL
                && !PlayerMovementRules.groundedClaimPlausible(move.onGround(), hasGroundSupport)) {
            return MovementRejectReason.GROUND;
        }
        PlayerWaterState waterState = world.playerWaterState(move.x(), move.y(), move.z(), PLAYER_PHYSICS.bounds());
        if (movementMode == PlayerMovementRules.MovementMode.SURVIVAL
                && !PlayerMovementRules.waterStateClaimPlausible(move.waterState(), waterState)) {
            return MovementRejectReason.WATER;
        }
        if (!Double.isFinite(lastAcceptedMoveTime)) {
            return PlayerMovementRules.withinInitialSyncDistance(
                    playerX,
                    playerY,
                    playerZ,
                    move.x(),
                    move.y(),
                    move.z(),
                    INITIAL_MOVE_SYNC_RADIUS
            ) ? MovementRejectReason.NONE : MovementRejectReason.INITIAL_SYNC;
        }
        if (move.sequence() > 0L && move.sequence() <= lastAcceptedMoveSequence) {
            return MovementRejectReason.SEQUENCE;
        }
        if (movementMode != PlayerMovementRules.MovementMode.SPECTATOR && !world.playerPathClear(
                playerX,
                playerY,
                playerZ,
                move.x(),
                move.y(),
                move.z(),
                PLAYER_PHYSICS.bounds(),
                PLAYER_PHYSICS.maxCollisionStep()
        )) {
            return MovementRejectReason.PATH;
        }
        boolean movementAssist = playerTouchesWater(playerX, playerY, playerZ)
                || PlayerMovementRules.waterMovementAssist(waterState);
        if (movementMode == PlayerMovementRules.MovementMode.SURVIVAL
                && !PlayerMovementRules.upwardMovementPlausible(playerY, move.y(), lastAcceptedGrounded, lastAcceptedMoveDeltaY, movementAssist)) {
            return MovementRejectReason.UPWARD;
        }
        double deltaSeconds = now - lastAcceptedMoveTime;
        if (!PlayerMovementRules.isPlausibleModeDelta(
                playerX,
                playerY,
                playerZ,
                move.x(),
                move.y(),
                move.z(),
                deltaSeconds,
                PLAYER_PHYSICS,
                waterState,
                movementMode
        )) {
            return MovementRejectReason.SPEED;
        }
        return !hasAcceptedMovementSample || PlayerMovementRules.isPlausibleHorizontalAcceleration(
                lastAcceptedMoveDeltaX,
                lastAcceptedMoveDeltaZ,
                lastAcceptedMoveDeltaSeconds,
                move.x() - playerX,
                move.z() - playerZ,
                deltaSeconds,
                PLAYER_PHYSICS,
                movementMode
        ) ? MovementRejectReason.NONE : MovementRejectReason.ACCELERATION;
    }

    private void recordMovementReject(MovementRejectReason reason, GamePacket.PlayerMove move) {
        MovementRejectReason safeReason = reason == null ? MovementRejectReason.NONE : reason;
        if (safeReason == MovementRejectReason.NONE) {
            return;
        }
        movementRejectCounts.merge(safeReason, 1, Integer::sum);
        movementRejectStrikes = Math.min(100, movementRejectStrikes + strikeWeight(safeReason));
        lastMovementRejectReason = safeReason;
        lastMovementCorrectionClass = correctionClassFor(move);
    }

    private int movementRejectCount(MovementRejectReason reason) {
        return movementRejectCounts.getOrDefault(reason, 0);
    }

    private static int strikeWeight(MovementRejectReason reason) {
        return switch (reason) {
            case RATE, SEQUENCE -> 1;
            case GROUND, WATER, UPWARD -> 2;
            case COLLISION, PATH, SPEED, ACCELERATION -> 3;
            case VERTICAL_BOUNDS, INITIAL_SYNC, NON_FINITE -> 4;
            case NONE -> 0;
        };
    }

    private AuthoritativeCorrectionClass correctionClassFor(GamePacket.PlayerMove move) {
        if (move == null
                || !Double.isFinite(move.x())
                || !Double.isFinite(move.y())
                || !Double.isFinite(move.z())) {
            return AuthoritativeCorrectionClass.RESPAWN_TELEPORT;
        }
        double dx = move.x() - playerX;
        double dy = move.y() - playerY;
        double dz = move.z() - playerZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > INITIAL_MOVE_SYNC_RADIUS * INITIAL_MOVE_SYNC_RADIUS) {
            return AuthoritativeCorrectionClass.RESPAWN_TELEPORT;
        }
        if (distanceSquared > 16.0 || Math.abs(dy) > 4.0) {
            return AuthoritativeCorrectionClass.HARD;
        }
        return AuthoritativeCorrectionClass.SOFT;
    }

    private PlayerMovementRules.MovementMode movementMode() {
        return movementModeFor(gameMode);
    }

    private static PlayerMovementRules.MovementMode movementModeFor(String modeKey) {
        String normalized = modeKey == null ? "" : modeKey.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "creative" -> PlayerMovementRules.MovementMode.FLYING;
            case "spectator" -> PlayerMovementRules.MovementMode.SPECTATOR;
            default -> PlayerMovementRules.MovementMode.SURVIVAL;
        };
    }

    private boolean acceptMovementRate(double now) {
        if (!Double.isFinite(now)) {
            return false;
        }
        if (!Double.isFinite(moveRateWindowStart) || now - moveRateWindowStart >= MOVE_RATE_WINDOW_SECONDS) {
            moveRateWindowStart = now;
            moveRateWindowCount = 1;
            return true;
        }
        moveRateWindowCount++;
        return moveRateWindowCount <= MAX_MOVES_PER_RATE_WINDOW;
    }

    private boolean acceptIntentRate(ClientIntent intent, double now) {
        if (!Double.isFinite(now)) {
            recordIntentRateReject(intent);
            return false;
        }
        IntentRateWindow window = intentRateWindows.get(intent);
        if (window == null || now - window.startedAtSeconds >= INTENT_RATE_WINDOW_SECONDS) {
            intentRateWindows.put(intent, new IntentRateWindow(now, 1));
            return true;
        }
        if (window.count < Integer.MAX_VALUE) {
            window.count++;
        }
        if (window.count <= maxIntentsPerWindow(intent)) {
            return true;
        }
        recordIntentRateReject(intent);
        return false;
    }

    private void recordIntentRateReject(ClientIntent intent) {
        intentRateRejectCounts.merge(intent, 1, Integer::sum);
    }

    private int intentRateRejectCount(ClientIntent intent) {
        return intentRateRejectCounts.getOrDefault(intent, 0);
    }

    private static int maxIntentsPerWindow(ClientIntent intent) {
        return switch (intent) {
            case BLOCK_ACTION -> 20;
            case BLOCK_INTERACT, ENTITY_INTERACT -> 12;
            case STORAGE_OPEN, CRAFT -> 8;
            case STORAGE_TRANSFER -> 20;
            case COOK -> 6;
            case PROJECTILE_SHOOT -> 10;
            case SLEEP -> 4;
            case CHAT -> 5;
        };
    }

    private boolean playerTouchesWater(double x, double y, double z) {
        return PlayerMovementRules.waterMovementAssist(world.playerWaterState(x, y, z, PLAYER_PHYSICS.bounds()));
    }

    private boolean hasGroundSupport(double x, double y, double z) {
        return world.collidesPlayer(
                x,
                y - PLAYER_PHYSICS.groundProbeDistance(),
                z,
                PLAYER_PHYSICS.bounds()
        );
    }

    private boolean updateFallImpact(double previousY, double currentY, boolean hasGroundSupport, PlayerWaterState waterState) {
        boolean touchesWater = PlayerMovementRules.waterMovementAssist(waterState);
        if (hasGroundSupport) {
            boolean damaged = false;
            if (Double.isFinite(fallStartY)) {
                fallTouchedWater |= touchesWater;
                damaged = survivalState.applyFallImpact(fallStartY - currentY, fallTouchedWater) > 0;
            }
            fallStartY = Double.NaN;
            fallTouchedWater = false;
            return damaged;
        }
        double airborneStartY = Math.max(previousY, currentY);
        if (Double.isFinite(fallStartY)) {
            fallStartY = Math.max(fallStartY, currentY);
            fallTouchedWater |= touchesWater;
        } else {
            fallStartY = airborneStartY;
            fallTouchedWater = touchesWater;
        }
        return false;
    }

    private void sendAuthoritativePlayerPosition(ChannelHandlerContext ctx, long sequence) {
        sendAuthoritativePlayerPosition(ctx, sequence, AuthoritativeCorrectionClass.SOFT);
    }

    private void sendAuthoritativePlayerPosition(ChannelHandlerContext ctx, long sequence, AuthoritativeCorrectionClass correctionClass) {
        sendAuthoritativePlayerPosition(
                ctx,
                sequence,
                world.playerWaterState(playerX, playerY, playerZ, PLAYER_PHYSICS.bounds()),
                correctionClass
        );
    }

    private void sendAuthoritativePlayerPosition(ChannelHandlerContext ctx, long sequence, PlayerWaterState waterState) {
        sendAuthoritativePlayerPosition(ctx, sequence, waterState, AuthoritativeCorrectionClass.SOFT);
    }

    private void sendAuthoritativePlayerPosition(
            ChannelHandlerContext ctx,
            long sequence,
            PlayerWaterState waterState,
            AuthoritativeCorrectionClass correctionClass
    ) {
        ctx.writeAndFlush(new GamePacket.PlayerPositionSnapshot(
                sequence,
                playerX,
                playerY,
                playerZ,
                playerYaw,
                playerPitch,
                lastAcceptedGrounded,
                waterState,
                movementCorrection(correctionClass)
        ));
        recordSentPacket(64L);
    }

    private static GamePacket.MovementCorrection movementCorrection(AuthoritativeCorrectionClass correctionClass) {
        return switch (correctionClass == null ? AuthoritativeCorrectionClass.SOFT : correctionClass) {
            case SOFT -> GamePacket.MovementCorrection.SOFT;
            case HARD -> GamePacket.MovementCorrection.HARD;
            case RESPAWN_TELEPORT -> GamePacket.MovementCorrection.RESPAWN_TELEPORT;
        };
    }

    private void sendPlayerStats(ChannelHandlerContext ctx, double now, boolean force) {
        int comfort = survivalState.comfort();
        if (!force && now < nextStatsSyncTime && comfort == lastSyncedComfort) {
            return;
        }
        lastSyncedComfort = comfort;
        nextStatsSyncTime = now + 1.0;
        ctx.writeAndFlush(survivalState.snapshot());
        recordSentPacket(32L);
    }

    private void updateSurvival(double now, boolean moving) {
        long tick = survivalTick++;
        PlayerWaterState waterState = world.playerWaterState(playerX, playerY, playerZ, PLAYER_PHYSICS.bounds());
        boolean sprinting = sprintingFromAcceptedMove(waterState);
        Set<StatusEffectType> beforeEffects = survivalState.activeStatusTypes();
        int healthBefore = survivalState.health();
        int hungerBefore = survivalState.hunger();
        int staminaBefore = survivalState.stamina();
        int breathBefore = survivalState.breath();
        if (!Double.isFinite(lastSurvivalUpdateTime)) {
            survivalState.updateComfort(world.comfortAt(playerX, playerY, playerZ), tick);
            lastSurvivalUpdateTime = now;
            applyEnvironmentEffects(now, waterState);
            return;
        }
        if (survivalState.shouldScanComfort(tick)) {
            survivalState.tick(now - lastSurvivalUpdateTime, world.comfortAt(playerX, playerY, playerZ), tick, moving, waterState.headUnderwater(), sprinting);
        } else {
            survivalState.tick(now - lastSurvivalUpdateTime, moving, waterState.headUnderwater(), sprinting);
        }
        lastSurvivalUpdateTime = now;
        emitExpiredStatusEffects(beforeEffects, survivalState.activeStatusTypes(), now);
        applyEnvironmentEffects(now, waterState);
        if (statsChanged(healthBefore, hungerBefore, staminaBefore, breathBefore)) {
            ChannelHandlerContext ctx = context;
            if (ctx != null) {
                sendPlayerStats(ctx, now, true);
            }
        }
    }

    private void applyEnvironmentEffects(double now, PlayerWaterState waterState) {
        if (!loggedIn) {
            return;
        }
        EnvironmentHazardRules.Hazard hazard = world.environmentHazardAtPlayer(playerX, playerY, playerZ, PLAYER_PHYSICS.bounds());
        for (StatusEffectType type : StatusEffectEnvironmentRules.effectsFor(new StatusEffectEnvironmentRules.EnvironmentContext(
                waterState.movementAffected(),
                hazard.hot(),
                hazard.cold(),
                world.biomeKeyAt(playerX, playerZ),
                survivalState.comfort()
        ))) {
            emitStatusEffectChange(survivalState.applyStatusEffect(type), type, now, false);
        }
        if (now < nextEnvironmentDamageTime || hazard.damagePerPulse() <= 0 || hazard.hot()) {
            return;
        }
        int damage = survivalState.applyEnvironmentalDamage(hazard.damagePerPulse());
        if (damage <= 0) {
            return;
        }
        nextEnvironmentDamageTime = now + EnvironmentHazardRules.DAMAGE_COOLDOWN_SECONDS;
        ChannelHandlerContext ctx = context;
        if (ctx != null) {
            sendGameplayEvent(new GameplayEvent.Damage(gameplayEventSequence++, 0L, damage, hazard.key()));
            sendPlayerStats(ctx, now, true);
        }
    }

    private boolean statsChanged(int healthBefore, int hungerBefore, int staminaBefore, int breathBefore) {
        return healthBefore != survivalState.health()
                || hungerBefore != survivalState.hunger()
                || staminaBefore != survivalState.stamina()
                || breathBefore != survivalState.breath();
    }

    private void emitExpiredStatusEffects(Set<StatusEffectType> beforeEffects, Set<StatusEffectType> afterEffects, double now) {
        for (StatusEffectType type : beforeEffects) {
            if (!afterEffects.contains(type)) {
                sendStatusEffectEvent(type, "expired", 1);
            }
        }
    }

    private void emitStatusEffectChange(
            ServerPlayerSurvivalState.StatusEffectChange change,
            StatusEffectType type,
            double now,
            boolean includeRefresh
    ) {
        if (change == ServerPlayerSurvivalState.StatusEffectChange.UNCHANGED) {
            return;
        }
        if (change == ServerPlayerSurvivalState.StatusEffectChange.REFRESHED && !includeRefresh) {
            return;
        }
        String changeKey = change == ServerPlayerSurvivalState.StatusEffectChange.APPLIED ? "applied" : "refreshed";
        int intensity = survivalState.statusEffects()
                .find(type)
                .map(effect -> effect.intensity())
                .orElse(1);
        sendStatusEffectEvent(type, changeKey, intensity);
    }

    private void sendStatusEffectEvent(StatusEffectType type, String changeKey, int intensity) {
        if (playerId == null) {
            return;
        }
        sendGameplayEvent(new GameplayEvent.StatusEffectChanged(
                gameplayEventSequence++,
                playerId,
                type.key(),
                changeKey,
                Math.max(1, intensity)
        ));
    }

    private void sendGameplayEvent(GameplayEvent event) {
        ChannelHandlerContext ctx = context;
        if (ctx == null) {
            return;
        }
        ctx.writeAndFlush(new GamePacket.GameplayEvents(List.of(event)));
        recordSentPacket(64L);
    }

    private boolean sprintingFromAcceptedMove(PlayerWaterState waterState) {
        if (!hasAcceptedMovementSample
                || PlayerMovementRules.waterMovementAssist(waterState)
                || lastAcceptedMoveDeltaSeconds <= 0.0) {
            return false;
        }
        double seconds = Math.max(1.0 / 20.0, Math.min(lastAcceptedMoveDeltaSeconds, 0.5));
        double horizontalDistance = Math.sqrt(
                lastAcceptedMoveDeltaX * lastAcceptedMoveDeltaX
                        + lastAcceptedMoveDeltaZ * lastAcceptedMoveDeltaZ
        );
        double horizontalSpeed = horizontalDistance / seconds;
        return horizontalSpeed > PLAYER_PHYSICS.walkSpeed() * 1.08 && survivalState.canSprint();
    }

    private boolean collectNearbyItemDrops() {
        boolean collected = false;
        for (DroppedItemEntity drop : entityTracker.itemDropsNear(playerX, playerY, playerZ, 1.45, survivalTick)) {
            if (!inventory.canAdd(drop.stack().itemId(), drop.stack().count(), items)) {
                continue;
            }
            Optional<DroppedItemEntity> claimed = entityTracker.claimItemDrop(drop.entityId());
            if (claimed.isEmpty()) {
                continue;
            }
            if (inventory.addStack(drop.stack(), items) == 0) {
                collected = true;
            }
        }
        return collected;
    }

    private boolean movedEnough(double x, double y, double z) {
        double dx = x - playerX;
        double dy = y - playerY;
        double dz = z - playerZ;
        return dx * dx + dy * dy + dz * dz > 0.0001;
    }

    private void sendEntitySnapshots(ChannelHandlerContext ctx) {
        List<EntitySnapshot> snapshots = visibleEntitySnapshots(entityTracker);
        ctx.writeAndFlush(new GamePacket.EntitySnapshots(snapshots));
        sentEntitySnapshotPackets.incrementAndGet();
        sentEntitySnapshots.addAndGet(snapshots.size());
        recordSentPacket(32L + snapshots.size() * 96L);
    }

    private void sendServerStats(ChannelHandlerContext ctx) {
        InterestStats stats = interestStats();
        SaveQueue.SaveQueueStats saveStats = saveQueue == null ? SaveQueue.SaveQueueStats.empty() : saveQueue.stats();
        GamePacket.ServerStatsSnapshot packet = new GamePacket.ServerStatsSnapshot(
                stats.chunkSubscriptions(),
                stats.sentEntitySnapshotPackets(),
                stats.sentEntitySnapshots(),
                stats.sentChunkPackets(),
                stats.sentBlockUpdates(),
                stats.discardedUpdatesOutsideInterest(),
                stats.rejectedChunkRequests(),
                stats.failedChunkRequests(),
                stats.sentPackets(),
                stats.estimatedPacketBytes(),
                stats.averagePacketBytes(),
                stats.packetRatePerSecond(),
                saveStats.pendingWrites(),
                saveStats.runningWrites(),
                saveStats.enqueuedWrites(),
                saveStats.completedWrites(),
                saveStats.failedWrites(),
                saveStats.rejectedWrites(),
                saveStats.writtenBytes(),
                saveStats.totalWriteMilliseconds(),
                saveStats.averageWriteMilliseconds(),
                saveStats.enqueuedWritesPerSecond(),
                saveStats.writtenBytesPerSecond(),
                saveStats.writeMillisecondsPerSecond(),
                saveStats.failedWritesPerSecond()
        );
        ctx.writeAndFlush(packet);
        recordSentPacket(192L);
    }

    private static void markEntitySnapshotsDirty(ServerWorld world) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            if (handler.world == world && handler.loggedIn) {
                DIRTY_ENTITY_SNAPSHOT_WORLDS.add(world);
                return;
            }
        }
    }

    private List<EntitySnapshot> visibleEntitySnapshots(ServerEntityTracker sourceTracker) {
        return sourceTracker.snapshots().stream()
                .filter(this::isEntitySnapshotRelevant)
                .toList();
    }

    private boolean isEntitySnapshotRelevant(EntitySnapshot snapshot) {
        if (playerId != null && playerId.equals(snapshot.ownerPlayerId())) {
            return true;
        }
        double dx = snapshot.x() - playerX;
        double dz = snapshot.z() - playerZ;
        return dx * dx + dz * dz <= ENTITY_SNAPSHOT_RADIUS * ENTITY_SNAPSHOT_RADIUS;
    }

    private void streamChunksAround(ChannelHandlerContext ctx, ChunkPos center) {
        ChunkStreamingRings rings = ChunkStreamingRings.subscription(streamRadiusChunks);
        List<ChunkPos> requestedPositions = rings.positionsInRing(center, ChunkStreamingRings.Ring.RENDER);
        Set<ChunkPos> requestedSet = Set.copyOf(requestedPositions);
        for (ChunkPos sentChunk : List.copyOf(sentChunks)) {
            if (!requestedSet.contains(sentChunk)) {
                recordInterestDebug("filter chunk_subscription outside_interest " + sentChunk.x() + " " + sentChunk.z());
            }
        }
        sentChunks.retainAll(requestedSet);
        for (ChunkPos pos : requestedPositions) {
            if (!sentChunks.add(pos)) {
                continue;
            }
            Optional<CompletableFuture<GamePacket.ChunkData>> request = chunkStreamer.request(world, pos, chunkPriority(pos, center));
            if (request.isEmpty()) {
                sentChunks.remove(pos);
                rejectedChunkRequests.incrementAndGet();
                recordInterestDebug("reject chunk_request backpressure " + pos.x() + " " + pos.z());
                continue;
            }
            CompletableFuture<GamePacket.ChunkData> chunkFuture = request.get();
            if (chunkFuture.isDone()) {
                deliverCompletedChunk(ctx, pos, chunkFuture);
            } else {
                chunkFuture.whenComplete((packet, error) -> ctx.executor().execute(() -> deliverChunk(ctx, pos, packet, error)));
            }
        }
    }

    private void deliverCompletedChunk(ChannelHandlerContext ctx, ChunkPos pos, CompletableFuture<GamePacket.ChunkData> chunkFuture) {
        try {
            deliverChunk(ctx, pos, chunkFuture.join(), null);
        } catch (RuntimeException error) {
            deliverChunk(ctx, pos, null, error);
        }
    }

    private void deliverChunk(ChannelHandlerContext ctx, ChunkPos pos, GamePacket.ChunkData packet, Throwable error) {
        if (error != null) {
            sentChunks.remove(pos);
            failedChunkRequests.incrementAndGet();
            recordInterestDebug("reject chunk_delivery failed " + pos.x() + " " + pos.z());
            return;
        }
        if (context != ctx || !loggedIn || !sentChunks.contains(pos)) {
            recordInterestDebug("filter chunk_delivery outside_interest " + pos.x() + " " + pos.z());
            return;
        }
        ctx.writeAndFlush(packet);
        sentChunkPackets.incrementAndGet();
        recordSentPacket(estimateChunkPacketBytes(packet));
    }

    private static int chunkPriority(ChunkPos pos, ChunkPos center) {
        long priority = ChunkStreamingRings.distanceSquared(center, pos);
        return priority >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) priority;
    }

    private static void broadcastToLoggedIn(GamePacket packet) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            ChannelHandlerContext ctx = handler.context;
            if (ctx == null || !handler.loggedIn) {
                continue;
            }
            ctx.writeAndFlush(packet);
        }
    }

    private static void broadcastToLoggedInWorld(ServerWorld world, GamePacket packet) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            ChannelHandlerContext ctx = handler.context;
            if (ctx == null || !handler.loggedIn || handler.world != world) {
                continue;
            }
            GamePacket outbound = packet;
            if (outbound instanceof GamePacket.BlockUpdate update && !handler.hasSeenChunk(update)) {
                handler.discardedUpdatesOutsideInterest.incrementAndGet();
                ChunkPos chunk = ChunkPos.fromBlock(update.x(), update.z());
                handler.recordInterestDebug("filter block_update outside_chunk_interest "
                        + update.x() + " " + update.y() + " " + update.z()
                        + " chunk=" + chunk.x() + "," + chunk.z());
                continue;
            }
            if (outbound instanceof GamePacket.ProjectileImpact impact && !handler.isProjectileImpactRelevant(impact)) {
                handler.discardedUpdatesOutsideInterest.incrementAndGet();
                ChunkPos chunk = ChunkPos.fromBlock((int) Math.floor(impact.x()), (int) Math.floor(impact.z()));
                handler.recordInterestDebug("filter projectile_impact outside_event_interest "
                        + impact.projectileId()
                        + " chunk=" + chunk.x() + "," + chunk.z());
                continue;
            }
            if (outbound instanceof GamePacket.GameplayEvents events) {
                GamePacket.GameplayEvents filtered = handler.filterGameplayEvents(events);
                int discarded = events.events().size() - filtered.events().size();
                if (discarded > 0) {
                    handler.discardedUpdatesOutsideInterest.addAndGet(discarded);
                    handler.recordInterestDebug("filter gameplay_event outside_event_interest discarded=" + discarded);
                }
                if (filtered.events().isEmpty()) {
                    continue;
                }
                outbound = filtered;
            }
            ctx.writeAndFlush(outbound);
            if (outbound instanceof GamePacket.BlockUpdate) {
                handler.sentBlockUpdates.incrementAndGet();
                handler.recordSentPacket(16L);
            } else if (outbound instanceof GamePacket.ProjectileImpact) {
                handler.recordSentPacket(128L);
            } else if (outbound instanceof GamePacket.GameplayEvents events) {
                handler.recordSentPacket(32L + events.events().size() * 64L);
            }
        }
    }

    private boolean hasSeenChunk(GamePacket.BlockUpdate update) {
        return sentChunks.contains(ChunkPos.fromBlock(update.x(), update.z()));
    }

    private boolean isProjectileImpactRelevant(GamePacket.ProjectileImpact impact) {
        return ServerGameplayEventInterest.isRelevant(impact, gameplayEventViewer());
    }

    private GamePacket.GameplayEvents filterGameplayEvents(GamePacket.GameplayEvents events) {
        ServerGameplayEventInterest.Viewer viewer = gameplayEventViewer();
        List<GameplayEvent> relevantEvents = events.events().stream()
                .filter(event -> ServerGameplayEventInterest.isRelevant(event, viewer))
                .toList();
        if (relevantEvents.size() == events.events().size()) {
            return events;
        }
        return new GamePacket.GameplayEvents(relevantEvents);
    }

    private ServerGameplayEventInterest.Viewer gameplayEventViewer() {
        return new ServerGameplayEventInterest.Viewer(playerId, this::canSeeEntityEvent, this::canSeeEventPosition);
    }

    private boolean canSeeEntityEvent(long entityId) {
        return entityTracker.snapshots().stream()
                .filter(snapshot -> snapshot.entityId() == entityId)
                .anyMatch(this::isEntitySnapshotRelevant);
    }

    private boolean canSeeEventPosition(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return false;
        }
        double dx = x - playerX;
        double dz = z - playerZ;
        if (dx * dx + dz * dz > EVENT_INTEREST_RADIUS * EVENT_INTEREST_RADIUS) {
            return false;
        }
        return sentChunks.contains(ChunkPos.fromBlock((int) Math.floor(x), (int) Math.floor(z)));
    }

    private void broadcastToLoggedInWorld(GamePacket packet) {
        broadcastToLoggedInWorld(world, packet);
    }

    private void recordSentPacket(long estimatedBytes) {
        sentPackets.incrementAndGet();
        estimatedPacketBytes.addAndGet(Math.max(0L, estimatedBytes));
    }

    private void recordInterestDebug(String message) {
        synchronized (interestDebugLog) {
            if (interestDebugLog.size() >= INTEREST_DEBUG_LOG_LIMIT) {
                interestDebugLog.removeFirst();
            }
            interestDebugLog.addLast(message);
        }
    }

    private static long estimateChunkPacketBytes(GamePacket.ChunkData packet) {
        return 32L + packet.blockIds().length * 2L + packet.skyLight().length + packet.blockLight().length;
    }

    private static double[] lookDirection(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double horizontal = Math.cos(pitch);
        return new double[]{
                Math.cos(yaw) * horizontal,
                Math.sin(pitch),
                Math.sin(yaw) * horizontal
        };
    }

    public record InterestStats(
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
    }

    public record MovementRejectStats(
            int totalRejects,
            int strikes,
            MovementRejectReason lastReason,
            AuthoritativeCorrectionClass lastCorrectionClass,
            int rateRejects,
            int verticalBoundsRejects,
            int collisionRejects,
            int groundRejects,
            int waterRejects,
            int initialSyncRejects,
            int sequenceRejects,
            int pathRejects,
            int upwardRejects,
            int speedRejects,
            int accelerationRejects,
            int nonFiniteRejects
    ) {
    }

    public record IntentRejectStats(
            int totalRejects,
            int blockActionRejects,
            int blockInteractRejects,
            int storageOpenRejects,
            int storageTransferRejects,
            int craftRejects,
            int cookRejects,
            int projectileShootRejects,
            int sleepRejects,
            int entityInteractRejects,
            int chatRejects
    ) {
    }

    private static final class IntentRateWindow {
        private final double startedAtSeconds;
        private int count;

        private IntentRateWindow(double startedAtSeconds, int count) {
            this.startedAtSeconds = startedAtSeconds;
            this.count = count;
        }
    }

    private record PendingCook(int stationX, int stationY, int stationZ, CraftingRecipe recipe, double completeAtSeconds) {
    }

    private record LoadedPosition(double x, double y, double z) {
    }

    private record OpenStorage(int x, int y, int z) {
        boolean matches(int otherX, int otherY, int otherZ) {
            return x == otherX && y == otherY && z == otherZ;
        }
    }
}
