package dev.voxelgame.client.net;

import dev.voxelgame.common.net.PacketCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class ClientPacketDecoder extends ByteToMessageDecoder {
    private static final int MIN_PACKET_SIZE = Integer.BYTES;
    private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < Integer.BYTES) {
            return;
        }

        in.markReaderIndex();
        int packetLength = in.readInt();
        if (packetLength < 0) {
            throw new IllegalArgumentException("Negative packet length: " + packetLength);
        }
        if (packetLength > PacketCodec.MAX_PACKET_SIZE) {
        if (packetLength < MIN_PACKET_SIZE) {
            throw new IllegalArgumentException("Packet length below minimum header size: " + packetLength);
        }
        if (packetLength > MAX_PACKET_SIZE) {
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
