package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.server.TickLoop;
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

import java.nio.file.Path;
import java.util.List;

public final class GameServer implements AutoCloseable {
    private static final double ENTITY_TICK_SECONDS = 10.0 / TickLoop.TPS;

    private final int port;
    private final ServerWorld world;
    private final AuthProvider authProvider;
    private final ServerEntityTracker entityTracker;
    private final Path playerSaveDirectory;
    private final ServerChunkStreamer chunkStreamer;
    private volatile long lastPhysicsTickNanos;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public GameServer(int port, ServerWorld world, AuthProvider authProvider) {
        this(port, world, authProvider, null);
    }

    public GameServer(int port, ServerWorld world, AuthProvider authProvider, Path playerSaveDirectory) {
        this.port = port;
        this.world = world;
        this.authProvider = authProvider;
        this.entityTracker = new ServerEntityTracker(world.seed(), world::entityPlacementClear);
        this.playerSaveDirectory = playerSaveDirectory;
        this.chunkStreamer = ServerChunkStreamer.createDefault();
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
                                .addLast(new ServerConnectionHandler(world, authProvider, entityTracker, playerSaveDirectory, chunkStreamer));
                    }
                });

        channel = bootstrap.bind(port).sync().channel();
        System.out.println("Adventura server listening on port " + port + " with protocol " + GamePacket.PROTOCOL_VERSION);
    }

    public void broadcast(GamePacket packet) {
        ServerConnectionHandler.broadcast(world, packet);
    }

    public void tickEntities(long tick) {
        long startNanos = System.nanoTime();
        boolean ambientChanged = !entityTracker.tickAmbient(tick, world::canMoveAmbientEntity).isEmpty();
        boolean projectileChanged = !entityTracker.tickProjectiles(
                ENTITY_TICK_SECONDS,
                world::collidesProjectile,
                world::projectileInWater,
                System.nanoTime() / 1_000_000_000.0
        ).isEmpty();
        boolean clientStateChanged = ServerConnectionHandler.consumeEntitySnapshotDirty(world);
        lastPhysicsTickNanos = System.nanoTime() - startNanos;
        if (!ambientChanged && !projectileChanged && !clientStateChanged) {
            return;
        }
        ServerConnectionHandler.broadcastEntitySnapshots(world, entityTracker);
    }

    public ServerEntityTracker.AmbientTickStats entityTickStats() {
        return entityTracker.lastAmbientTickStats();
    }

    public ServerPhysicsStats physicsStats() {
        ServerEntityTracker.AmbientTickStats ambient = entityTracker.lastAmbientTickStats();
        ServerEntityTracker.ProjectileTickStats projectiles = entityTracker.lastProjectileTickStats();
        ServerConnectionHandler.MovementRejectStats rejects = ServerConnectionHandler.movementRejectStats(world);
        return new ServerPhysicsStats(
                rejects.totalRejects(),
                rejects.strikes(),
                ambient.activeAmbient(),
                ambient.parkedAmbient(),
                ambient.blockedAmbientMoves(),
                entityTracker.projectileCount(),
                projectiles.emittedHits(),
                projectiles.blockHits(),
                projectiles.entityHits(),
                projectiles.expired(),
                ambient.durationNanos(),
                projectiles.durationNanos(),
                lastPhysicsTickNanos
        );
    }

    public List<ServerConnectionHandler.InterestStats> interestStats() {
        return ServerConnectionHandler.interestStats(world);
    }

    public void broadcastServerStats() {
        ServerConnectionHandler.broadcastServerStats(world);
    }

    public void tickCooking(double nowSeconds) {
        ServerConnectionHandler.tickCookingJobs(nowSeconds);
    }

    @Override
    public void close() {
        ServerConnectionHandler.saveActivePlayers(world);
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        chunkStreamer.close();
    }

    public record ServerPhysicsStats(
            int movementRejects,
            int movementRejectStrikes,
            int activeAmbientEntities,
            int parkedAmbientEntities,
            int blockedAmbientMoves,
            int projectileCount,
            int projectileSteps,
            int projectileBlockHits,
            int projectileEntityHits,
            int projectileExpired,
            long ambientTickNanos,
            long projectileTickNanos,
            long totalTickNanos
    ) {
        public ServerPhysicsStats {
            movementRejects = Math.max(0, movementRejects);
            movementRejectStrikes = Math.max(0, movementRejectStrikes);
            activeAmbientEntities = Math.max(0, activeAmbientEntities);
            parkedAmbientEntities = Math.max(0, parkedAmbientEntities);
            blockedAmbientMoves = Math.max(0, blockedAmbientMoves);
            projectileCount = Math.max(0, projectileCount);
            projectileSteps = Math.max(0, projectileSteps);
            projectileBlockHits = Math.max(0, projectileBlockHits);
            projectileEntityHits = Math.max(0, projectileEntityHits);
            projectileExpired = Math.max(0, projectileExpired);
            ambientTickNanos = Math.max(0L, ambientTickNanos);
            projectileTickNanos = Math.max(0L, projectileTickNanos);
            totalTickNanos = Math.max(0L, totalTickNanos);
        }
    }
}
