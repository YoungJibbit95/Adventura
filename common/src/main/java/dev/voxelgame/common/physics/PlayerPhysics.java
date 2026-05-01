package dev.voxelgame.common.physics;

public final class PlayerPhysics {
    private static final float GROUND_ACCELERATION_PER_SECOND = 22.0f;
    private static final float GROUND_FRICTION_PER_SECOND = 18.0f;
    private static final float AIR_CONTROL_PER_SECOND = 4.5f;
    private static final float AIR_HORIZONTAL_DRAG = 0.985f;
    private static final float WATER_CONTROL_PER_SECOND = 6.5f;
    private static final float COYOTE_TIME_SECONDS = 0.10f;
    private static final float JUMP_BUFFER_SECONDS = 0.10f;

    private PlayerPhysics() {
    }

    @FunctionalInterface
    public interface CollisionQuery {
        boolean collides(double eyeX, double eyeY, double eyeZ);
    }

    public static PlayerState stepSurvival(
            PlayerState state,
            PlayerInput input,
            PlayerWaterState water,
            float deltaSeconds,
            PlayerPhysicsConfig config,
            CollisionQuery collisionQuery
    ) {
        if (state == null || input == null || water == null || config == null || collisionQuery == null) {
            throw new IllegalArgumentException("Player physics step arguments are required");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("Delta seconds must be finite and non-negative");
        }
        if (deltaSeconds == 0.0f) {
            return new PlayerState(
                    state.x(),
                    state.y(),
                    state.z(),
                    state.velocityX(),
                    state.velocityY(),
                    state.velocityZ(),
                    state.onGround(),
                    water.headUnderwater(),
                    0.0f,
                    state.coyoteTimeSeconds(),
                    state.jumpBufferSeconds()
            );
        }

        boolean swimming = water.movementAffected();
        boolean sprinting = input.sprint() && !swimming;
        float speed = horizontalSpeed(config, sprinting, swimming);
        float targetVelocityX = input.moveX() * speed;
        float targetVelocityZ = input.moveZ() * speed;
        float velocityX = state.velocityX();
        float velocityZ = state.velocityZ();
        float velocityY = state.velocityY();
        boolean onGround = state.onGround();
        float coyoteTimeSeconds = onGround
                ? COYOTE_TIME_SECONDS
                : Math.max(0.0f, state.coyoteTimeSeconds() - deltaSeconds);
        float jumpBufferSeconds = input.jump()
                ? JUMP_BUFFER_SECONDS
                : Math.max(0.0f, state.jumpBufferSeconds() - deltaSeconds);

        if (swimming) {
            float waterControl = Math.min(1.0f, deltaSeconds * WATER_CONTROL_PER_SECOND);
            velocityX += (targetVelocityX - velocityX) * waterControl;
            velocityZ += (targetVelocityZ - velocityZ) * waterControl;
            velocityX = dampSmallVelocity(velocityX * config.waterHorizontalDrag());
            velocityZ = dampSmallVelocity(velocityZ * config.waterHorizontalDrag());
        } else if (onGround) {
            if (input.moveX() != 0.0f || input.moveZ() != 0.0f) {
                float acceleration = Math.min(1.0f, deltaSeconds * GROUND_ACCELERATION_PER_SECOND);
                velocityX += (targetVelocityX - velocityX) * acceleration;
                velocityZ += (targetVelocityZ - velocityZ) * acceleration;
            } else {
                float friction = Math.max(0.0f, 1.0f - deltaSeconds * GROUND_FRICTION_PER_SECOND);
                velocityX = dampSmallVelocity(velocityX * friction);
                velocityZ = dampSmallVelocity(velocityZ * friction);
            }
        } else {
            float airControl = Math.min(1.0f, deltaSeconds * AIR_CONTROL_PER_SECOND);
            velocityX += (targetVelocityX - velocityX) * airControl;
            velocityZ += (targetVelocityZ - velocityZ) * airControl;
            velocityX *= AIR_HORIZONTAL_DRAG;
            velocityZ *= AIR_HORIZONTAL_DRAG;
        }

        if (swimming && input.jump()) {
            velocityY = Math.max(velocityY, config.swimRiseSpeed());
            jumpBufferSeconds = 0.0f;
        } else if (jumpBufferSeconds > 0.0f && (onGround || coyoteTimeSeconds > 0.0f)) {
            velocityY = config.jumpSpeed();
            onGround = false;
            coyoteTimeSeconds = 0.0f;
            jumpBufferSeconds = 0.0f;
        }

        if (swimming) {
            velocityY -= config.gravity() * config.waterGravityMultiplier() * deltaSeconds;
            velocityY *= config.waterVerticalDrag();
            velocityY = Math.max(velocityY, -config.maxWaterFallSpeed());
        } else {
            velocityY -= config.gravity() * deltaSeconds;
            velocityY = Math.max(velocityY, -config.maxFallSpeed());
        }

        Movement movement = moveWithCollision(state.x(), state.y(), state.z(), velocityX * deltaSeconds, 0.0f, 0.0f, config, collisionQuery);
        movement = moveWithCollision(movement.x(), movement.y(), movement.z(), 0.0f, 0.0f, velocityZ * deltaSeconds, config, collisionQuery);
        movement = moveWithCollision(movement.x(), movement.y(), movement.z(), 0.0f, velocityY * deltaSeconds, 0.0f, config, collisionQuery);

        float fallImpactSpeed = 0.0f;
        if (movement.collided()) {
            if (velocityY < -15.0f && !swimming) {
                fallImpactSpeed = -velocityY;
            }
            onGround = velocityY < 0.0f;
            velocityY = 0.0f;
            if (onGround) {
                coyoteTimeSeconds = COYOTE_TIME_SECONDS;
                if (!swimming && jumpBufferSeconds > 0.0f) {
                    velocityY = config.jumpSpeed();
                    onGround = false;
                    fallImpactSpeed = 0.0f;
                    coyoteTimeSeconds = 0.0f;
                    jumpBufferSeconds = 0.0f;
                }
            }
        } else {
            onGround = false;
        }

        return new PlayerState(
                movement.x(),
                movement.y(),
                movement.z(),
                velocityX,
                velocityY,
                velocityZ,
                onGround,
                water.headUnderwater(),
                fallImpactSpeed,
                coyoteTimeSeconds,
                jumpBufferSeconds
        );
    }

