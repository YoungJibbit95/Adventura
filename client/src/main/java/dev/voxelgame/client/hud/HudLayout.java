package dev.voxelgame.client.hud;

import dev.voxelgame.client.GameSettings;
import dev.voxelgame.client.Hotbar;

public record HudLayout(
        float scale,
        float margin,
        float hotbarX,
        float hotbarY,
        float hotbarSlotSize,
        float hotbarGap,
        float hotbarWidth,
        float statsY,
        float statWidth,
        float statIconSize,
        float selectedTooltipY,
        float selectedTooltipScale,
        float comfortY,
        float timeY,
        float feedbackBottomY,
        float interactionBottomLimitY,
        boolean showSelectedTooltip,
        boolean showTime,
        boolean showWorldInfo,
        boolean showMode,
        boolean showComfort
) {
    public float hotbarBottom() {
        return hotbarY + hotbarSlotSize;
    }

    public static HudLayout forViewport(int framebufferWidth, int framebufferHeight, float uiScale) {
        return forViewport(framebufferWidth, framebufferHeight, uiScale, GameSettings.HudMode.NORMAL);
    }

    public static HudLayout forViewport(
            int framebufferWidth,
            int framebufferHeight,
            float uiScale,
            GameSettings.HudMode hudMode
    ) {
        GameSettings.HudMode safeHudMode = hudMode == null ? GameSettings.HudMode.NORMAL : hudMode;
        float safeUiScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        boolean minimalHud = safeHudMode == GameSettings.HudMode.MINIMAL || safeHudMode == GameSettings.HudMode.HIDDEN;
        float margin = Math.max(10.0f, Math.min(20.0f * safeUiScale, framebufferWidth * 0.055f));
        float gap = Math.max(3.0f, Math.min(6.0f * safeUiScale, 8.0f));
        float desiredSlot = 58.0f * safeUiScale;
        float maxSlotByWidth = (framebufferWidth - margin * 2.0f - gap * (Hotbar.HOTBAR_SLOTS - 1)) / Hotbar.HOTBAR_SLOTS;
        float maxSlotByHeight = Math.max(32.0f, framebufferHeight * 0.14f);
        float hotbarSlotSize = Math.max(34.0f, Math.min(desiredSlot, Math.min(maxSlotByWidth, maxSlotByHeight)));
        float scale = Math.max(0.62f, Math.min(safeUiScale, hotbarSlotSize / 58.0f));
        gap = Math.max(3.0f, 6.0f * scale);
        float hotbarWidth = Hotbar.HOTBAR_SLOTS * hotbarSlotSize + (Hotbar.HOTBAR_SLOTS - 1) * gap;
        float hotbarX = clamp(framebufferWidth * 0.5f - hotbarWidth * 0.5f, margin, framebufferWidth - hotbarWidth - margin);
        float hotbarY = Math.min(framebufferHeight - hotbarSlotSize - 14.0f * scale, framebufferHeight - 86.0f * scale);
        hotbarY = Math.max(framebufferHeight * 0.62f, hotbarY);
        float statsY = hotbarY - 44.0f * scale;
        float statWidth = Math.min(188.0f * scale, hotbarWidth * 0.31f);
        float statIconSize = Math.max(6.0f, 11.0f * scale);
        boolean showSelectedTooltip = !minimalHud && statsY - 42.0f * scale > 58.0f * scale && framebufferWidth >= 460;
        boolean showTime = !minimalHud && statsY - 18.0f * scale > 46.0f * scale && framebufferWidth >= 420;
        boolean showWorldInfo = !minimalHud && framebufferWidth >= 520 && framebufferHeight >= 420;
        boolean showMode = !minimalHud && framebufferWidth >= 720;
        boolean showComfort = !minimalHud && framebufferHeight >= 410 && framebufferWidth >= 430;
        float selectedTooltipY = statsY - 42.0f * scale;
        float feedbackBottomY = showSelectedTooltip ? selectedTooltipY - 14.0f * scale : statsY - 38.0f * scale;
        feedbackBottomY = Math.max(86.0f * scale, feedbackBottomY);
        float interactionBottomLimitY = showSelectedTooltip ? selectedTooltipY - 16.0f * scale : statsY - 42.0f * scale;
        interactionBottomLimitY = Math.max(framebufferHeight * 0.5f + 64.0f * scale, interactionBottomLimitY);
        return new HudLayout(
                scale,
                margin,
                hotbarX,
                hotbarY,
                hotbarSlotSize,
                gap,
                hotbarWidth,
                statsY,
                statWidth,
                statIconSize,
                selectedTooltipY,
                1.45f * scale,
                statsY + 22.0f * scale,
                statsY - 18.0f * scale,
                feedbackBottomY,
                interactionBottomLimitY,
                showSelectedTooltip,
                showTime,
                showWorldInfo,
                showMode,
                showComfort
        );
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
