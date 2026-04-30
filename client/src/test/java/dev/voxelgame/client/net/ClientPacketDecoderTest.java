package dev.voxelgame.client.net;

import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPacketDecoderTest {
    @Test
    void rejectsTooSmallPackets() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder());
        ByteBuf undersizedHeader = Unpooled.buffer(Integer.BYTES);
        undersizedHeader.writeInt(PacketLimits.LENGTH_PREFIX_BYTES - 1);

        assertThrows(IllegalArgumentException.class, () -> channel.writeInbound(undersizedHeader));
        channel.finishAndReleaseAll();
    }

    @Test
    void rejectsOversizedPackets() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder());
        ByteBuf oversizedHeader = Unpooled.buffer(Integer.BYTES);
        oversizedHeader.writeInt(PacketLimits.MAX_PACKET_SIZE + 1);

        assertThrows(IllegalArgumentException.class, () -> channel.writeInbound(oversizedHeader));
        channel.finishAndReleaseAll();
    }
}
