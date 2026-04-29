package dev.voxelgame.server.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.Inventory;
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
        switch (action.action()) {
            case BREAK -> handleBreakAction(ctx, action);
            case PLACE -> handlePlaceAction(ctx, action);
        }
    }

    private void handleBreakAction(ChannelHandlerContext ctx, GamePacket.BlockAction action) {
        Optional<String> dropKey = world.dropFor(action.targetX(), action.targetY(), action.targetZ());
        Optional<Short> dropItemId = dropKey.flatMap(this::itemIdForKey);
        if (dropKey.isPresent() && dropItemId.isEmpty()) {
            sendInventory(ctx);
            return;
        }
        if (dropItemId.isPresent() && !inventory.canAdd(dropItemId.get(), 1, items)) {
            sendInventory(ctx);
            return;
        }

        world.applyBlockAction(action).ifPresent(update -> {
            dropItemId.ifPresent(itemId -> inventory.add(itemId, 1, items));
            CHANNELS.writeAndFlush(update);
            sendInventory(ctx);
        });
    }

    private void handlePlaceAction(ChannelHandlerContext ctx, GamePacket.BlockAction action) {
        Optional<ItemType> placeItem = itemForPlacedBlock(action.blockId());
        if (placeItem.isEmpty() || !inventory.has(placeItem.get().id(), 1)) {
            sendInventory(ctx);
            return;
        }

        world.applyBlockAction(action).ifPresent(update -> {
            inventory.remove(placeItem.get().id(), 1);
            CHANNELS.writeAndFlush(update);
            sendInventory(ctx);
        });
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

    private Optional<ItemType> itemForPlacedBlock(short blockId) {
        Optional<String> blockKey = world.blockKey(blockId);
        if (blockKey.isEmpty()) {
            return Optional.empty();
        }
        for (ItemType item : items.values()) {
            if (blockKey.get().equals(item.placesBlockKey())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private void sendInventory(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new GamePacket.InventorySnapshot(inventory.slots()));
    }

    private void handlePlayerMove(ChannelHandlerContext ctx, GamePacket.PlayerMove move) {
        if (!Double.isFinite(move.x()) || !Double.isFinite(move.y()) || !Double.isFinite(move.z())) {
            ctx.close();
            return;
        }
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
