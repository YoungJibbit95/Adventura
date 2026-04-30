package dev.voxelgame.server.net;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.CampfireRules;
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
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.auth.AuthProvider;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.player.ServerPlayerSurvivalState;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private static final int STREAM_RADIUS_CHUNKS = 4;
    private static final double ENTITY_INTERACT_RANGE = 6.0;
    private static final double INITIAL_MOVE_SYNC_RADIUS = 128.0;
    private static final PlayerPhysicsConfig PLAYER_PHYSICS = PlayerPhysicsConfig.defaults();
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Set<ServerConnectionHandler> ACTIVE_HANDLERS = ConcurrentHashMap.newKeySet();

    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
    private final Inventory inventory = new Inventory(36);
    private final ServerPlayerSurvivalState survivalState = new ServerPlayerSurvivalState();
    private final Set<ChunkPos> sentChunks = new HashSet<>();
    private volatile ChannelHandlerContext context;
    private PendingCook pendingCook;
    private volatile boolean sleepReady;
    private volatile boolean loggedIn;
    private UUID playerId;
    private double playerX = 8.5;
    private double playerY = 120.0;
    private double playerZ = 8.5;
    private double nextBlockActionTime;
    private double nextEntityInteractTime;
    private double nextStatsSyncTime;
    private double lastAcceptedMoveTime = Double.NaN;
    private double lastSurvivalUpdateTime = Double.NaN;
    private long survivalTick;
    private int lastSyncedComfort = -1;
    private int lastStorageTransactionId;

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker) {
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = entityTracker;
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
            entityTracker.removePlayer(playerId);
            broadcastEntitySnapshots();
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
                    CHANNELS.writeAndFlush(chat);
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
        StarterInventory.apply(inventory, items);
        entityTracker.registerPlayer(playerId);
        ctx.writeAndFlush(new GamePacket.LoginAccepted(playerId, world.seed(), world.dimension().minY(), world.dimension().maxYExclusive()));
        sendInventory(ctx);
        updateSurvival(System.nanoTime() / 1_000_000_000.0, false);
        sendPlayerStats(ctx, 0.0, true);
        streamChunksAround(ctx, new ChunkPos(0, 0));
        broadcastEntitySnapshots();
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
            CHANNELS.writeAndFlush(update);
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
        if (PlayerBounds.DEFAULT.intersectsBlock(playerX, playerY, playerZ, action.placeX(), action.placeY(), action.placeZ())) {
            sendInventory(ctx);
            return;
        }

        world.applyBlockAction(action).ifPresentOrElse(update -> {
            inventory.removeFromSlot(action.selectedSlot(), 1);
            nextBlockActionTime = now + 0.12;
            CHANNELS.writeAndFlush(update);
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

    private void handleBlockInteract(ChannelHandlerContext ctx, GamePacket.BlockInteract interact) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        if (!InteractionRules.isHotbarSlot(interact.selectedSlot(), inventory.size())) {
            sendInventory(ctx);
            return;
        }
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
        double now = System.nanoTime() / 1_000_000_000.0;
        if (pendingCook != null || now < nextBlockActionTime
                || !InteractionRules.canReachBlock(playerX, playerY, playerZ, cook.stationX(), cook.stationY(), cook.stationZ())) {
            sendInventory(ctx);
            return;
        }
        Optional<CraftingRecipe> recipe = recipes.stream()
                .filter(candidate -> candidate.key().equals(cook.recipeKey()))
                .findFirst();
        if (recipe.isEmpty() || recipe.get().stationType() != CraftingStationType.CAMPFIRE) {
            sendInventory(ctx);
            return;
        }
        world.tickCampfires(now).forEach(CHANNELS::writeAndFlush);
        Optional<BlockType> station = world.blockAt(cook.stationX(), cook.stationY(), cook.stationZ());
        if (station.isEmpty() || !CampfireRules.isActiveCampfire(station.get().id())) {
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
        sendInventory(ctx);
    }

    private void handleStorageTransfer(ChannelHandlerContext ctx, GamePacket.StorageTransfer transfer) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        if (!InteractionRules.canReachBlock(playerX, playerY, playerZ, transfer.x(), transfer.y(), transfer.z())) {
            sendInventory(ctx);
            return;
        }
        if (!isNextStorageTransaction(transfer.transactionId())) {
            sendInventory(ctx);
            return;
        }
        lastStorageTransactionId = transfer.transactionId();
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

    private boolean isNextStorageTransaction(int transactionId) {
        return transactionId > lastStorageTransactionId
                || (lastStorageTransactionId == Integer.MAX_VALUE && transactionId == 1);
    }

    private boolean tryFuelCampfire(ChannelHandlerContext ctx, GamePacket.BlockInteract interact, BlockType targetBlock, double now) {
        if (!CampfireRules.isCampfire(targetBlock.id())) {
            return false;
        }
        ItemStack selected = inventory.slot(interact.selectedSlot());
        if (selected.isEmpty()) {
            sendInventory(ctx);
            return true;
        }
        ItemType selectedItem = items.requireById(selected.itemId());
        OptionalDouble fuelSeconds = CampfireRules.fuelSeconds(selectedItem.key());
        if (fuelSeconds.isEmpty()) {
            sendInventory(ctx);
            return true;
        }
        if (!inventory.removeFromSlot(interact.selectedSlot(), 1)) {
            sendInventory(ctx);
            return true;
        }
        world.fuelCampfire(interact.targetX(), interact.targetY(), interact.targetZ(), now, fuelSeconds.getAsDouble())
                .ifPresent(CHANNELS::writeAndFlush);
        nextBlockActionTime = now + 0.25;
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
        double now = System.nanoTime() / 1_000_000_000.0;
        Optional<EntitySnapshot> target = entityTracker.snapshot(interact.entityId());
        if (target.isEmpty() || now < nextEntityInteractTime || !canReachEntity(target.get())) {
            sendInventory(ctx);
            return;
        }
        if (interact.action() == GamePacket.EntityInteract.Action.FEED && !tryFeedEntity(ctx, interact)) {
            return;
        }
        nextEntityInteractTime = now + 0.35;
        CHANNELS.writeAndFlush(new GamePacket.EntitySnapshots(entityTracker.snapshots()));
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

    private boolean canReachEntity(EntitySnapshot snapshot) {
        double dx = snapshot.x() - playerX;
        double dy = snapshot.y() - playerY;
        double dz = snapshot.z() - playerZ;
        return dx * dx + dy * dy + dz * dz <= ENTITY_INTERACT_RANGE * ENTITY_INTERACT_RANGE;
    }

    private void handleCraftRequest(ChannelHandlerContext ctx, GamePacket.CraftRequest craft) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        Optional<CraftingRecipe> recipe = recipes.stream()
                .filter(recipe -> recipe.key().equals(craft.recipeKey()))
                .findFirst();
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
            world.tickCampfires(now).forEach(CHANNELS::writeAndFlush);
        }
        Optional<BlockType> station = world.blockAt(craft.stationX(), craft.stationY(), craft.stationZ());
        if (recipe.stationType() == CraftingStationType.CAMPFIRE
                && station.isPresent()
                && CampfireRules.isActiveCampfire(station.get().id())) {
            return Optional.of(CraftingStationType.CAMPFIRE);
        }
        return Optional.empty();
    }

    public static void broadcast(GamePacket packet) {
        CHANNELS.writeAndFlush(packet);
    }

    public static void tickCookingJobs(double nowSeconds) {
        for (ServerConnectionHandler handler : ACTIVE_HANDLERS) {
            handler.tickCooking(nowSeconds);
        }
    }

    void tickCooking(double nowSeconds) {
        if (pendingCook == null || context == null || !loggedIn || nowSeconds < pendingCook.completeAtSeconds()) {
            return;
        }
        world.tickCampfires(nowSeconds).forEach(CHANNELS::writeAndFlush);
        Optional<BlockType> station = world.blockAt(pendingCook.stationX(), pendingCook.stationY(), pendingCook.stationZ());
        if (station.isEmpty() || !CampfireRules.isActiveCampfire(station.get().id())) {
            return;
        }
        if (inventory.addStack(pendingCook.recipe().result(), items) != 0) {
            return;
        }
        pendingCook = null;
        sendInventory(context);
    }

    private Optional<Short> itemIdForKey(String itemKey) {
        return items.findByKey(itemKey).map(ItemType::id);
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

    private void handlePlayerMove(ChannelHandlerContext ctx, GamePacket.PlayerMove move) {
        if (!PlayerMovementRules.isFinite(move.x(), move.y(), move.z(), move.yaw(), move.pitch())) {
            ctx.close();
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (!acceptPlayerMove(move, now)) {
            sendPlayerStats(ctx, now, false);
            return;
        }
        boolean moving = movedEnough(move.x(), move.y(), move.z());
        if (moving) {
            sleepReady = false;
        }
        playerX = move.x();
        playerY = move.y();
        playerZ = move.z();
        lastAcceptedMoveTime = now;
        updateSurvival(now, moving);
        entityTracker.updatePlayer(playerId, move.x(), move.y(), move.z(), move.yaw(), move.pitch());
        streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(move.x()), (int) Math.floor(move.z())));
        sendPlayerStats(ctx, now, false);
        CHANNELS.writeAndFlush(new GamePacket.EntitySnapshots(entityTracker.snapshots()));
    }

    private boolean acceptPlayerMove(GamePacket.PlayerMove move, double now) {
        if (!PlayerMovementRules.withinVerticalBounds(
                move.y(),
                PLAYER_PHYSICS,
                world.dimension().minY(),
                world.dimension().maxYExclusive()
        )) {
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
        return PlayerMovementRules.isPlausibleDelta(
                playerX,
                playerY,
                playerZ,
                move.x(),
                move.y(),
                move.z(),
                now - lastAcceptedMoveTime,
                PLAYER_PHYSICS
        );
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
        if (!Double.isFinite(lastSurvivalUpdateTime)) {
            survivalState.updateComfort(world.comfortAt(playerX, playerY, playerZ), tick);
            lastSurvivalUpdateTime = now;
            return;
        }
        if (survivalState.shouldScanComfort(tick)) {
            survivalState.tick(now - lastSurvivalUpdateTime, world.comfortAt(playerX, playerY, playerZ), tick, moving);
        } else {
            survivalState.tick(now - lastSurvivalUpdateTime, moving);
        }
        lastSurvivalUpdateTime = now;
    }

    private boolean movedEnough(double x, double y, double z) {
        double dx = x - playerX;
        double dy = y - playerY;
        double dz = z - playerZ;
        return dx * dx + dy * dy + dz * dz > 0.0001;
    }

    private void broadcastEntitySnapshots() {
        CHANNELS.writeAndFlush(new GamePacket.EntitySnapshots(entityTracker.snapshots()));
    }

    private void streamChunksAround(ChannelHandlerContext ctx, ChunkPos center) {
        for (int z = center.z() - STREAM_RADIUS_CHUNKS; z <= center.z() + STREAM_RADIUS_CHUNKS; z++) {
            for (int x = center.x() - STREAM_RADIUS_CHUNKS; x <= center.x() + STREAM_RADIUS_CHUNKS; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (sentChunks.add(pos)) {
                    ctx.writeAndFlush(world.packetFor(pos));
                }
            }
        }
    }

    private record PendingCook(int stationX, int stationY, int stationZ, CraftingRecipe recipe, double completeAtSeconds) {
    }
}
