package dev.voxelgame.client.hud;

import dev.voxelgame.client.ui.BitmapFont;

public record InteractionStatusCard(
        String title,
        String detail,
        String extra,
        float width,
        float height,
        float titleScale,
        float detailScale,
        float extraScale,
        float progress,
        boolean progressVisible
) {
    public InteractionStatusCard {
        title = title == null ? "" : title;
        detail = detail == null ? "" : detail;
        extra = extra == null ? "" : extra;
        width = Math.max(1.0f, width);
        height = Math.max(1.0f, height);
        titleScale = Math.max(0.1f, titleScale);
        detailScale = Math.max(0.1f, detailScale);
        extraScale = Math.max(0.1f, extraScale);
        progress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    public static InteractionStatusCard from(
            String title,
            String detail,
            String extra,
            float uiScale,
            int framebufferWidth,
            float progress
    ) {
        float safeScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        float titleScale = 1.08f * safeScale;
        float detailScale = 0.96f * safeScale;
        float extraScale = 0.96f * safeScale;
        float maxWidth = Math.min(382.0f * safeScale, Math.max(180.0f * safeScale, framebufferWidth - 34.0f * safeScale));
        String fittedTitle = fit(title, titleScale, maxWidth - 24.0f * safeScale);
        String fittedDetail = fit(detail, detailScale, maxWidth - 24.0f * safeScale);
        String fittedExtra = fit(extra, extraScale, maxWidth - 24.0f * safeScale);
        float textWidth = Math.max(BitmapFont.textWidth(fittedTitle, titleScale), BitmapFont.textWidth(fittedDetail, detailScale));
        if (!fittedExtra.isBlank()) {
            textWidth = Math.max(textWidth, BitmapFont.textWidth(fittedExtra, extraScale));
        }
        float width = Math.min(maxWidth, Math.max(190.0f * safeScale, textWidth + 24.0f * safeScale));
        boolean progressVisible = progress > 0.0f;
        float height = fittedExtra.isBlank() ? 48.0f * safeScale : 66.0f * safeScale;
        if (progressVisible) {
            height += 12.0f * safeScale;
        }
        return new InteractionStatusCard(
                fittedTitle,
                fittedDetail,
                fittedExtra,
                width,
                height,
                titleScale,
                detailScale,
                extraScale,
                progress,
                progressVisible
        );
    }

    private static String fit(String text, float scale, float maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0.0f) {
            return "";
        }
        if (BitmapFont.textWidth(text, scale) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        if (BitmapFont.textWidth(suffix, scale) > maxWidth) {
            return "";
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            String candidate = text.substring(0, mid) + suffix;
            if (BitmapFont.textWidth(candidate, scale) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low).stripTrailing() + suffix;
    }
}
