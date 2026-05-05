package dev.voxelgame.client;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.physics.PlayerInput;
import dev.voxelgame.common.physics.PlayerPhysics;
import dev.voxelgame.common.physics.PlayerPhysicsConfig;
import dev.voxelgame.common.physics.PlayerState;
import dev.voxelgame.common.physics.PlayerWaterState;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

final class ClientPlayerController {
    private static final PlayerPhysicsConfig PHYSICS = PlayerPhysicsConfig.defaults();
    private static final float GROUND_JUMP_GRACE_SECONDS = 0.10f;

    private final Vector3f velocity = new Vector3f();
    private boolean onGround;
    private float lastFallImpactSpeed;
    private float coyoteTimeSeconds;
    private float jumpBufferSeconds;

    void updateFreecam(long window, float deltaSeconds, Vector3f position, Vector3f forward) {
        float speed = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ? 58.0f : 24.0f;
        float amount = speed * deltaSeconds;
        Vector3f front = new Vector3f(forward);
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
        velocity.zero();
        onGround = false;
        coyoteTimeSeconds = 0.0f;
        jumpBufferSeconds = 0.0f;
    }

    void updateFlying(long window, float deltaSeconds, Vector3f position, ClientWorld world, Vector3f forward) {
        Vector3f move = movementInput(window, forward, true);
        PlayerState state = new PlayerState(
                position.x,
                position.y,
                position.z,
                velocity.x,
                velocity.y,
                velocity.z,
                onGround,
                world.playerWaterState(position).headUnderwater(),
                0.0f,
                0.0f,
                0.0f
        );
        PlayerState next = PlayerPhysics.stepFlying(
                state,
                move.x,
                move.y,
                move.z,
                wantsSprint(window),
                deltaSeconds,
                PHYSICS,
                world::collidesPlayer
        );
        applyState(position, next);
    }

    void updateSurvival(long window, float deltaSeconds, Vector3f position, ClientWorld world, Vector3f forward, boolean sprintAllowed) {
        PlayerWaterState water = world.playerWaterState(position);
        Vector3f move = movementInput(window, forward, false);
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
                0.0f,
                coyoteTimeSeconds,
                jumpBufferSeconds
        );
        PlayerState next = PlayerPhysics.stepSurvival(
                state,
                input,
                water,
                deltaSeconds,
                PHYSICS,
                world::collidesPlayer,
                world::surfaceAt
        );
        applyState(position, next);
        lastFallImpactSpeed = next.fallImpactSpeed();
        if (position.y < world.dimension().minY() - 16.0f) {
            Vector3f spawn = world.spawnPosition();
            position.set(spawn);
            resetVelocity();
            onGround = false;
            coyoteTimeSeconds = 0.0f;
            jumpBufferSeconds = 0.0f;
        }
    }

    boolean wantsSprint(long window) {
        return glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
    }

    boolean hasMovementInput(long window) {
        return glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
    }

    boolean onGround() {
        return onGround;
    }

    void resetVelocity() {
        velocity.zero();
        jumpBufferSeconds = 0.0f;
    }

    void reconcileGrounded(boolean grounded) {
        onGround = grounded;
        coyoteTimeSeconds = grounded ? GROUND_JUMP_GRACE_SECONDS : 0.0f;
        if (grounded && velocity.y < 0.0f) {
            velocity.y = 0.0f;
        }
    }

    float consumeFallImpactSpeed() {
        float value = lastFallImpactSpeed;
        lastFallImpactSpeed = 0.0f;
        return value;
    }

    private void applyState(Vector3f position, PlayerState next) {
        position.set((float) next.x(), (float) next.y(), (float) next.z());
        velocity.set(next.velocityX(), next.velocityY(), next.velocityZ());
        onGround = next.onGround();
        coyoteTimeSeconds = next.coyoteTimeSeconds();
        jumpBufferSeconds = next.jumpBufferSeconds();
    }

    private Vector3f movementInput(long window, Vector3f cameraForward, boolean vertical) {
        Vector3f front = new Vector3f(cameraForward);
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
}
