package dev.voxelgame.client.net;

import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPacketDecoderTest {
    @Test
    void rejectsTooSmallPackets() {
        ClientNetworkStats stats = new ClientNetworkStats();
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder(stats));
        ByteBuf undersizedHeader = Unpooled.buffer(Integer.BYTES);
        undersizedHeader.writeInt(PacketLimits.MIN_PACKET_SIZE - 1);

        try {
            DecoderException thrown = assertThrows(DecoderException.class, () -> channel.writeInbound(undersizedHeader));
            assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
            org.junit.jupiter.api.Assertions.assertEquals(1L, stats.snapshot().invalidPacketsDropped());
        } finally {
            if (undersizedHeader.refCnt() > 0) {
                ReferenceCountUtil.release(undersizedHeader);
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsOversizedPackets() {
        ClientNetworkStats stats = new ClientNetworkStats();
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder(stats));
        ByteBuf oversizedHeader = Unpooled.buffer(Integer.BYTES);
        oversizedHeader.writeInt(PacketLimits.MAX_PACKET_SIZE + 1);

        try {
            DecoderException thrown = assertThrows(DecoderException.class, () -> channel.writeInbound(oversizedHeader));
            assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
            org.junit.jupiter.api.Assertions.assertEquals(1L, stats.snapshot().invalidPacketsDropped());
        } finally {
            if (oversizedHeader.refCnt() > 0) {
                ReferenceCountUtil.release(oversizedHeader);
            }
            channel.finishAndReleaseAll();
        }
    }
}
