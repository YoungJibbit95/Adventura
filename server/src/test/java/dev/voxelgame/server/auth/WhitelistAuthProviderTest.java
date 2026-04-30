package dev.voxelgame.server.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistAuthProviderTest {
    @Test
    void authenticateMatchesWhitelistCaseInsensitivelyAndIgnoresSurroundingWhitespace() {
        WhitelistAuthProvider provider = new WhitelistAuthProvider(Set.of("Alice"));

        AuthResult result = provider.authenticate("  alice  ", "token");

        assertTrue(result.accepted());
        assertEquals(UUID.nameUUIDFromBytes("dev-player:alice".getBytes(StandardCharsets.UTF_8)), result.playerId());
    }

    @Test
    void authenticateRejectsWhenPlayerNotWhitelisted() {
        WhitelistAuthProvider provider = new WhitelistAuthProvider(Set.of("Alice"));

        AuthResult result = provider.authenticate("Bob", "token");

        assertFalse(result.accepted());
        assertEquals("Player is not whitelisted", result.message());
    }
}
