package dev.voxelgame.client;

public record ConnectionOptions(
        boolean enabled,
        String host,
        int port,
        String username,
        long seed,
        int previewRadius,
        int renderDistance,
        boolean autoSingleplayer,
        boolean autoJoin
) {
    public static ConnectionOptions fromArgs(String[] args) {
        String host = null;
        int port = 25565;
        String username = "Player";
        long seed = 1337L;
        int previewRadius = 3;
        int renderDistance = 8;
        boolean autoSingleplayer = false;
        boolean autoJoin = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--connect" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--connect requires a host");
                    }
                    host = args[++i];
                }
                case "--port" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--port requires a number");
                    }
                    port = Integer.parseInt(args[++i]);
                }
                case "--username" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--username requires a value");
                    }
                    username = args[++i];
                }
                case "--seed" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--seed requires a number");
                    }
                    seed = Long.parseLong(args[++i]);
                }
                case "--preview-radius" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--preview-radius requires a number");
                    }
                    previewRadius = Math.max(0, Integer.parseInt(args[++i]));
                }
                case "--render-distance" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--render-distance requires a number");
                    }
                    renderDistance = Math.max(1, Integer.parseInt(args[++i]));
                }
                case "--auto-singleplayer" -> autoSingleplayer = true;
                case "--auto-join" -> autoJoin = true;
                default -> {
                }
            }
        }
        return new ConnectionOptions(host != null, host, port, username, seed, previewRadius, renderDistance, autoSingleplayer, autoJoin);
    }
}
