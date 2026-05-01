package dev.voxelgame.client.net;

import dev.voxelgame.client.Hotbar;
import dev.voxelgame.client.ChatLog;
import dev.voxelgame.client.PlayerStats;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.net.GamePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.function.Consumer;

public final class ClientConnectionHandler extends SimpleChannelInboundHandler<GamePacket> {
    private final String username;
    private final ClientWorld world;
    private final Hotbar hotbar;
    private final PlayerStats playerStats;
    private final ChatLog chatLog;
    private final ClientNetworkStats stats;
    private final Consumer<GamePacket.PlayerPositionSnapshot> playerPositionHandler;
    private final Consumer<GamePacket.ProjectileImpact> projectileImpactHandler;
    private final Consumer<GamePacket.GameplayEvents> gameplayEventsHandler;

    public ClientConnectionHandler(String username, ClientWorld world, Hotbar hotbar, ChatLog chatLog) {
        this(username, world, hotbar, new PlayerStats(), chatLog, new ClientNetworkStats());
    }

    public ClientConnectionHandler(String username, ClientWorld world, Hotbar hotbar, PlayerStats playerStats, ChatLog chatLog, ClientNetworkStats stats) {
        this(username, world, hotbar, playerStats, chatLog, stats, snapshot -> {
        });
    }

    public ClientConnectionHandler(
            String username,
            ClientWorld world,
            Hotbar hotbar,
            PlayerStats playerStats,
            ChatLog chatLog,
            ClientNetworkStats stats,
            Consumer<GamePacket.PlayerPositionSnapshot> playerPositionHandler
    ) {
        this(username, world, hotbar, playerStats, chatLog, stats, playerPositionHandler, impact -> {
        });
    }

    public ClientConnectionHandler(
            String username,
            ClientWorld world,
            Hotbar hotbar,
            PlayerStats playerStats,
            ChatLog chatLog,
            ClientNetworkStats stats,
            Consumer<GamePacket.PlayerPositionSnapshot> playerPositionHandler,
            Consumer<GamePacket.ProjectileImpact> projectileImpactHandler
    ) {
        this(username, world, hotbar, playerStats, chatLog, stats, playerPositionHandler, projectileImpactHandler, events -> {
        });
    }

    public ClientConnectionHandler(
            String username,
            ClientWorld world,
            Hotbar hotbar,
            PlayerStats playerStats,
            ChatLog chatLog,
            ClientNetworkStats stats,
            Consumer<GamePacket.PlayerPositionSnapshot> playerPositionHandler,
            Consumer<GamePacket.ProjectileImpact> projectileImpactHandler,
            Consumer<GamePacket.GameplayEvents> gameplayEventsHandler
    ) {
        this.username = username;
        this.world = world;
        this.hotbar = hotbar;
        this.playerStats = playerStats;
        this.chatLog = chatLog;
        this.stats = stats;
        this.playerPositionHandler = playerPositionHandler == null ? snapshot -> {
        } : playerPositionHandler;
        this.projectileImpactHandler = projectileImpactHandler == null ? impact -> {
        } : projectileImpactHandler;
        this.gameplayEventsHandler = gameplayEventsHandler == null ? events -> {
        } : gameplayEventsHandler;
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
            case GamePacket.PlayerPositionSnapshot snapshot -> playerPositionHandler.accept(snapshot);
            case GamePacket.ProjectileImpact impact -> projectileImpactHandler.accept(impact);
            case GamePacket.GameplayEvents events -> gameplayEventsHandler.accept(events);
            case GamePacket.InventorySnapshot inventory -> hotbar.applySnapshot(inventory.slots());
            case GamePacket.PlayerStatsSnapshot snapshot -> playerStats.applySnapshot(
                    snapshot.health(),
                    snapshot.hunger(),
                    snapshot.stamina(),
                    snapshot.breath(),
                    snapshot.armor(),
                    snapshot.comfort()
            );
            case GamePacket.CampfireStatus status -> world.applyCampfireStatus(status);
            case GamePacket.StorageOpen storage -> hotbar.applyStorageSnapshot(storage.x(), storage.y(), storage.z(), storage.slots());
            case GamePacket.StorageClose close -> hotbar.closeStorage(close.x(), close.y(), close.z());
            default -> {
            }
        }
    }
}
