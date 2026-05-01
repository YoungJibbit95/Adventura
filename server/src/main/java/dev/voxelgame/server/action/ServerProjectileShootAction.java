package dev.voxelgame.server.action;

import dev.voxelgame.common.actions.ActionDefinitions;
import dev.voxelgame.common.actions.ActionExecutionResult;
import dev.voxelgame.common.actions.ActionPipeline;
import dev.voxelgame.common.actions.ActionRequest;
import dev.voxelgame.common.actions.ActionTarget;
import dev.voxelgame.common.actions.ActionValidationResult;
import dev.voxelgame.common.content.ContentKey;
import dev.voxelgame.common.content.ContentTagRegistry;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.gameplay.ProjectileItemRules;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ServerProjectileShootAction {
    private static final UUID UNAUTHENTICATED_ACTOR_ID = new UUID(0L, 0L);

    private final ActionPipeline actionPipeline;

    public ServerProjectileShootAction() {
        this(ActionPipeline.defaults(ContentTagRegistry.createDefault()));
    }

    ServerProjectileShootAction(ActionPipeline actionPipeline) {
        this.actionPipeline = Objects.requireNonNull(actionPipeline, "actionPipeline");
    }

    public ActionExecutionResult execute(Context context, GamePacket.ProjectileShoot shoot, double nowSeconds) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(shoot, "shoot");
        ActionRequest request = actionRequest(context, shoot, null);

        if (!context.loggedIn()) {
            context.closeConnection();
            return rejected(request, ActionValidationResult.RejectionReason.NOT_LOGGED_IN, "projectile shoot requires login");
        }
        if (!context.acceptProjectileShootRate(nowSeconds)) {
            return rejected(request, ActionValidationResult.RejectionReason.RATE_LIMITED, "projectile shoot rate limit exceeded");
        }
        if (!InteractionRules.isHotbarSlot(shoot.selectedSlot(), context.inventorySize())) {
            context.sendInventory();
            return rejected(request, ActionValidationResult.RejectionReason.INVALID_SLOT, "selected slot is outside the hotbar");
        }

        context.setHotbarSelection(shoot.selectedSlot());
        ItemStack selected = context.inventorySlot(shoot.selectedSlot());
        request = actionRequest(context, shoot, heldItemKey(selected, context.items()));
        ActionValidationResult validation = actionPipeline.validate(request);
        if (!validation.accepted()) {
            context.sendInventory();
            return ActionExecutionResult.rejected(request, validation);
        }
        if (nowSeconds < context.nextProjectileShootTime()) {
            context.sendInventory();
            return rejected(request, ActionValidationResult.RejectionReason.COOLDOWN, "projectile shoot is cooling down");
        }
        if (!ProjectileItemRules.canLaunch(selected, context.items())) {
            context.sendInventory();
            return rejected(request, ActionValidationResult.RejectionReason.INVALID_ITEM, "selected item cannot launch a projectile");
        }

        int durabilityDamage = ProjectileItemRules.durabilityDamageOnLaunch(selected, context.items());
        double cooldownSeconds = ProjectileItemRules.cooldownSeconds(selected, context.items());
        context.spawnProjectileFromCurrentLook();
        context.damageInventorySlot(shoot.selectedSlot(), durabilityDamage);
        context.setNextProjectileShootTime(nowSeconds + cooldownSeconds);
        context.markEntitySnapshotsDirty();
        context.sendInventory();
        context.sendEntitySnapshots();

        return ActionExecutionResult.accepted(request, List.of(ActionDefinitions.projectileShoot().resultEventKey()));
    }

    private static ActionExecutionResult rejected(ActionRequest request, ActionValidationResult.RejectionReason reason, String message) {
        return ActionExecutionResult.rejected(request, ActionValidationResult.rejected(reason, message));
    }

    private static ActionRequest actionRequest(Context context, GamePacket.ProjectileShoot shoot, ContentKey heldItemKey) {
        UUID actorId = context.playerId() == null ? UNAUTHENTICATED_ACTOR_ID : context.playerId();
        return ActionRequest.of(
                ActionDefinitions.PROJECTILE_SHOOT,
                actorId,
                heldItemKey,
                shoot.selectedSlot(),
                context.projectileTarget(),
                0,
                shoot.sequence()
        );
    }

    private static ContentKey heldItemKey(ItemStack selected, Registry<ItemType> items) {
        if (selected.isEmpty()) {
            return null;
        }
        return items.findById(selected.itemId())
                .map(item -> ContentKey.item(item.key()))
                .orElse(null);
    }

    public interface Context {
        UUID playerId();

        boolean loggedIn();

        void closeConnection();

        boolean acceptProjectileShootRate(double nowSeconds);

        int inventorySize();

        void sendInventory();

        void setHotbarSelection(int selectedSlot);

        ItemStack inventorySlot(int slot);

        Registry<ItemType> items();

        ActionTarget projectileTarget();

        double nextProjectileShootTime();

        void spawnProjectileFromCurrentLook();

        void damageInventorySlot(int slot, int durabilityDamage);

        void setNextProjectileShootTime(double nextProjectileShootTime);

        void markEntitySnapshotsDirty();

        void sendEntitySnapshots();
    }
}
