package dev.voxelgame.server.net;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.item.StarterInventory;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.auth.AuthProvider;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ServerConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private static final int STREAM_RADIUS_CHUNKS = 4;
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
    private final Inventory inventory = new Inventory(36);
    private final Set<ChunkPos> sentChunks = new HashSet<>();
    private boolean loggedIn;
    private UUID playerId;
    private double playerX = 8.5;
    private double playerY = 120.0;
    private double playerZ = 8.5;
    private double nextBlockActionTime;

    public ServerConnectionHandler(ServerWorld world, AuthProvider authProvider, ServerEntityTracker entityTracker) {
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = entityTracker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        CHANNELS.add(ctx.channel());
        ctx.writeAndFlush(new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-server"));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        CHANNELS.remove(ctx.channel());
        if (playerId != null) {
            entityTracker.removePlayer(playerId);
            broadcastEntitySnapshots();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        switch (packet) {
            case GamePacket.Handshake handshake -> handleHandshake(ctx, handshake);
            case GamePacket.LoginRequest login -> handleLogin(ctx, login);
            case GamePacket.BlockAction action -> handleBlockAction(ctx, action);
            case GamePacket.BlockInteract interact -> handleBlockInteract(ctx, interact);
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

    private void handleCraftRequest(ChannelHandlerContext ctx, GamePacket.CraftRequest craft) {
        if (!loggedIn) {
            ctx.close();
            return;
        }
        recipes.stream()
                .filter(recipe -> recipe.key().equals(craft.recipeKey()))
                .findFirst()
                .ifPresent(recipe -> recipe.craft(inventory, items));
        sendInventory(ctx);
    }

    private Optional<Short> itemIdForKey(String itemKey) {
        return items.findByKey(itemKey).map(ItemType::id);
    }

    private void sendInventory(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new GamePacket.InventorySnapshot(inventory.slots()));
    }

    private void handlePlayerMove(ChannelHandlerContext ctx, GamePacket.PlayerMove move) {
        if (!Double.isFinite(move.x()) || !Double.isFinite(move.y()) || !Double.isFinite(move.z())) {
            ctx.close();
            return;
        }
        playerX = move.x();
        playerY = move.y();
        playerZ = move.z();
        entityTracker.updatePlayer(playerId, move.x(), move.y(), move.z(), move.yaw(), move.pitch());
        streamChunksAround(ctx, ChunkPos.fromBlock((int) Math.floor(move.x()), (int) Math.floor(move.z())));
        CHANNELS.writeAndFlush(new GamePacket.EntitySnapshots(entityTracker.snapshots()));
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
}
