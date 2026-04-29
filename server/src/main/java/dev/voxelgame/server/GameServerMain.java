package dev.voxelgame.server;

import dev.voxelgame.server.auth.WhitelistAuthProvider;
import dev.voxelgame.server.net.GameServer;
import dev.voxelgame.server.world.ServerWorld;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public final class GameServerMain {
    private GameServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = intArg(args, "--port", 25565);
        long seed = longArg(args, "--seed", 1337L);
        Set<String> whitelist = stringSetArg(args, "--whitelist");

        ServerWorld world = new ServerWorld(seed);
        GameServer server = new GameServer(port, world, new WhitelistAuthProvider(whitelist));
        server.start();

        TickLoop tickLoop = new TickLoop(tick -> {
            double now = System.nanoTime() / 1_000_000_000.0;
            world.tickCampfires(now).forEach(server::broadcast);
            if (tick % TickLoop.TPS == 0) {
                System.out.println("Server tick " + tick);
            }
        });
        tickLoop.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tickLoop.close();
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
}
