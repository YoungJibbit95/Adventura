package dev.voxelgame.server.auth;

import java.util.UUID;

public record AuthResult(boolean accepted, UUID playerId, String message) {
    public static AuthResult accepted(UUID playerId) {
        return new AuthResult(true, playerId, "accepted");
    }

    public static AuthResult rejected(String message) {
        return new AuthResult(false, null, message);
    }
}
