package dev.voxelgame.client;

import dev.voxelgame.client.animation.Easing;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.math.Raycast;

public final class BlockBreakAnimation {
    private Target target;
    private double startedAt;
    private double durationSeconds = 0.12;

    public void startOrContinue(Raycast.Hit hit, BlockType block, double durationSeconds, double nowSeconds) {
        double safeDuration = Math.max(0.08, durationSeconds);
        if (target == null || !target.matches(hit, block.id())) {
            target = new Target(hit.x(), hit.y(), hit.z(), block.id());
            startedAt = nowSeconds;
            this.durationSeconds = safeDuration;
            return;
        }
        this.durationSeconds = safeDuration;
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
        return Easing.linear((nowSeconds - startedAt) / durationSeconds);
    }

    public float easedProgress(double nowSeconds) {
        return Easing.smoothStep(progress(nowSeconds));
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
