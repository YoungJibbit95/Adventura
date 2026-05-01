package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.FluidPhysics;
import dev.voxelgame.common.physics.PhysicsNumericGuard;

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
        if (!PhysicsNumericGuard.allFinite(x, y, z, groundY, velocityX, velocityY, velocityZ)) {
            throw new IllegalArgumentException("Dropped item coordinates must be finite");
        }
    }

    public DroppedItemEntity tick(long tick) {
        return tick(tick, (x, y, z) -> FluidPhysics.air());
    }

    public DroppedItemEntity tick(long tick, FluidPhysics.FluidQuery fluidQuery) {
        Objects.requireNonNull(fluidQuery, "fluidQuery");
        FluidPhysics.FluidSample fluid = fluidQuery.sample(x, y, z);
        double nextVelocityX;
        double nextVelocityY;
        double nextVelocityZ;
        if (fluid.inFluid()) {
            FluidPhysics.Velocity velocity = FluidPhysics.applyFloatingBodyForces(
                    new FluidPhysics.Velocity(velocityX, velocityY, velocityZ),
                    TICK_SECONDS,
                    fluid
            );
            nextVelocityX = velocity.x();
            nextVelocityY = velocity.y();
            nextVelocityZ = velocity.z();
        } else {
            nextVelocityX = velocityX * AIR_DRAG;
            nextVelocityY = velocityY - GRAVITY * TICK_SECONDS;
            nextVelocityZ = velocityZ * AIR_DRAG;
        }
        double nextX = x + nextVelocityX * TICK_SECONDS;
        double nextY = y + nextVelocityY * TICK_SECONDS;
        double nextZ = z + nextVelocityZ * TICK_SECONDS;

        if (!fluid.inFluid() && nextY <= groundY) {
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

    public DroppedItemEntity withPosition(double x, double y, double z) {
        return new DroppedItemEntity(entityId, itemKey, stack, x, y, z, groundY, velocityX, velocityY, velocityZ, createdTick);
    }

    public DroppedItemEntity withStack(ItemStack stack, long createdTick) {
        return new DroppedItemEntity(entityId, itemKey, stack, x, y, z, groundY, velocityX, velocityY, velocityZ, createdTick);
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
