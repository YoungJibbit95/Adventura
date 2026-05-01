package dev.voxelgame.server;

import dev.voxelgame.server.auth.WhitelistAuthProvider;
import dev.voxelgame.server.net.GameServer;
import dev.voxelgame.server.save.WorldSaveStore;
import dev.voxelgame.server.world.ServerWorld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public final class GameServerMain {
    private static final int DEFAULT_AUTOSAVE_SECONDS = 60;

    private GameServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = intArg(args, "--port", 25565);
        long seed = longArg(args, "--seed", 1337L);
        Set<String> whitelist = stringSetArg(args, "--whitelist");
        Path savePath = pathArg(args, "--save");
        Path playerSaveDirectory = playerSaveDirectory(args, savePath);
        long autosaveIntervalTicks = autosaveIntervalTicks(args);

        ServerWorld world = loadWorld(seed, savePath);
        GameServer server = new GameServer(port, world, new WhitelistAuthProvider(whitelist), playerSaveDirectory);
        server.start();

        TickLoop tickLoop = new TickLoop(tick -> {
            double now = System.nanoTime() / 1_000_000_000.0;
            world.tickTime(1L);
            world.tickCampfires(now).forEach(server::broadcast);
            server.tickCooking(now);
            if (tick % 10 == 0) {
                server.tickEntities(tick);
            }
            if (tick % TickLoop.TPS == 0) {
                server.broadcastServerStats();
                System.out.println("Server tick " + tick);
            }
            if (tick > 0 && autosaveIntervalTicks > 0 && tick % autosaveIntervalTicks == 0) {
                saveWorldQuietly(savePath, world);
            }
        });
        tickLoop.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tickLoop.close();
            saveWorldQuietly(savePath, world);
            server.close();
        }, "shutdown"));

        new CountDownLatch(1).await();
    }

    private static int intArg(String[] args, String name, int fallback) {
        return (int) longArg(args, name, fallback);
    }

    private static long longArg(String[] args, String name, long fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return Long.parseLong(args[i + 1]);
            }
        }
        return fallback;
    }

    private static Set<String> stringSetArg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                if (args[i + 1].isBlank()) {
                    return Set.of();
                }
                return Set.copyOf(Arrays.asList(args[i + 1].split(",")));
            }
        }
        return Set.of();
    }

    private static Path pathArg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i]) && !args[i + 1].isBlank()) {
                return Path.of(args[i + 1]);
            }
        }
        return null;
    }

    private static ServerWorld loadWorld(long seed, Path savePath) throws IOException {
        if (savePath == null || !Files.exists(savePath)) {
            return new ServerWorld(seed);
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        return WorldSaveStore.loadWorld(savePath, now);
    }

    private static Path playerSaveDirectory(String[] args, Path savePath) {
        Path explicit = pathArg(args, "--player-saves");
        if (explicit != null || savePath == null) {
            return explicit;
        }
        Path parent = savePath.getParent();
        return (parent == null ? Path.of("players") : parent.resolve("players"));
    }

    static long autosaveIntervalTicks(String[] args) {
        int seconds = intArg(args, "--autosave-seconds", DEFAULT_AUTOSAVE_SECONDS);
        if (seconds <= 0) {
            return 0L;
        }
        return (long) seconds * TickLoop.TPS;
    }

    private static void saveWorldQuietly(Path savePath, ServerWorld world) {
        if (savePath == null) {
            return;
        }
        try {
            double now = System.nanoTime() / 1_000_000_000.0;
            WorldSaveStore.saveWorld(savePath, world, now);
        } catch (IOException exception) {
            System.err.println("Failed to save Adventura world: " + exception.getMessage());
        }
    }
}
