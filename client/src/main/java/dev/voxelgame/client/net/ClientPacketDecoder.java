package dev.voxelgame.client.net;

import dev.voxelgame.common.net.PacketCodec;
import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class ClientPacketDecoder extends ByteToMessageDecoder {
    private final ClientNetworkStats stats;

    public ClientPacketDecoder() {
        this(null);
    }

    public ClientPacketDecoder(ClientNetworkStats stats) {
        this.stats = stats;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < PacketLimits.LENGTH_PREFIX_BYTES) {
            return;
        }

        in.markReaderIndex();
        int packetLength = in.readInt();
        if (packetLength < 0) {
            recordInvalidPacket();
            throw new IllegalArgumentException("Negative packet length: " + packetLength);
        }
        if (packetLength < PacketLimits.MIN_PACKET_SIZE) {
            recordInvalidPacket();
            throw new IllegalArgumentException("Packet length below minimum: " + packetLength);
        }
        if (packetLength > PacketLimits.MAX_PACKET_SIZE) {
            recordInvalidPacket();
            throw new IllegalArgumentException("Packet length exceeds limit: " + packetLength);
        }
        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[packetLength];
        in.readBytes(bytes);
        try {
            out.add(PacketCodec.decode(bytes));
            if (stats != null) {
                stats.recordReceivedBytes(packetLength);
            }
        } catch (RuntimeException e) {
            recordInvalidPacket();
            throw e;
        }
    }

    private void recordInvalidPacket() {
        if (stats != null) {
            stats.recordInvalidPacket();
        }
    }
}
