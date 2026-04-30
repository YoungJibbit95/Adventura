package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;

import java.util.Objects;

public record DroppedItemEntity(
        long entityId,
        String itemKey,
        ItemStack stack,
        double x,
        double y,
        double z,
        double groundY,
        double velocityX,
        double velocityY,
        double velocityZ,
        long createdTick
) {
    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final double GRAVITY = 18.0;
    private static final double AIR_DRAG = 0.92;
    private static final double GROUND_DRAG = 0.72;
    private static final double BOUNCE = 0.22;
    private static final double REST_VELOCITY = 0.08;
    private static final long PICKUP_DELAY_TICKS = 8L;

    public DroppedItemEntity {
        Objects.requireNonNull(itemKey, "itemKey");
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Dropped item stack cannot be empty");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(groundY)
                || !Double.isFinite(velocityX) || !Double.isFinite(velocityY) || !Double.isFinite(velocityZ)) {
            throw new IllegalArgumentException("Dropped item coordinates must be finite");
        }
    }

    public DroppedItemEntity tick(long tick) {
        double nextVelocityY = velocityY - GRAVITY * TICK_SECONDS;
        double nextX = x + velocityX * TICK_SECONDS;
        double nextY = y + nextVelocityY * TICK_SECONDS;
        double nextZ = z + velocityZ * TICK_SECONDS;
        double nextVelocityX = velocityX * AIR_DRAG;
        double nextVelocityZ = velocityZ * AIR_DRAG;

        if (nextY <= groundY) {
            nextY = groundY;
            if (Math.abs(nextVelocityY) > REST_VELOCITY) {
                nextVelocityY = -nextVelocityY * BOUNCE;
            } else {
                nextVelocityY = 0.0;
            }
            nextVelocityX *= GROUND_DRAG;
            nextVelocityZ *= GROUND_DRAG;
        }

        return new DroppedItemEntity(
                entityId,
                itemKey,
                stack,
                nextX,
                nextY,
                nextZ,
                groundY,
                nextVelocityX,
                nextVelocityY,
                nextVelocityZ,
                createdTick
        );
    }

    public boolean canPickup(long tick) {
        return tick - createdTick >= PICKUP_DELAY_TICKS;
    }

    public double distanceSquared(double targetX, double targetY, double targetZ) {
        double dx = x - targetX;
        double dy = y - targetY;
        double dz = z - targetZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public EntitySnapshot snapshot() {
        return new EntitySnapshot(
                entityId,
                ItemDropType.typeKey(itemKey),
                null,
                x,
                y,
                z,
                yaw(),
                0.0f,
                1,
                EntitySnapshot.STATE_IDLE,
                velocityX,
                velocityY,
                velocityZ
        );
    }

    private float yaw() {
        return (float) Math.floorMod(entityId * 37L, 360L);
    }
}
