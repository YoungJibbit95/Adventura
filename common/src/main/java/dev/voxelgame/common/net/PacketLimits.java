package dev.voxelgame.common.net;

public final class PacketLimits {
    public static final int LENGTH_PREFIX_BYTES = Integer.BYTES;
    public static final int MIN_PACKET_SIZE = Integer.BYTES;
    public static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;

    private PacketLimits() {
    }
}
