package dev.voxelgame.client;

import dev.voxelgame.common.net.GamePacket;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraTest {
    @Test
    void softAuthoritativeCorrectionSmoothsMediumPredictionError() {
        Camera camera = new Camera();
        camera.setPosition(0.0f, 0.0f, 0.0f);

        camera.reconcileAuthoritativePosition(2.0, 0.0, 0.0, false, GamePacket.MovementCorrection.SOFT);

        Vector3f position = camera.position();
        assertEquals(1.0f, position.x, 0.001f);
        assertEquals(0.0f, position.y, 0.001f);
        assertEquals(0.0f, position.z, 0.001f);
    }

    @Test
    void hardAuthoritativeCorrectionSnapsToServerPosition() {
        Camera camera = new Camera();
        camera.setPosition(0.0f, 0.0f, 0.0f);

        camera.reconcileAuthoritativePosition(2.0, 0.0, 0.0, false, GamePacket.MovementCorrection.HARD);

        Vector3f position = camera.position();
        assertEquals(2.0f, position.x, 0.001f);
    }

    @Test
    void respawnTeleportCorrectionSnapsEvenForSmallPredictionError() {
        Camera camera = new Camera();
        camera.setPosition(0.0f, 0.0f, 0.0f);

        camera.reconcileAuthoritativePosition(0.5, 0.0, 0.0, false, GamePacket.MovementCorrection.RESPAWN_TELEPORT);

        Vector3f position = camera.position();
        assertEquals(0.5f, position.x, 0.001f);
    }
}
