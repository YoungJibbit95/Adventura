package dev.voxelgame.server.auth;

public interface AuthProvider {
    AuthResult authenticate(String username, String authToken);
}
