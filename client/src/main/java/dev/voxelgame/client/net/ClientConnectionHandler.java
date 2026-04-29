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

    public ClientConnectionHandler(String username, ClientWorld world, Hotbar hotbar, ChatLog chatLog) {
        this.username = username;
        this.world = world;
        this.hotbar = hotbar;
        this.chatLog = chatLog;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-client"));
        ctx.writeAndFlush(new GamePacket.LoginRequest(username, "dev-token"));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
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
            default -> {
            }
        }
    }
}
