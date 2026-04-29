package dev.voxelgame.client;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.physics.PlayerInput;
import dev.voxelgame.common.physics.PlayerPhysics;
import dev.voxelgame.common.physics.PlayerPhysicsConfig;
import dev.voxelgame.common.physics.PlayerState;
import dev.voxelgame.common.physics.PlayerWaterState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public final class Camera {
    private static final PlayerPhysicsConfig PHYSICS = PlayerPhysicsConfig.defaults();

    private final Vector3f position = new Vector3f(8.0f, 118.0f, 8.0f);
    private final Vector3f velocity = new Vector3f();
    private float yaw = -45.0f;
    private float pitch = -24.0f;
    private boolean onGround;
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;
    private float lastFallImpactSpeed;

    public void update(long window, float deltaSeconds) {
        update(window, deltaSeconds, 1.0f);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale) {
        updateMouse(window, sensitivityScale);
        updateMovement(window, deltaSeconds);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale, ClientWorld world, GameMode gameMode) {
        update(window, deltaSeconds, sensitivityScale, world, gameMode, true);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale, ClientWorld world, GameMode gameMode, boolean sprintAllowed) {
        updateMouse(window, sensitivityScale);
        if (world == null || !gameMode.hasCollision()) {
            updateMovement(window, deltaSeconds);
            velocity.zero();
            onGround = false;
            return;
        }
        if (gameMode.canFly()) {
            updateFlyingCollision(window, deltaSeconds, world);
        } else {
            updateSurvivalPhysics(window, deltaSeconds, world, sprintAllowed);
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
        velocity.zero();
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
        return onGround;
    }

    public float consumeFallImpactSpeed() {
        float value = lastFallImpactSpeed;
        lastFallImpactSpeed = 0.0f;
        return value;
    }

    public boolean wantsSprint(long window) {
        return glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
    }

    public boolean hasMovementInput(long window) {
        return glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
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

    private void updateMovement(long window, float deltaSeconds) {
        float speed = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ? 58.0f : 24.0f;
        float amount = speed * deltaSeconds;
        Vector3f front = front();
        Vector3f right = new Vector3f(front).cross(0.0f, 1.0f, 0.0f).normalize();

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            position.fma(amount, front);
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            position.fma(-amount, front);
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            position.fma(amount, right);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            position.fma(-amount, right);
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            position.y += amount;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            position.y -= amount;
        }
    }

    private void updateFlyingCollision(long window, float deltaSeconds, ClientWorld world) {
        Vector3f move = movementInput(window, true);
        PlayerState state = new PlayerState(
                position.x,
                position.y,
                position.z,
                velocity.x,
                velocity.y,
                velocity.z,
                onGround,
                world.playerWaterState(position).headUnderwater(),
                0.0f
        );
        PlayerState next = PlayerPhysics.stepFlying(
                state,
                move.x,
                move.y,
                move.z,
                glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS,
                deltaSeconds,
                PHYSICS,
                world::collidesPlayer
        );
        position.set((float) next.x(), (float) next.y(), (float) next.z());
        velocity.set(next.velocityX(), next.velocityY(), next.velocityZ());
        onGround = next.onGround();
    }

    private void updateSurvivalPhysics(long window, float deltaSeconds, ClientWorld world, boolean sprintAllowed) {
        PlayerWaterState water = world.playerWaterState(position);
        Vector3f move = movementInput(window, false);
        if (move.lengthSquared() > 0.0f) {
            move.normalize();
        }
        PlayerInput input = new PlayerInput(
                move.x,
                move.z,
                glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
                false,
                sprintAllowed && wantsSprint(window)
        );
        PlayerState state = new PlayerState(
                position.x,
                position.y,
                position.z,
                velocity.x,
                velocity.y,
                velocity.z,
                onGround,
                water.headUnderwater(),
                0.0f
        );
        PlayerState next = PlayerPhysics.stepSurvival(state, input, water, deltaSeconds, PHYSICS, world::collidesPlayer);
        position.set((float) next.x(), (float) next.y(), (float) next.z());
        velocity.set(next.velocityX(), next.velocityY(), next.velocityZ());
        onGround = next.onGround();
        lastFallImpactSpeed = next.fallImpactSpeed();
        if (position.y < world.dimension().minY() - 16.0f) {
            Vector3f spawn = world.spawnPosition();
            setPosition(spawn.x, spawn.y, spawn.z);
        }
    }

    private Vector3f movementInput(long window, boolean vertical) {
        Vector3f front = front();
        if (!vertical) {
            front.y = 0.0f;
            if (front.lengthSquared() > 0.0f) {
                front.normalize();
            }
        }
        Vector3f right = new Vector3f(front).cross(0.0f, 1.0f, 0.0f);
        if (right.lengthSquared() > 0.0f) {
            right.normalize();
        }
        Vector3f move = new Vector3f();
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            move.add(front);
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            move.sub(front);
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            move.add(right);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            move.sub(right);
        }
        if (vertical && glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            move.y += 1.0f;
        }
        if (vertical && glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            move.y -= 1.0f;
        }
        return move;
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
