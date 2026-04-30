package dev.voxelgame.client.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPacketDecoderTest {
    @Test
    void rejectsOversizedPackets() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder());
        ByteBuf oversizedHeader = Unpooled.buffer(Integer.BYTES);
        oversizedHeader.writeInt(2 * 1024 * 1024 + 1);

        assertThrows(IllegalArgumentException.class, () -> channel.writeInbound(oversizedHeader));
        channel.finishAndReleaseAll();
    }
}
