package dev.voxelgame.client.net;

import dev.voxelgame.client.ChatLog;
import dev.voxelgame.client.Hotbar;
import dev.voxelgame.client.PlayerStats;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.net.GamePacket;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ClientConnectionHandlerTest {
    @Test
    void dispatchesGameplayEventBatchesToConsumer() {
        AtomicReference<GamePacket.GameplayEvents> received = new AtomicReference<>();
        ClientConnectionHandler handler = new ClientConnectionHandler(
                "Player",
                new ClientWorld(123L),
                new Hotbar(),
                new PlayerStats(),
                new ChatLog(),
                new ClientNetworkStats(),
                snapshot -> {
                },
                impact -> {
                },
                received::set
        );
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        GamePacket.GameplayEvents packet = new GamePacket.GameplayEvents(List.of(
                new GameplayEvent.Pickup(4L, "voxel:moss_clump", 5)
        ));

        try {
            channel.writeInbound(packet);

            assertNotNull(received.get());
            assertEquals(packet.events(), received.get().events());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
