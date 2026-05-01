package dev.voxelgame.common.actions;

import dev.voxelgame.common.content.ContentKey;
import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.content.ContentTagRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionPipelineTest {
    private static final UUID PLAYER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void projectileShootRequiresRangedHeldItemAndDirectionTarget() {
        ActionPipeline pipeline = ActionPipeline.defaults(ContentTagRegistry.createDefault());
        ActionRequest knifeThrow = projectileRequest(ContentKey.item("voxel:stone_knife"));

        ActionValidationResult accepted = pipeline.validate(knifeThrow);

        assertTrue(accepted.accepted());
        ActionDefinition definition = pipeline.definitions().get(ActionDefinitions.PROJECTILE_SHOOT);
        assertEquals(ActionTargetType.DIRECTION, definition.targetType());
        assertEquals(1, definition.cost().durabilityDamage());
        assertEquals(0.55, definition.cooldown().seconds(), 0.0001);
        assertTrue(definition.requiredHeldItemTags().contains(ContentTag.RANGED));
    }

    @Test
    void projectileShootRejectsNonRangedHeldItem() {
        ActionPipeline pipeline = ActionPipeline.defaults(ContentTagRegistry.createDefault());

        ActionValidationResult result = pipeline.validate(projectileRequest(ContentKey.item("voxel:stone_pickaxe")));

        assertFalse(result.accepted());
        assertEquals(ActionValidationResult.RejectionReason.MISSING_REQUIRED_TAG, result.reason());
    }

    @Test
    void projectileShootRejectsWrongTargetTypeBeforeServerExecution() {
        ActionPipeline pipeline = ActionPipeline.defaults(ContentTagRegistry.createDefault());
        ActionRequest request = ActionRequest.of(
                ActionDefinitions.PROJECTILE_SHOOT,
                PLAYER_ID,
                ContentKey.item("voxel:stone_knife"),
                0,
                new ActionTarget.Block(1, 64, 1, 0, 1, 0),
                7,
                99L
        );

        ActionValidationResult result = pipeline.validate(request);

        assertFalse(result.accepted());
        assertEquals(ActionValidationResult.RejectionReason.TARGET_MISMATCH, result.reason());
    }

    @Test
    void eatActionUsesFoodTagsFromContentRegistry() {
        ActionPipeline pipeline = ActionPipeline.defaults(ContentTagRegistry.createDefault());
        ActionRequest request = new ActionRequest(
                ActionDefinitions.EAT,
                PLAYER_ID,
                java.util.Optional.of(ContentKey.item("voxel:berries")),
                2,
                new ActionTarget.None(),
                17,
                3L
        );

        ActionExecutionResult result = pipeline.acceptValidated(request, List.of("item.eat"));

        assertTrue(result.accepted());
        assertEquals(17, result.transactionId());
        assertEquals(List.of("item.eat"), result.eventKeys());
    }

    @Test
    void unknownActionIsRejectedWithoutExecution() {
        ActionPipeline pipeline = ActionPipeline.defaults(ContentTagRegistry.createDefault());
        ActionRequest request = new ActionRequest(
                "voxel:not_real",
                PLAYER_ID,
                java.util.Optional.empty(),
                -1,
                new ActionTarget.None(),
                0,
                1L
        );

        ActionValidationResult result = pipeline.validate(request);

        assertFalse(result.accepted());
        assertEquals(ActionValidationResult.RejectionReason.UNKNOWN_ACTION, result.reason());
    }

    @Test
    void actionModelRejectsInvalidInputEarly() {
        assertThrows(IllegalArgumentException.class, () -> new ActionCooldown(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ActionCost(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActionTarget.Entity(-1L));
        assertThrows(IllegalArgumentException.class, () -> new ActionTarget.Direction(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }

    private static ActionRequest projectileRequest(ContentKey heldItem) {
        return ActionRequest.of(
                ActionDefinitions.PROJECTILE_SHOOT,
                PLAYER_ID,
                heldItem,
                0,
                new ActionTarget.Direction(0.0, 64.0, 0.0, 1.0, 0.0, 0.0),
                42,
                11L
        );
    }
}
