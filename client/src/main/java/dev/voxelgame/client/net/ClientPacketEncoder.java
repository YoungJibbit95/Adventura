package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class ClientPacketEncoder extends MessageToByteEncoder<GamePacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, GamePacket msg, ByteBuf out) {
        out.writeBytes(PacketCodec.encode(msg));
    }
}
