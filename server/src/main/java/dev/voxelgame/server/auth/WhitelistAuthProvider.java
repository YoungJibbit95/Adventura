package dev.voxelgame.server.auth;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public final class WhitelistAuthProvider implements AuthProvider {
    private final Set<String> whitelist;

    public WhitelistAuthProvider(Set<String> whitelist) {
        this.whitelist = Set.copyOf(whitelist);
    }

    @Override
    public AuthResult authenticate(String username, String authToken) {
        if (username == null || username.isBlank()) {
            return AuthResult.rejected("Username is required");
        }
        if (!whitelist.isEmpty() && !whitelist.contains(username)) {
            return AuthResult.rejected("Player is not whitelisted");
        }
        UUID offlineUuid = UUID.nameUUIDFromBytes(("dev-player:" + username).getBytes(StandardCharsets.UTF_8));
        return AuthResult.accepted(offlineUuid);
    }
}
