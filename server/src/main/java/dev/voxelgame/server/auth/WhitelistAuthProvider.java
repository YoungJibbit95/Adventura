package dev.voxelgame.server.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WhitelistAuthProvider implements AuthProvider {
    private final Set<String> whitelist;

    public WhitelistAuthProvider(Set<String> whitelist) {
        this.whitelist = whitelist.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(WhitelistAuthProvider::normalizeUsername)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public AuthResult authenticate(String username, String authToken) {
        if (username == null || username.isBlank()) {
            return AuthResult.rejected("Username is required");
        }
        String normalizedUsername = normalizeUsername(username);
        if (!whitelist.isEmpty() && !whitelist.contains(normalizedUsername)) {
            return AuthResult.rejected("Player is not whitelisted");
        }
        UUID offlineUuid = UUID.nameUUIDFromBytes(("dev-player:" + normalizedUsername).getBytes(StandardCharsets.UTF_8));
        return AuthResult.accepted(offlineUuid);
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
