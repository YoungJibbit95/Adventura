package dev.voxelgame.common.physics;

import dev.voxelgame.common.world.DimensionSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPhysicsTest {
    @Test
    void defaultBoundsAreDerivedFromEyePosition() {
        PlayerBounds bounds = PlayerBounds.DEFAULT;

        assertEquals(7.7, bounds.minX(8.0), 0.001);
        assertEquals(8.3, bounds.maxX(8.0), 0.001);
        assertEquals(78.38, bounds.minY(80.0), 0.001);
        assertEquals(80.18, bounds.maxY(80.0), 0.001);
    }

    @Test
    void collisionSubstepsSplitLargeMovements() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();

        assertEquals(1, PlayerPhysics.collisionSubsteps(0.1f, 0.0f, 0.0f, config));
        assertTrue(PlayerPhysics.collisionSubsteps(0.0f, -2.1f, 0.0f, config) > 1);
    }

    @Test
    void playerInputNormalizesHorizontalMovement() {
        PlayerInput input = new PlayerInput(1.0f, 1.0f, false, false, false);

        assertEquals(1.0f, input.moveX() * input.moveX() + input.moveZ() * input.moveZ(), 0.001f);
    }

    @Test
    void playerBoundsDetectPlacedBlockOverlap() {
        PlayerBounds bounds = PlayerBounds.DEFAULT;

        assertTrue(bounds.intersectsBlock(8.5, 65.62, 8.5, 8, 64, 8));
        assertFalse(bounds.intersectsBlock(8.5, 65.62, 8.5, 8, 66, 8));
        assertFalse(bounds.intersectsBlock(8.5, 65.62, 8.5, 10, 64, 8));
    }

    @Test
    void movementRulesRejectNonFiniteOrientation() {
        assertFalse(PlayerMovementRules.isFinite(8.0, 64.0, 8.0, Float.NaN, 0.0f));
        assertFalse(PlayerMovementRules.isFinite(8.0, Double.POSITIVE_INFINITY, 8.0, 0.0f, 0.0f));
        assertTrue(PlayerMovementRules.isFinite(8.0, 64.0, 8.0, 180.0f, -25.0f));
    }

    @Test
    void movementRulesRejectExtremeTeleportDeltas() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();

        assertTrue(PlayerMovementRules.isPlausibleDelta(8.0, 64.0, 8.0, 9.5, 64.2, 8.0, 0.05, config));
        assertFalse(PlayerMovementRules.isPlausibleDelta(8.0, 64.0, 8.0, 80.0, 64.0, 8.0, 0.05, config));
        assertFalse(PlayerMovementRules.isPlausibleDelta(8.0, 64.0, 8.0, 8.0, 180.0, 8.0, 0.05, config));
    }

    @Test
    void movementRulesUseSurvivalSpeedForServerDeltas() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        assertTrue(PlayerMovementRules.isPlausibleSurvivalDelta(8.0, 64.0, 8.0, 9.5, 64.0, 8.0, 0.05, config, dry));
        assertFalse(PlayerMovementRules.isPlausibleSurvivalDelta(8.0, 64.0, 8.0, 11.2, 64.0, 8.0, 0.05, config, dry));
    }

    @Test
    void movementRulesUseSurfaceSpeedForServerDeltas() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        assertTrue(PlayerMovementRules.isPlausibleSurvivalDelta(
                8.0, 64.0, 8.0,
                12.7, 64.0, 8.0,
                0.5,
                config,
                dry,
                BlockSurfacePhysics.DEFAULT,
                BlockSurfacePhysics.DEFAULT
        ));
        assertFalse(PlayerMovementRules.isPlausibleSurvivalDelta(
                8.0, 64.0, 8.0,
                12.7, 64.0, 8.0,
                0.5,
                config,
                dry,
                BlockSurfacePhysics.SNOW,
                BlockSurfacePhysics.SNOW
        ));
    }

    @Test
    void movementRulesApplyModeSpecificSpeedEnvelopes() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        assertFalse(PlayerMovementRules.isPlausibleModeDelta(
                8.0, 64.0, 8.0,
                11.2, 64.0, 8.0,
                0.05,
                config,
                dry,
                PlayerMovementRules.MovementMode.SURVIVAL
        ));
        assertTrue(PlayerMovementRules.isPlausibleModeDelta(
                8.0, 64.0, 8.0,
                11.2, 64.0, 8.0,
                0.05,
                config,
                dry,
                PlayerMovementRules.MovementMode.FLYING
        ));
    }

    @Test
    void movementRulesRejectSuddenHorizontalAccelerationBursts() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();

        assertTrue(PlayerMovementRules.isPlausibleHorizontalAcceleration(
                0.4, 0.0, 0.1,
                0.8, 0.0, 0.1,
                config,
                PlayerMovementRules.MovementMode.SURVIVAL
        ));
        assertFalse(PlayerMovementRules.isPlausibleHorizontalAcceleration(
                0.1, 0.0, 0.1,
                2.0, 0.0, 0.1,
                config,
                PlayerMovementRules.MovementMode.SURVIVAL
        ));
    }

    @Test
    void movementRulesKeepPlayerInsideVerticalWorldBounds() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();

        assertTrue(PlayerMovementRules.withinVerticalBounds(64.0, config, -64, 320));
        assertFalse(PlayerMovementRules.withinVerticalBounds(-80.0, config, -64, 320));
        assertFalse(PlayerMovementRules.withinVerticalBounds(320.0, config, -64, 320));
    }

    @Test
    void movementRulesRejectUnsupportedGroundedClaim() {
        assertTrue(PlayerMovementRules.groundedClaimPlausible(false, false));
        assertTrue(PlayerMovementRules.groundedClaimPlausible(true, true));
        assertFalse(PlayerMovementRules.groundedClaimPlausible(true, false));
    }

    @Test
    void movementRulesRejectOverstatedWaterStateClaim() {
        PlayerWaterState dry = new PlayerWaterState(false, false, false);
        PlayerWaterState feetOnly = new PlayerWaterState(true, false, false);

        assertTrue(PlayerMovementRules.waterStateClaimPlausible(dry, dry));
        assertTrue(PlayerMovementRules.waterStateClaimPlausible(feetOnly, feetOnly));
        assertFalse(PlayerMovementRules.waterStateClaimPlausible(feetOnly, dry));
        assertTrue(PlayerMovementRules.waterStateClaimPlausible(dry, feetOnly));
    }

    @Test
    void movementRulesTreatWaterStateAsMovementAssist() {
        assertFalse(PlayerMovementRules.waterMovementAssist(new PlayerWaterState(false, false, false)));
        assertTrue(PlayerMovementRules.waterMovementAssist(new PlayerWaterState(true, false, false)));
        assertTrue(PlayerMovementRules.waterMovementAssist(new PlayerWaterState(false, true, false)));
        assertTrue(PlayerMovementRules.waterMovementAssist(new PlayerWaterState(false, false, true)));
    }

    @Test
    void movementRulesRequireGroundOrAssistToStartMovingUpward() {
        assertTrue(PlayerMovementRules.upwardMovementPlausible(64.0, 64.02, false, 0.0, false));
        assertTrue(PlayerMovementRules.upwardMovementPlausible(64.0, 64.4, true, 0.0, false));
        assertTrue(PlayerMovementRules.upwardMovementPlausible(64.0, 64.4, false, 0.2, false));
        assertTrue(PlayerMovementRules.upwardMovementPlausible(64.0, 64.4, false, 0.0, true));
        assertFalse(PlayerMovementRules.upwardMovementPlausible(64.0, 64.4, false, 0.0, false));
    }

    @Test
    void survivalStepStartsJumpOnlyWhenGrounded() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerInput jump = new PlayerInput(0.0f, 0.0f, true, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState grounded = new PlayerState(8.0, 65.0, 8.0, 0.0f, 0.0f, 0.0f, true, false, 0.0f);
        PlayerState jumped = PlayerPhysics.stepSurvival(grounded, jump, dry, 0.016f, config, (x, y, z) -> false);

        PlayerState airborne = new PlayerState(8.0, 65.0, 8.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f);
        PlayerState noDoubleJump = PlayerPhysics.stepSurvival(airborne, jump, dry, 0.016f, config, (x, y, z) -> false);

        assertTrue(jumped.velocityY() > 0.0f);
        assertFalse(jumped.onGround());
        assertTrue(noDoubleJump.velocityY() < 0.0f);
    }

    @Test
    void survivalStepAllowsShortCoyoteJumpAfterLeavingGround() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerInput jump = new PlayerInput(0.0f, 0.0f, true, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);
        PlayerState justLeftGround = new PlayerState(8.0, 65.0, 8.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f, 0.08f, 0.0f);

        PlayerState next = PlayerPhysics.stepSurvival(justLeftGround, jump, dry, 0.016f, config, (x, y, z) -> false);

        assertTrue(next.velocityY() > 0.0f);
        assertFalse(next.onGround());
    }

    @Test
    void survivalStepAcceptsSharedPhysicsStepContext() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PhysicsStepContext context = PhysicsStepContext.survival(
                0.05,
                10L,
                DimensionSettings.OVERWORLD,
                new PlayerWaterState(false, false, false)
        );
        PhysicsTestWorld world = new PhysicsTestWorld();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, true, false, 0.0f);

        PlayerState next = PlayerPhysics.stepSurvival(state, new PlayerInput(1.0f, 0.0f, false, false, false), context, config, world.playerCollision(config));

        assertTrue(next.x() > state.x());
        assertTrue(context.containsEyeY(config, next.y()));
    }

    @Test
    void survivalStepBuffersJumpPressedJustBeforeLanding() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerInput jump = new PlayerInput(0.0f, 0.0f, true, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);
        PlayerState falling = new PlayerState(8.0, 64.2, 8.0, 0.0f, -5.0f, 0.0f, false, false, 0.0f);

        PlayerState next = PlayerPhysics.stepSurvival(falling, jump, dry, 0.1f, config, (x, y, z) -> y <= 64.0);

        assertTrue(next.velocityY() > 0.0f);
        assertFalse(next.onGround());
        assertEquals(0.0f, next.fallImpactSpeed(), 0.001f);
    }

    @Test
    void survivalStepUsesLandingSurfaceForBufferedJump() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerInput jump = new PlayerInput(0.0f, 0.0f, true, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);
        PlayerState falling = new PlayerState(8.0, 64.2, 8.0, 0.0f, -5.0f, 0.0f, false, false, 0.0f);

        PlayerState next = PlayerPhysics.stepSurvival(
                falling,
                jump,
                dry,
                0.1f,
                config,
                (x, y, z) -> y <= 64.0,
                (x, y, z) -> BlockSurfacePhysics.SNOW
        );

        assertEquals(config.jumpSpeed() * BlockSurfacePhysics.SNOW.jumpMultiplier(), next.velocityY(), 0.001f);
        assertFalse(next.onGround());
    }

    @Test
    void survivalStepSlidesAlongBlockedHorizontalAxis() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, true, false, 0.0f);
        PlayerInput diagonal = new PlayerInput(1.0f, 1.0f, false, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState next = PlayerPhysics.stepSurvival(state, diagonal, dry, 0.1f, config, (x, y, z) -> x > 0.05);

        assertEquals(0.0, next.x(), 0.001);
        assertTrue(next.z() > 0.1);
    }

    @Test
    void survivalStepLimitsAirControlInsteadOfSnappingToFullSpeed() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState airborne = new PlayerState(0.0, 65.0, 0.0, 0.0f, -1.0f, 0.0f, false, false, 0.0f);
        PlayerInput strafe = new PlayerInput(1.0f, 0.0f, false, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState next = PlayerPhysics.stepSurvival(airborne, strafe, dry, 0.1f, config, (x, y, z) -> false);

        assertTrue(next.velocityX() > 0.0f);
        assertTrue(next.velocityX() < config.walkSpeed());
    }

    @Test
    void survivalStepAppliesGroundFrictionWhenInputStops() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState sliding = new PlayerState(0.0, 65.0, 0.0, config.walkSpeed(), 0.0f, 0.0f, true, false, 0.0f);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState next = PlayerPhysics.stepSurvival(sliding, PlayerInput.idle(), dry, 0.016f, config, (x, y, z) -> false);

        assertTrue(next.velocityX() > 0.0f);
        assertTrue(next.velocityX() < config.walkSpeed());
    }

    @Test
    void survivalStepUsesSurfaceFrictionForIceSliding() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState sliding = new PlayerState(0.0, 65.0, 0.0, config.walkSpeed(), 0.0f, 0.0f, true, false, 0.0f);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState normal = PlayerPhysics.stepSurvival(
                sliding,
                PlayerInput.idle(),
                dry,
                0.016f,
                config,
                (x, y, z) -> false,
                (x, y, z) -> BlockSurfacePhysics.DEFAULT
        );
        PlayerState ice = PlayerPhysics.stepSurvival(
                sliding,
                PlayerInput.idle(),
                dry,
                0.016f,
                config,
                (x, y, z) -> false,
                (x, y, z) -> BlockSurfacePhysics.ICE
        );

        assertTrue(ice.velocityX() > normal.velocityX());
    }

    @Test
    void survivalStepUsesSurfaceSpeedForSnowAndPaths() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, true, false, 0.0f);
        PlayerInput forward = new PlayerInput(1.0f, 0.0f, false, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState snow = PlayerPhysics.stepSurvival(state, forward, dry, 0.1f, config, (x, y, z) -> false, (x, y, z) -> BlockSurfacePhysics.SNOW);
        PlayerState path = PlayerPhysics.stepSurvival(state, forward, dry, 0.1f, config, (x, y, z) -> false, (x, y, z) -> BlockSurfacePhysics.PATH);

        assertTrue(path.velocityX() > snow.velocityX());
    }

    @Test
    void survivalStepStopsAtBlockedCorner() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, true, false, 0.0f);
        PlayerInput diagonal = new PlayerInput(1.0f, 1.0f, false, false, false);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState next = PlayerPhysics.stepSurvival(state, diagonal, dry, 0.1f, config, (x, y, z) -> x > 0.05 || z > 0.05);

        assertEquals(0.0, next.x(), 0.001);
        assertEquals(0.0, next.z(), 0.001);
    }

    @Test
    void survivalStepReportsFallImpactWhenLanding() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState falling = new PlayerState(8.0, 65.2, 8.0, 0.0f, -30.0f, 0.0f, false, false, 0.0f);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState landed = PlayerPhysics.stepSurvival(falling, PlayerInput.idle(), dry, 0.1f, config, (x, y, z) -> y <= 64.0);

        assertTrue(landed.onGround());
        assertEquals(0.0f, landed.velocityY(), 0.001f);
        assertTrue(landed.fallImpactSpeed() > 15.0f);
    }

    @Test
    void survivalStepSuppressesFallImpactWhenEnteringWater() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState falling = new PlayerState(8.0, 65.2, 8.0, 0.0f, -30.0f, 0.0f, false, false, 0.0f);
        PlayerWaterState bodyInWater = new PlayerWaterState(true, true, false);

        PlayerState landed = PlayerPhysics.stepSurvival(falling, PlayerInput.idle(), bodyInWater, 0.1f, config, (x, y, z) -> y <= 64.0);

        assertEquals(0.0f, landed.fallImpactSpeed(), 0.001f);
        assertTrue(landed.velocityY() > -30.0f);
        assertFalse(landed.underwater());
    }

    @Test
    void survivalStepZerosVerticalVelocityOnHeadBump() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState rising = new PlayerState(8.0, 65.0, 8.0, 0.0f, 8.0f, 0.0f, false, false, 0.0f);
        PlayerWaterState dry = new PlayerWaterState(false, false, false);

        PlayerState bumped = PlayerPhysics.stepSurvival(rising, PlayerInput.idle(), dry, 0.1f, config, (x, y, z) -> y > 65.2);

        assertFalse(bumped.onGround());
        assertEquals(0.0f, bumped.velocityY(), 0.001f);
        assertEquals(0.0f, bumped.fallImpactSpeed(), 0.001f);
    }

    @Test
    void survivalStepUsesWaterMovementFlagsForSwimming() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(8.0, 65.0, 8.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f);
        PlayerInput swimUp = new PlayerInput(0.0f, 0.0f, true, false, true);
        PlayerWaterState bodyInWater = new PlayerWaterState(false, true, false);

        PlayerState next = PlayerPhysics.stepSurvival(state, swimUp, bodyInWater, 0.1f, config, (x, y, z) -> false);

        assertFalse(next.underwater());
        assertTrue(next.velocityY() > 0.0f);
        assertEquals(0.0f, next.velocityX(), 0.001f);
    }

    @Test
    void survivalStepAppliesWaterHorizontalDrag() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState drifting = new PlayerState(8.0, 65.0, 8.0, config.walkSpeed(), 0.0f, 0.0f, false, false, 0.0f);
        PlayerWaterState bodyInWater = new PlayerWaterState(false, true, false);

        PlayerState next = PlayerPhysics.stepSurvival(drifting, PlayerInput.idle(), bodyInWater, 0.1f, config, (x, y, z) -> false);

        assertTrue(next.velocityX() > 0.0f);
        assertTrue(next.velocityX() < config.walkSpeed());
    }

    @Test
    void flyingStepNormalizesThreeDimensionalMovement() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f);

        PlayerState next = PlayerPhysics.stepFlying(state, 1.0f, 1.0f, 0.0f, false, 0.1f, config, (x, y, z) -> false);

        double expected = config.flySpeed() * 0.1 / Math.sqrt(2.0);
        assertEquals(expected, next.x(), 0.001);
        assertEquals(65.0 + expected, next.y(), 0.001);
        assertEquals(0.0, next.z(), 0.001);
        assertEquals(0.0f, next.velocityY(), 0.001f);
    }

    @Test
    void flyingStepUsesCollisionSubsteps() {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(0.0, 65.0, 0.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f);

        PlayerState next = PlayerPhysics.stepFlying(state, 1.0f, 0.0f, 0.0f, true, 0.1f, config, (x, y, z) -> x > 0.2);

        assertEquals(0.0, next.x(), 0.001);
        assertEquals(65.0, next.y(), 0.001);
        assertEquals(0.0, next.z(), 0.001);
    }
}
