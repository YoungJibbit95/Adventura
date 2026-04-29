package dev.voxelgame.client;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        new GameClient(ConnectionOptions.fromArgs(args)).run();
    }
}
