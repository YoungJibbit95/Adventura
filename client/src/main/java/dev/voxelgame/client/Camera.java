package dev.voxelgame.client;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

public final class Camera {
    private final Vector3f position = new Vector3f(8.0f, 118.0f, 8.0f);
    private final ClientPlayerController playerController = new ClientPlayerController();
    private float yaw = -45.0f;
    private float pitch = -24.0f;
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;

    public void update(long window, float deltaSeconds) {
        update(window, deltaSeconds, 1.0f);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale) {
        updateMouse(window, sensitivityScale);
        playerController.updateFreecam(window, deltaSeconds, position, front());
    }

    public void update(long window, float deltaSeconds, float sensitivityScale, ClientWorld world, GameMode gameMode) {
        update(window, deltaSeconds, sensitivityScale, world, gameMode, true);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale, ClientWorld world, GameMode gameMode, boolean sprintAllowed) {
        updateMouse(window, sensitivityScale);
        if (world == null || !gameMode.hasCollision()) {
            playerController.updateFreecam(window, deltaSeconds, position, front());
            return;
        }
        if (gameMode.canFly()) {
            playerController.updateFlying(window, deltaSeconds, position, world, front());
        } else {
            playerController.updateSurvival(window, deltaSeconds, position, world, front(), sprintAllowed);
        }
    }

    public void resetMouseTracking() {
        firstMouse = true;
    }

    public Matrix4f viewMatrix() {
        Vector3f target = new Vector3f(position).add(front());
        return new Matrix4f().lookAt(position, target, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public Vector3f position() {
        return new Vector3f(position);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        playerController.resetVelocity();
    }

    public void reconcileAuthoritativePosition(double x, double y, double z, boolean grounded) {
        reconcileAuthoritativePosition(x, y, z, grounded, GamePacket.MovementCorrection.SOFT);
    }

    public void reconcileAuthoritativePosition(
            double x,
            double y,
            double z,
            boolean grounded,
            GamePacket.MovementCorrection correction
    ) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return;
        }
        float targetX = (float) x;
        float targetY = (float) y;
        float targetZ = (float) z;
        float dx = targetX - position.x;
        float dy = targetY - position.y;
        float dz = targetZ - position.z;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        GamePacket.MovementCorrection safeCorrection = correction == null ? GamePacket.MovementCorrection.SOFT : correction;
        if (safeCorrection == GamePacket.MovementCorrection.RESPAWN_TELEPORT
                || safeCorrection == GamePacket.MovementCorrection.HARD
                || distanceSquared > 9.0f) {
            setPosition(targetX, targetY, targetZ);
        } else if (distanceSquared > 2.25f) {
            position.lerp(new Vector3f(targetX, targetY, targetZ), 0.5f);
        }
        playerController.reconcileGrounded(grounded);
    }

    public Vector3f forward() {
        return front();
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public boolean onGround() {
        return playerController.onGround();
    }

    public float consumeFallImpactSpeed() {
        return playerController.consumeFallImpactSpeed();
    }

    public boolean wantsSprint(long window) {
        return playerController.wantsSprint(window);
    }

    public boolean hasMovementInput(long window) {
        return playerController.hasMovementInput(window);
    }

    private void updateMouse(long window, float sensitivityScale) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer mouseX = stack.mallocDouble(1);
            DoubleBuffer mouseY = stack.mallocDouble(1);
            glfwGetCursorPos(window, mouseX, mouseY);
            double x = mouseX.get(0);
            double y = mouseY.get(0);
            if (firstMouse) {
                lastMouseX = x;
                lastMouseY = y;
                firstMouse = false;
                return;
            }
            double dx = x - lastMouseX;
            double dy = lastMouseY - y;
            lastMouseX = x;
            lastMouseY = y;

            float sensitivity = 0.08f * sensitivityScale;
            yaw += (float) dx * sensitivity;
            pitch += (float) dy * sensitivity;
            pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        }
    }

    private Vector3f front() {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.cos(yawRad) * Math.cos(pitchRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.sin(yawRad) * Math.cos(pitchRad))
        ).normalize();
    }
}
