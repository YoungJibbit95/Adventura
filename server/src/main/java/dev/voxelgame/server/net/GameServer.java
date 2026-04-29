package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.server.auth.AuthProvider;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public final class GameServer implements AutoCloseable {
    private final int port;
    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public GameServer(int port, ServerWorld world, AuthProvider authProvider) {
        this.port = port;
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = new ServerEntityTracker(world.seed());
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(2 * 1024 * 1024, 0, 4, 0, 4))
                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new NettyPacketDecoder())
                                .addLast(new NettyPacketEncoder())
                                .addLast(new ServerConnectionHandler(world, authProvider, entityTracker));
                    }
                });

        channel = bootstrap.bind(port).sync().channel();
        System.out.println("Adventura server listening on port " + port + " with protocol " + GamePacket.PROTOCOL_VERSION);
    }

    public void broadcast(GamePacket packet) {
        ServerConnectionHandler.broadcast(packet);
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
