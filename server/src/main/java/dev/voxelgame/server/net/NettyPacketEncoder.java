package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class NettyPacketEncoder extends MessageToByteEncoder<GamePacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, GamePacket msg, ByteBuf out) {
        byte[] bytes = PacketCodec.encode(msg);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
