package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketLimits;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyPacketCodecTest {
    @Test
    void decoderSupportsPartialFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyPacketDecoder());
        try {
            GamePacket.Chat expected = new GamePacket.Chat("hello");
            EmbeddedChannel encoder = new EmbeddedChannel(new NettyPacketEncoder());
            encoder.writeOutbound(expected);
            var encoded = (io.netty.buffer.ByteBuf) encoder.readOutbound();

            int splitAt = 3;
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
        EmbeddedChannel channel = new EmbeddedChannel(new NettyPacketEncoder(), new NettyPacketDecoder());
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
        EmbeddedChannel channel = new EmbeddedChannel(new NettyPacketDecoder());
        try {
            int tooLarge = PacketLimits.MAX_PACKET_SIZE + 1;
            var frameHeader = Unpooled.buffer(Integer.BYTES).writeInt(tooLarge);
            try {
                DecoderException thrown = assertThrows(DecoderException.class, () -> channel.writeInbound(frameHeader));
                assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
            } finally {
                if (frameHeader.refCnt() > 0) {
                    ReferenceCountUtil.release(frameHeader);
                }
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decoderRejectsUndersizedFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyPacketDecoder());
        try {
            var frameHeader = Unpooled.buffer(Integer.BYTES).writeInt(PacketLimits.MIN_PACKET_SIZE - 1);
            try {
                DecoderException thrown = assertThrows(DecoderException.class, () -> channel.writeInbound(frameHeader));
                assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
            } finally {
                if (frameHeader.refCnt() > 0) {
                    ReferenceCountUtil.release(frameHeader);
                }
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decoderAcceptsFrameLengthAtLimit() {
        EmbeddedChannel channel = new EmbeddedChannel(new NettyPacketDecoder());
        try {
            var frameHeader = Unpooled.buffer(Integer.BYTES).writeInt(PacketLimits.MAX_PACKET_SIZE);
            assertFalse(channel.writeInbound(frameHeader));
            assertNull(channel.readInbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
