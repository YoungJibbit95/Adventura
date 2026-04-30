package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPacketCodecTest {
    @Test
    void decoderSupportsPartialFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder());
        try {
            GamePacket.Chat expected = new GamePacket.Chat("hello");
            EmbeddedChannel encoder = new EmbeddedChannel(new ClientPacketEncoder());
            encoder.writeOutbound(expected);
            var encoded = (io.netty.buffer.ByteBuf) encoder.readOutbound();

            int splitAt = 2;
            channel.writeInbound(encoded.retainedSlice(0, splitAt));
            assertNull(channel.readInbound());

            channel.writeInbound(encoded.retainedSlice(splitAt, encoded.readableBytes() - splitAt));
            GamePacket.Chat decoded = channel.readInbound();
            assertEquals(expected.message(), decoded.message());

            encoded.release();
            encoder.finishAndReleaseAll();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decoderSupportsMultipleFramesInSingleBuffer() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketEncoder(), new ClientPacketDecoder());
        try {
            channel.writeOutbound(new GamePacket.Chat("first"));
            channel.writeOutbound(new GamePacket.Chat("second"));
            var firstEncoded = (io.netty.buffer.ByteBuf) channel.readOutbound();
            var secondEncoded = (io.netty.buffer.ByteBuf) channel.readOutbound();
            var combined = Unpooled.wrappedBuffer(firstEncoded, secondEncoded);

            assertTrue(channel.writeInbound(combined));

            GamePacket.Chat firstDecoded = channel.readInbound();
            GamePacket.Chat secondDecoded = channel.readInbound();
            assertEquals("first", firstDecoded.message());
            assertEquals("second", secondDecoded.message());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decoderRejectsOversizedFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientPacketDecoder());
        try {
            int oversizedLength = PacketCodec.MAX_PACKET_SIZE + 1;
            var buffer = Unpooled.buffer(Integer.BYTES);
            buffer.writeInt(oversizedLength);

            assertThrows(IllegalArgumentException.class, () -> channel.writeInbound(buffer));
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
