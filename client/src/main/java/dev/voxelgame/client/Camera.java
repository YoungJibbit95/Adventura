package dev.voxelgame.client;

import dev.voxelgame.client.world.ClientWorld;
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
    private static final float EYE_HEIGHT = 1.62f;
    private static final float GRAVITY = 34.0f;
    private static final float JUMP_SPEED = 9.2f;

    private final Vector3f position = new Vector3f(8.0f, 118.0f, 8.0f);
    private final Vector3f velocity = new Vector3f();
    private float yaw = -45.0f;
    private float pitch = -24.0f;
    private boolean onGround;
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;

    public void update(long window, float deltaSeconds) {
        update(window, deltaSeconds, 1.0f);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale) {
        updateMouse(window, sensitivityScale);
        updateMovement(window, deltaSeconds);
    }

    public void update(long window, float deltaSeconds, float sensitivityScale, ClientWorld world, GameMode gameMode) {
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
            updateSurvivalPhysics(window, deltaSeconds, world);
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
        float speed = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ? 44.0f : 18.0f;
        Vector3f move = movementInput(window, true);
        if (move.lengthSquared() > 0.0f) {
            move.normalize().mul(speed * deltaSeconds);
            moveWithCollision(world, move.x, move.y, move.z);
        }
        onGround = world.collidesPlayer(position.x, position.y - 0.08f, position.z);
    }

    private void updateSurvivalPhysics(long window, float deltaSeconds, ClientWorld world) {
        float speed = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ? 8.0f : 5.2f;
        Vector3f move = movementInput(window, false);
        if (move.lengthSquared() > 0.0f) {
            move.normalize().mul(speed);
        }
        velocity.x = move.x;
        velocity.z = move.z;
        if (onGround && glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }
        velocity.y -= GRAVITY * deltaSeconds;
        velocity.y = Math.max(velocity.y, -42.0f);

        moveWithCollision(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        moveWithCollision(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
        boolean verticalCollision = moveWithCollision(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        if (verticalCollision) {
            onGround = velocity.y < 0.0f;
            velocity.y = 0.0f;
        } else {
            onGround = false;
        }
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

    private boolean moveWithCollision(ClientWorld world, float dx, float dy, float dz) {
        if (dx == 0.0f && dy == 0.0f && dz == 0.0f) {
            return false;
        }
        if (!world.collidesPlayer(position.x + dx, position.y + dy, position.z + dz)) {
            position.add(dx, dy, dz);
            return false;
        }
        return true;
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
