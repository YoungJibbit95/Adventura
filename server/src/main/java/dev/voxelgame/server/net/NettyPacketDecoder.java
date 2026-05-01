package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class NettyPacketDecoder extends ByteToMessageDecoder {
    private static final AtomicLong REJECTED_FRAMES = new AtomicLong();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < PacketLimits.LENGTH_PREFIX_BYTES) {
            return;
        }

        in.markReaderIndex();
        int packetLength = in.readInt();
        if (packetLength < 0) {
            rejectFrame(ctx, in, "Negative packet length: " + packetLength);
            return;
        }
        if (packetLength < PacketLimits.MIN_PACKET_SIZE) {
            rejectFrame(ctx, in, "Packet length below minimum: " + packetLength);
            return;
        }
        if (packetLength > PacketLimits.MAX_PACKET_SIZE) {
            rejectFrame(ctx, in, "Packet length exceeds limit: " + packetLength);
            return;
        }
        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[packetLength];
        in.readBytes(bytes);
        try {
            out.add(PacketCodec.decode(bytes));
        } catch (RuntimeException exception) {
            rejectFrame(ctx, in, "Packet payload decode failed: " + exception.getMessage());
        }
    }

    public static long rejectedFrames() {
        return REJECTED_FRAMES.get();
    }

    private static void rejectFrame(ChannelHandlerContext ctx, ByteBuf in, String message) {
        REJECTED_FRAMES.incrementAndGet();
        in.clear();
        System.err.println("Rejected invalid Adventura packet frame: " + message);
        ctx.close();
    }
}
