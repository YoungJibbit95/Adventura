package dev.voxelgame.client.net;

import dev.voxelgame.client.Hotbar;
import dev.voxelgame.client.ChatLog;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.net.GamePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class ClientConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private final String username;
    private final ClientWorld world;
    private final Hotbar hotbar;
    private final ChatLog chatLog;
    private final ClientNetworkStats stats;

    public ClientConnectionHandler(String username, ClientWorld world, Hotbar hotbar, ChatLog chatLog) {
        this(username, world, hotbar, chatLog, new ClientNetworkStats());
    }

    public ClientConnectionHandler(String username, ClientWorld world, Hotbar hotbar, ChatLog chatLog, ClientNetworkStats stats) {
        this.username = username;
        this.world = world;
        this.hotbar = hotbar;
        this.chatLog = chatLog;
        this.stats = stats;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        GamePacket.Handshake handshake = new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-client");
        GamePacket.LoginRequest login = new GamePacket.LoginRequest(username, "dev-token");
        stats.recordSent(handshake);
        stats.recordSent(login);
        ctx.writeAndFlush(handshake);
        ctx.writeAndFlush(login);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        stats.recordReceived(packet);
        switch (packet) {
            case GamePacket.Handshake handshake -> System.out.println("Server handshake: " + handshake.clientName());
            case GamePacket.LoginAccepted accepted -> {
                world.setOwnPlayerId(accepted.playerId());
                System.out.println("Login accepted as " + accepted.playerId());
            }
            case GamePacket.LoginRejected rejected -> {
                System.out.println("Login rejected: " + rejected.reason());
                ctx.close();
            }
            case GamePacket.ChunkData chunk -> {
                world.applyChunk(chunk);
                System.out.println("Received chunk " + chunk.pos());
            }
            case GamePacket.BlockUpdate update -> {
                world.applyBlock(update);
                System.out.println("Block update " + update.x() + "," + update.y() + "," + update.z());
            }
            case GamePacket.Chat chat -> {
                chatLog.add(chat.message());
                System.out.println("<server> " + chat.message());
            }
            case GamePacket.EntitySnapshots snapshots -> world.applyEntitySnapshots(snapshots.snapshots());
            case GamePacket.InventorySnapshot inventory -> hotbar.applySnapshot(inventory.slots());
            case GamePacket.StorageOpen storage -> hotbar.applyStorageSnapshot(storage.x(), storage.y(), storage.z(), storage.slots());
            default -> {
            }
        }
    }
}
