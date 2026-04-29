package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.math.Raycast;

public final class BlockBreakAnimation {
    private static final double HAND_SWING_SECONDS = 0.34;

    private Target target;
    private double startedAt;
    private double durationSeconds = 0.12;
    private double handSwingUntil;

    public void startOrContinue(Raycast.Hit hit, BlockType block, double durationSeconds, double nowSeconds) {
        double safeDuration = Math.max(0.08, durationSeconds);
        if (target == null || !target.matches(hit, block.id())) {
            target = new Target(hit.x(), hit.y(), hit.z(), block.id());
            startedAt = nowSeconds;
            this.durationSeconds = safeDuration;
            handSwingUntil = nowSeconds + HAND_SWING_SECONDS;
            return;
        }
        this.durationSeconds = safeDuration;
        if (nowSeconds > handSwingUntil) {
            handSwingUntil = nowSeconds + HAND_SWING_SECONDS;
        }
    }

    public boolean active() {
        return target != null;
    }

    public boolean complete(double nowSeconds) {
        return progress(nowSeconds) >= 1.0f;
    }

    public float progress(double nowSeconds) {
        if (target == null) {
            return 0.0f;
        }
        return (float) Math.max(0.0, Math.min(1.0, (nowSeconds - startedAt) / durationSeconds));
    }

    public float handSwing(double nowSeconds) {
        double remaining = handSwingUntil - nowSeconds;
        if (remaining <= 0.0) {
            return 0.0f;
        }
        return (float) Math.max(0.0, Math.min(1.0, remaining / HAND_SWING_SECONDS));
    }

    public void hit(double nowSeconds) {
        handSwingUntil = nowSeconds + HAND_SWING_SECONDS;
    }

    public void clear() {
        target = null;
        startedAt = 0.0;
        durationSeconds = 0.12;
    }

    private record Target(int x, int y, int z, short blockId) {
        private boolean matches(Raycast.Hit hit, short otherBlockId) {
            return x == hit.x() && y == hit.y() && z == hit.z() && blockId == otherBlockId;
        }
    }
}
