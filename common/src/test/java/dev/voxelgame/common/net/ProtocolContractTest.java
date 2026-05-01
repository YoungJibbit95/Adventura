package dev.voxelgame.common.net;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolContractTest {
    @Test
    void everyPacketTypeHasExactlyOneContract() {
        assertEquals(PacketType.values().length, ProtocolContract.entries().size());

        EnumSet<PacketType> seen = EnumSet.noneOf(PacketType.class);
        for (ProtocolContract.Entry entry : ProtocolContract.entries()) {
            assertTrue(seen.add(entry.type()), "duplicate contract for " + entry.type());
            assertEquals(entry.type(), ProtocolContract.require(entry.type()).type());
            assertTrue(entry.maxPayloadBytes() >= PacketLimits.MIN_PACKET_SIZE);
            assertTrue(entry.maxPayloadBytes() <= PacketLimits.MAX_PACKET_SIZE);
        }
    }

    @Test
    void protocolDocumentListsCurrentVersionChangelogAndPacketContracts() throws Exception {
        String document = Files.readString(repoPath("docs/NETWORK_PROTOCOL.md"));

        assertTrue(document.contains("Aktuelle Protocol Version: `" + GamePacket.PROTOCOL_VERSION + "`"));
        assertTrue(document.contains("## Version " + GamePacket.PROTOCOL_VERSION));
        for (PacketType type : PacketType.values()) {
            ProtocolContract.Entry entry = ProtocolContract.require(type);
            String rowPrefix = "| `" + type.name()
                    + "` | " + type.id()
                    + " | `" + entry.direction().name()
                    + "` | `" + entry.authority().name()
                    + "` |";
            assertTrue(document.contains(rowPrefix), "missing packet row for " + type);
        }
    }

    private static Path repoPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current.resolve(relativePath);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root for " + relativePath);
    }
}