    public static PlayerState stepFlying(
            PlayerState state,
            float moveX,
            float moveY,
            float moveZ,
            boolean sprinting,
            float deltaSeconds,
            PlayerPhysicsConfig config,
            CollisionQuery collisionQuery
    ) {
        if (state == null || config == null || collisionQuery == null) {
            throw new IllegalArgumentException("Player flying step arguments are required");
        }
        if (!Float.isFinite(moveX) || !Float.isFinite(moveY) || !Float.isFinite(moveZ)
                || !Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("Flying movement must be finite and delta must be non-negative");
        }

        float velocityX = 0.0f;
        float velocityY = 0.0f;
        float velocityZ = 0.0f;
        float lengthSquared = moveX * moveX + moveY * moveY + moveZ * moveZ;
        if (lengthSquared > 0.0f) {
            float speed = flyingSpeed(config, sprinting);
            float invLength = 1.0f / (float) Math.sqrt(lengthSquared);
            velocityX = moveX * invLength * speed;
            velocityY = moveY * invLength * speed;
            velocityZ = moveZ * invLength * speed;
        }

        Movement movement = moveWithCollision(
                state.x(),
                state.y(),
                state.z(),
                velocityX * deltaSeconds,
                velocityY * deltaSeconds,
                velocityZ * deltaSeconds,
                config,
                collisionQuery
        );
        boolean grounded = collisionQuery.collides(movement.x(), movement.y() - config.groundProbeDistance(), movement.z());
        return new PlayerState(
                movement.x(),
                movement.y(),
                movement.z(),
                0.0f,
                0.0f,
                0.0f,
                grounded,
                state.underwater(),
                0.0f
        );
    }

    public static int collisionSubsteps(float dx, float dy, float dz, PlayerPhysicsConfig config) {
        float maxDistance = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (maxDistance <= 0.0f) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(maxDistance / config.maxCollisionStep()));
    }

    public static float horizontalSpeed(PlayerPhysicsConfig config, boolean sprinting, boolean underwater) {
        float speed = sprinting ? config.sprintSpeed() : config.walkSpeed();
        return underwater ? speed * config.waterSpeedMultiplier() : speed;
    }

    public static float flyingSpeed(PlayerPhysicsConfig config, boolean sprinting) {
        return sprinting ? config.flySprintSpeed() : config.flySpeed();
    }

    private static Movement moveWithCollision(
            double x,
            double y,
            double z,
            float dx,
            float dy,
            float dz,
            PlayerPhysicsConfig config,
            CollisionQuery collisionQuery
    ) {
        if (dx == 0.0f && dy == 0.0f && dz == 0.0f) {
            return new Movement(x, y, z, false);
        }
        int steps = collisionSubsteps(dx, dy, dz, config);
        float stepX = dx / steps;
        float stepY = dy / steps;
        float stepZ = dz / steps;
        double nextX = x;
        double nextY = y;
        double nextZ = z;
        for (int i = 0; i < steps; i++) {
            if (collisionQuery.collides(nextX + stepX, nextY + stepY, nextZ + stepZ)) {
                return new Movement(nextX, nextY, nextZ, true);
            }
            nextX += stepX;
            nextY += stepY;
            nextZ += stepZ;
        }
        return new Movement(nextX, nextY, nextZ, false);
    }

    private static float dampSmallVelocity(float velocity) {
        return Math.abs(velocity) < 0.001f ? 0.0f : velocity;
    }

    private record Movement(double x, double y, double z, boolean collided) {
    }
}
