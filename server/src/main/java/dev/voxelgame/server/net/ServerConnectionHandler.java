package dev.voxelgame.server.net;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.CraftingStationRules;
import dev.voxelgame.common.gameplay.EntityDrops;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.item.StarterInventory;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerMovementRules;
import dev.voxelgame.common.physics.PlayerPhysicsConfig;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.auth.AuthProvider;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.DroppedItemEntity;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.player.ServerPlayerSurvivalState;
import dev.voxelgame.server.save.PlayerSave;
import dev.voxelgame.server.save.PlayerSaveStore;
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
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private static final int STREAM_RADIUS_CHUNKS = 4;
    private static final double ENTITY_INTERACT_RANGE = 6.0;
    private static final double ENTITY_SNAPSHOT_RADIUS = 96.0;
    private static final double INITIAL_MOVE_SYNC_RADIUS = 128.0;
    private static final double MOVE_RATE_WINDOW_SECONDS = 1.0;
    private static final int MAX_MOVES_PER_RATE_WINDOW = 30;
    private static final PlayerPhysicsConfig PLAYER_PHYSICS = PlayerPhysicsConfig.defaults();
    private static final PlayerMovementRules.MovementMode PLAYER_MOVEMENT_MODE = PlayerMovementRules.MovementMode.SURVIVAL;
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Set<ServerConnectionHandler> ACTIVE_HANDLERS = ConcurrentHashMap.newKeySet();
    private static final Set<ServerWorld> DIRTY_ENTITY_SNAPSHOT_WORLDS = ConcurrentHashMap.newKeySet();

    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private final int streamRadiusChunks;
    private final Path playerSaveDirectory;
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
    private final Inventory inventory = new Inventory(36);
    private final ServerPlayerSurvivalState survivalState = new ServerPlayerSurvivalState();
    private final Set<ChunkPos> sentChunks = ConcurrentHashMap.newKeySet();
    private volatile ChannelHandlerContext context;
    private PendingCook pendingCook;
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
    private String lastWorldKey = "overworld";
    private double nextBlockActionTime;
    private double nextEntityInteractTime;
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
    private double fallStartY = Double.NaN;
    private boolean fallTouchedWater;
    private double lastSurvivalUpdateTime = Double.NaN;
    private long survivalTick;
    private int lastSyncedComfort = -1;
    private int lastClientTransactionId;

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, null);
    }

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker, Path playerSaveDirectory) {
        this(world, authProvider, entityTracker, STREAM_RADIUS_CHUNKS, playerSaveDirectory);
    }

    ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker, int streamRadiusChunks) {
        this(world, authProvider, entityTracker, streamRadiusChunks, null);
    }

    ServerConnectionHandler(
            ServerWorld world,
            AuthProvider authProvider,
            ServerEntityTracker entityTracker,
            int streamRadiusChunks,
            Path playerSaveDirectory
    ) {
        if (streamRadiusChunks < 0) {
            throw new IllegalArgumentException("streamRadiusChunks must be >= 0");
        }
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = entityTracker;
        this.streamRadiusChunks = streamRadiusChunks;
        this.playerSaveDirectory = playerSaveDirectory;
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
            case GamePacket.PlayerMove move -> {
                if (!loggedIn) {
                    ctx.close();
                } else {
                    handlePlayerMove(ctx, move);
                }
            }
            case GamePacket.Chat chat -> {
                if (loggedIn) {
                    broadcastToLoggedIn(chat);
                }
            }
            default -> {
            }
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx, GamePacket.Handshake handshake) {
        if (handshake.protocolVersion() != GamePacket.PROTOCOL_VERSION) {
            ctx.writeAndFlush(new GamePacket.LoginRejected("Protocol mismatch")).addListener(future -> ctx.close());
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
        sendAuthoritativePlayerPosition(ctx, 0L);
        streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(playerX), (int) Math.floor(playerZ)));
        sendEntitySnapshots(ctx);
        markEntitySnapshotsDirty(world);
    }

    private void handleBlockAction(ChannelHandlerContext ctx, GamePacket.BlockAction action) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        if (!InteractionRules.isHotbarSlot(action.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = action.selectedSlot();
        double now = System.nanoTime() / 1_000_000_000.0;
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
        if (InteractionRules.placementIntersectsPlayer(playerX, playerY, playerZ, action.placeX(), action.placeY(), action.placeZ())
                || placementIntersectsTrackedEntity(action.placeX(), action.placeY(), action.placeZ())) {
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
        if (!InteractionRules.canReachBlock(playerX, playerY, playerZ, action.targetX(), action.targetY(), action.targetZ())) {
            return false;
        }
        return action.action() == GamePacket.BlockAction.Action.BREAK
                || InteractionRules.canReachBlock(playerX, playerY, playerZ, action.placeX(), action.placeY(), action.placeZ());
    }

    private boolean placementIntersectsTrackedEntity(int blockX, int blockY, int blockZ) {
        for (EntitySnapshot snapshot : entityTracker.snapshots()) {
            if (playerId != null && playerId.equals(snapshot.ownerPlayerId())) {
                continue;
            }
            if (InteractionRules.placementIntersectsEntity(snapshot, blockX, blockY, blockZ)) {
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
        if (!InteractionRules.isHotbarSlot(interact.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = interact.selectedSlot();
        double now = System.nanoTime() / 1_000_000_000.0;
        if (now < nextBlockActionTime || !InteractionRules.canReachBlock(playerX, playerY, playerZ, interact.targetX(), interact.targetY(), interact.targetZ())) {
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
        if (!acceptClientTransaction(open.transactionId())) {
            sendInventory(ctx);
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (now < nextBlockActionTime || !InteractionRules.canReachBlock(playerX, playerY, playerZ, open.x(), open.y(), open.z())) {
            sendInventory(ctx);
            return;
        }
        openStorage(ctx, open.x(), open.y(), open.z(), now);
    }

    private void openStorage(ChannelHandlerContext ctx, int x, int y, int z, double now) {
        world.openStorageCrate(x, y, z).ifPresentOrElse(slots -> {
            ctx.writeAndFlush(new GamePacket.StorageOpen(x, y, z, slots));
            nextBlockActionTime = now + 0.12;
        }, () -> sendInventory(ctx));
    }

    private void handleSleepRequest(ChannelHandlerContext ctx, GamePacket.SleepRequest sleep) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (now < nextBlockActionTime || !InteractionRules.canReachBlock(playerX, playerY, playerZ, sleep.x(), sleep.y(), sleep.z())) {
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
            handler.sendPlayerStats(handler.context, now, true);
        }
    }

    private void handleCookRequest(ChannelHandlerContext ctx, GamePacket.CookRequest cook) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        if (!acceptClientTransaction(cook.transactionId())) {
            sendInventory(ctx);
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (pendingCook != null || now < nextBlockActionTime
                || !InteractionRules.canReachBlock(playerX, playerY, playerZ, cook.stationX(), cook.stationY(), cook.stationZ())) {
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
        if (!acceptClientTransaction(transfer.transactionId())) {
            sendInventory(ctx);
            return;
        }
        if (!InteractionRules.canReachBlock(playerX, playerY, playerZ, transfer.x(), transfer.y(), transfer.z())) {
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
            ctx.writeAndFlush(new GamePacket.StorageOpen(transfer.x(), transfer.y(), transfer.z(), slots));
            sendInventory(ctx);
        }, () -> sendInventory(ctx));
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
        if (!InteractionRules.isHotbarSlot(interact.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
        hotbarSelection = interact.selectedSlot();
        double now = System.nanoTime() / 1_000_000_000.0;
        Optional<EntitySnapshot> target = entityTracker.snapshot(interact.entityId());
        if (target.isEmpty() || now < nextEntityInteractTime || !canReachEntity(target.get())) {
            sendInventory(ctx);
            return;
        }
        switch (interact.action()) {
            case FEED -> {
                if (!tryFeedEntity(ctx, interact)) {
                    return;
                }
            }
            case ATTACK -> {
                if (!tryAttackEntity(ctx, interact, target.get())) {
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

    private boolean tryFeedEntity(ChannelHandlerContext ctx, GamePacket.EntityInteract interact) {
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
        Optional<EntitySnapshot> updated = entityTracker.feedAmbient(interact.entityId(), Math.max(1, selectedItem.foodValue() / 2), playerId);
        if (updated.isEmpty()) {
            sendInventory(ctx);
            return false;
        }
        inventory.removeFromSlot(interact.selectedSlot(), 1);
        return true;
    }

    private boolean tryAttackEntity(ChannelHandlerContext ctx, GamePacket.EntityInteract interact, EntitySnapshot target) {
        Optional<EntitySnapshot> updated = entityTracker.damageAmbient(
                interact.entityId(),
                entityAttackDamage(inventory.slot(interact.selectedSlot())),
                playerId
        );
        if (updated.isEmpty()) {
            sendInventory(ctx);
            return false;
        }
        if (updated.get().health() <= 0) {
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
        craftingStationFor(craft, recipe.get())
                .ifPresent(station -> recipe.get().craft(inventory, items, station, craft.count()));
        sendInventory(ctx);
    }

    private Optional<CraftingStationType> craftingStationFor(GamePacket.CraftRequest craft, CraftingRecipe recipe) {
        if (recipe.stationType() == CraftingStationType.INVENTORY) {
            return Optional.of(CraftingStationType.INVENTORY);
        }
        if (!craft.hasStation() || !InteractionRules.canReachBlock(playerX, playerY, playerZ, craft.stationX(), craft.stationY(), craft.stationZ())) {
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
            ctx.writeAndFlush(new GamePacket.EntitySnapshots(handler.visibleEntitySnapshots(entityTracker)));
        }
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

    private void sendCampfireStatus(ChannelHandlerContext ctx, int x, int y, int z, double nowSeconds) {
        ctx.writeAndFlush(campfireStatusPacket(x, y, z, nowSeconds));
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
        playerX = save.x();
        playerY = save.y();
        playerZ = save.z();
        playerYaw = save.yaw();
        playerPitch = save.pitch();
        hotbarSelection = Math.max(0, Math.min(inventory.size() - 1, save.hotbarSelection()));
        spawnPoint = save.spawnPoint();
        gameMode = save.gameMode();
        discoveredRecipes = List.copyOf(save.discoveredRecipes());
        discoveredBiomes = List.copyOf(save.discoveredBiomes());
        journalEntries = List.copyOf(save.journalEntries());
        lastWorldKey = save.lastWorldKey();
        inventory.clear();
        for (int i = 0; i < Math.min(inventory.size(), save.inventory().size()); i++) {
            inventory.setSlot(i, save.inventory().get(i));
        }
        survivalState.loadPersistentStats(
                save.survival().health(),
                save.survival().hunger(),
                save.survival().stamina(),
                save.survival().breath()
        );
    }

    private void savePlayerQuietly() {
        if (playerSaveDirectory == null || playerId == null) {
            return;
        }
        try {
            PlayerSaveStore.save(playerSaveDirectory, playerSaveSnapshot());
        } catch (IOException exception) {
            System.err.println("Failed to save Adventura player " + playerId + ": " + exception.getMessage());
        }
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
                lastWorldKey
        );
    }

    private void handlePlayerMove(ChannelHandlerContext ctx, GamePacket.PlayerMove move) {
        if (!PlayerMovementRules.isFinite(move.x(), move.y(), move.z(), move.yaw(), move.pitch())) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptPlayerMove(move, now)) {
            sendAuthoritativePlayerPosition(ctx, move.sequence());
            sendPlayerStats(ctx, now, false);
            return;
        }
        boolean moving = movedEnough(move.x(), move.y(), move.z());
        if (moving) {
            sleepReady = false;
        }
        boolean hadAcceptedMove = Double.isFinite(lastAcceptedMoveTime);
        double previousX = playerX;
        double previousY = playerY;
        double previousZ = playerZ;
        double moveDeltaSeconds = hadAcceptedMove ? now - lastAcceptedMoveTime : 0.0;
        boolean hasGroundSupport = hasGroundSupport(move.x(), move.y(), move.z());
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
        boolean fallDamaged = updateFallImpact(previousY, move.y(), hasGroundSupport, waterState);
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

    private boolean acceptPlayerMove(GamePacket.PlayerMove move, double now) {
        if (!acceptMovementRate(now)) {
            return false;
        }
        if (!PlayerMovementRules.withinVerticalBounds(
                move.y(),
                PLAYER_PHYSICS,
                world.dimension().minY(),
                world.dimension().maxYExclusive()
        )) {
            return false;
        }
        if (world.collidesPlayer(move.x(), move.y(), move.z(), PLAYER_PHYSICS.bounds())) {
            return false;
        }
        boolean hasGroundSupport = hasGroundSupport(move.x(), move.y(), move.z());
        if (!PlayerMovementRules.groundedClaimPlausible(move.onGround(), hasGroundSupport)) {
            return false;
        }
        PlayerWaterState waterState = world.playerWaterState(move.x(), move.y(), move.z(), PLAYER_PHYSICS.bounds());
        if (!PlayerMovementRules.waterStateClaimPlausible(move.waterState(), waterState)) {
            return false;
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
            );
        }
        if (move.sequence() > 0L && move.sequence() <= lastAcceptedMoveSequence) {
            return false;
        }
        if (!world.playerPathClear(
                playerX,
                playerY,
                playerZ,
                move.x(),
                move.y(),
                move.z(),
                PLAYER_PHYSICS.bounds(),
                PLAYER_PHYSICS.maxCollisionStep()
        )) {
            return false;
        }
        boolean movementAssist = playerTouchesWater(playerX, playerY, playerZ)
                || PlayerMovementRules.waterMovementAssist(waterState);
        if (!PlayerMovementRules.upwardMovementPlausible(playerY, move.y(), lastAcceptedGrounded, lastAcceptedMoveDeltaY, movementAssist)) {
            return false;
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
                PLAYER_MOVEMENT_MODE
        )) {
            return false;
        }
        return !hasAcceptedMovementSample || PlayerMovementRules.isPlausibleHorizontalAcceleration(
                lastAcceptedMoveDeltaX,
                lastAcceptedMoveDeltaZ,
                lastAcceptedMoveDeltaSeconds,
                move.x() - playerX,
                move.z() - playerZ,
                deltaSeconds,
                PLAYER_PHYSICS,
                PLAYER_MOVEMENT_MODE
        );
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
        sendAuthoritativePlayerPosition(ctx, sequence, world.playerWaterState(playerX, playerY, playerZ, PLAYER_PHYSICS.bounds()));
    }

    private void sendAuthoritativePlayerPosition(ChannelHandlerContext ctx, long sequence, PlayerWaterState waterState) {
        ctx.writeAndFlush(new GamePacket.PlayerPositionSnapshot(
                sequence,
                playerX,
                playerY,
                playerZ,
                playerYaw,
                playerPitch,
                lastAcceptedGrounded,
                waterState
        ));
    }

    private void sendPlayerStats(ChannelHandlerContext ctx, double now, boolean force) {
        int comfort = survivalState.comfort();
        if (!force && now < nextStatsSyncTime && comfort == lastSyncedComfort) {
            return;
        }
        lastSyncedComfort = comfort;
        nextStatsSyncTime = now + 1.0;
        ctx.writeAndFlush(survivalState.snapshot());
    }

    private void updateSurvival(double now, boolean moving) {
        long tick = survivalTick++;
        PlayerWaterState waterState = world.playerWaterState(playerX, playerY, playerZ, PLAYER_PHYSICS.bounds());
        if (!Double.isFinite(lastSurvivalUpdateTime)) {
            survivalState.updateComfort(world.comfortAt(playerX, playerY, playerZ), tick);
            lastSurvivalUpdateTime = now;
            return;
        }
        if (survivalState.shouldScanComfort(tick)) {
            survivalState.tick(now - lastSurvivalUpdateTime, world.comfortAt(playerX, playerY, playerZ), tick, moving, waterState.headUnderwater());
        } else {
            survivalState.tick(now - lastSurvivalUpdateTime, moving, waterState.headUnderwater());
        }
        lastSurvivalUpdateTime = now;
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
        ctx.writeAndFlush(new GamePacket.EntitySnapshots(visibleEntitySnapshots(entityTracker)));
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
        for (int z = center.z() - streamRadiusChunks; z <= center.z() + streamRadiusChunks; z++) {
            for (int x = center.x() - streamRadiusChunks; x <= center.x() + streamRadiusChunks; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (sentChunks.add(pos)) {
                    ctx.writeAndFlush(world.packetFor(pos));
                }
            }
        }
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
            if (packet instanceof GamePacket.BlockUpdate update && !handler.hasSeenChunk(update)) {
                continue;
            }
            ctx.writeAndFlush(packet);
        }
    }

    private boolean hasSeenChunk(GamePacket.BlockUpdate update) {
        return sentChunks.contains(ChunkPos.fromBlock(update.x(), update.z()));
    }

    private void broadcastToLoggedInWorld(GamePacket packet) {
        broadcastToLoggedInWorld(world, packet);
    }

    private record PendingCook(int stationX, int stationY, int stationZ, CraftingRecipe recipe, double completeAtSeconds) {
    }
}
