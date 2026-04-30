package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class NettyPacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < PacketLimits.LENGTH_PREFIX_BYTES) {
            return;
        }

        in.markReaderIndex();
        int packetLength = in.readInt();
        if (packetLength < 0) {
            throw new IllegalArgumentException("Negative packet length: " + packetLength);
        }
        if (packetLength < PacketLimits.MIN_PACKET_SIZE) {
            throw new IllegalArgumentException("Packet length below minimum: " + packetLength);
        }
        if (packetLength > PacketLimits.MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("Packet length exceeds limit: " + packetLength);
        }
        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[packetLength];
        in.readBytes(bytes);
        out.add(PacketCodec.decode(bytes));
    }
}
