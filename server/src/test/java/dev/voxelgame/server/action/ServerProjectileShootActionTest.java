package dev.voxelgame.server.action;

import dev.voxelgame.common.actions.ActionDefinitions;
import dev.voxelgame.common.actions.ActionExecutionResult;
import dev.voxelgame.common.actions.ActionTarget;
import dev.voxelgame.common.actions.ActionValidationResult;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerProjectileShootActionTest {
    private final ServerProjectileShootAction action = new ServerProjectileShootAction();

    @Test
    void acceptedShootSpawnsProjectileDamagesLauncherAndReportsResult() {
        FakeContext context = new FakeContext();
        short knife = context.items.requireByKey("voxel:stone_knife").id();
        context.inventory.setSlot(0, new ItemStack(knife, 1));

        ActionExecutionResult result = action.execute(context, new GamePacket.ProjectileShoot(0, 7L), 10.0);

        assertTrue(result.accepted());
        assertEquals(ActionDefinitions.PROJECTILE_SHOOT, result.actionKey());
        assertEquals(List.of("projectile.shoot"), result.eventKeys());
        assertEquals(0, context.hotbarSelection);
        assertEquals(1, context.spawnedProjectiles);
        assertEquals(new ItemStack(knife, 1, 1), context.inventory.slot(0));
        assertEquals(1, context.sentInventories);
        assertEquals(1, context.sentEntitySnapshots);
        assertTrue(context.entitySnapshotsDirty);
        assertTrue(context.nextProjectileShootTime > 10.0);
    }

    @Test
    void invalidSlotRejectsAndResendsInventoryWithoutSideEffects() {
        FakeContext context = new FakeContext();

        ActionExecutionResult result = action.execute(context, new GamePacket.ProjectileShoot(99, 7L), 10.0);

        assertFalse(result.accepted());
        assertEquals(ActionValidationResult.RejectionReason.INVALID_SLOT, result.validation().reason());
        assertEquals(1, context.sentInventories);
        assertEquals(0, context.spawnedProjectiles);
        assertTrue(result.eventKeys().isEmpty());
    }

    @Test
    void rateLimitedShootRejectsWithoutInventorySpam() {
        FakeContext context = new FakeContext();
        context.rateAccepted = false;

        ActionExecutionResult result = action.execute(context, new GamePacket.ProjectileShoot(0, 7L), 10.0);

        assertFalse(result.accepted());
        assertEquals(ActionValidationResult.RejectionReason.RATE_LIMITED, result.validation().reason());
        assertEquals(0, context.sentInventories);
        assertEquals(0, context.spawnedProjectiles);
    }

    private static final class FakeContext implements ServerProjectileShootAction.Context {
        private final Registry<ItemType> items = Items.createDefaultRegistry();
        private final Inventory inventory = new Inventory(36);
        private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        private boolean loggedIn = true;
        private boolean rateAccepted = true;
        private boolean closed;
        private boolean entitySnapshotsDirty;
        private int hotbarSelection = -1;
        private int sentInventories;
        private int spawnedProjectiles;
        private int sentEntitySnapshots;
        private double nextProjectileShootTime;

        @Override
        public UUID playerId() {
            return playerId;
        }

        @Override
        public boolean loggedIn() {
            return loggedIn;
        }

        @Override
        public void closeConnection() {
            closed = true;
        }

        @Override
        public boolean acceptProjectileShootRate(double nowSeconds) {
            return rateAccepted;
        }

        @Override
        public int inventorySize() {
            return inventory.size();
        }

        @Override
        public void sendInventory() {
            sentInventories++;
        }

        @Override
        public void setHotbarSelection(int selectedSlot) {
            hotbarSelection = selectedSlot;
        }

        @Override
        public ItemStack inventorySlot(int slot) {
            return inventory.slot(slot);
        }

        @Override
        public Registry<ItemType> items() {
            return items;
        }

        @Override
        public ActionTarget projectileTarget() {
            return new ActionTarget.Direction(0.0, 80.0, 0.0, 1.0, 0.0, 0.0);
        }

        @Override
        public double nextProjectileShootTime() {
            return nextProjectileShootTime;
        }

        @Override
        public void spawnProjectileFromCurrentLook() {
            spawnedProjectiles++;
        }

        @Override
        public void damageInventorySlot(int slot, int durabilityDamage) {
            inventory.damageSlot(slot, durabilityDamage, items);
        }

        @Override
        public void setNextProjectileShootTime(double nextProjectileShootTime) {
            this.nextProjectileShootTime = nextProjectileShootTime;
        }

        @Override
        public void markEntitySnapshotsDirty() {
            entitySnapshotsDirty = true;
        }

        @Override
        public void sendEntitySnapshots() {
            sentEntitySnapshots++;
        }
    }
}
