package dev.voxelgame.client.net;

import dev.voxelgame.client.Hotbar;
import dev.voxelgame.client.ChatLog;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.client.world.ClientWorld;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public final class GameClientConnection implements AutoCloseable {
    private final String host;
    private final int port;
    private final String username;
    private final ClientWorld world;
    private final Hotbar hotbar;
    private final ChatLog chatLog;
    private final ClientNetworkStats stats = new ClientNetworkStats();
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private Channel channel;

    public GameClientConnection(String host, int port, String username, ClientWorld world, Hotbar hotbar, ChatLog chatLog) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.world = world;
        this.hotbar = hotbar;
        this.chatLog = chatLog;
    }

    public void connect() {
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(2 * 1024 * 1024, 0, 4, 0, 4))
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new ClientPacketDecoder())
                                    .addLast(new ClientPacketEncoder())
                                    .addLast(new ClientConnectionHandler(username, world, hotbar, chatLog, stats));
                        }
                    });
            channel = bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while connecting to server", e);
        }
    }

    public void send(GamePacket packet) {
        if (channel != null && channel.isActive()) {
            stats.recordSent(packet);
            channel.writeAndFlush(packet);
        }
    }

    public ClientNetworkStats.Snapshot stats() {
        return stats.snapshot();
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }
}
