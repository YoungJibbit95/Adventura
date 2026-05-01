package dev.voxelgame.client.hud;

import dev.voxelgame.client.InteractionHint;
import dev.voxelgame.client.ui.BitmapFont;

import java.util.List;
import java.util.Locale;

public record InteractionHudCard(
        String title,
        String action,
        String detail,
        List<String> chips,
        InteractionHint.Tone tone,
        float progress,
        float width,
        float height,
        float titleScale,
        float actionScale,
        float detailScale,
        boolean progressVisible
) {
    public InteractionHudCard {
        title = title == null ? "" : title;
        action = action == null ? "" : action;
        detail = detail == null ? "" : detail;
        chips = chips == null ? List.of() : List.copyOf(chips);
        tone = tone == null ? InteractionHint.Tone.NEUTRAL : tone;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        width = Math.max(1.0f, width);
        height = Math.max(1.0f, height);
        titleScale = Math.max(0.1f, titleScale);
        actionScale = Math.max(0.1f, actionScale);
        detailScale = Math.max(0.1f, detailScale);
    }

    public static InteractionHudCard from(InteractionHint hint, float uiScale, int framebufferWidth) {
        float safeScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        float titleScale = 0.86f * safeScale;
        float actionScale = 1.08f * safeScale;
        float detailScale = 0.78f * safeScale;
        float maxWidth = Math.min(392.0f * safeScale, Math.max(180.0f * safeScale, framebufferWidth - 32.0f * safeScale));
        String title = fit(hint.title().toUpperCase(Locale.ROOT), titleScale, maxWidth - 34.0f * safeScale);
        String action = fit(hint.action(), actionScale, maxWidth - 34.0f * safeScale);
        String detail = fit(hint.detail(), detailScale, maxWidth - 34.0f * safeScale);
        List<String> chips = hint.chips().stream()
                .limit(3)
                .map(chip -> fit(chip.toUpperCase(Locale.ROOT), 0.62f * safeScale, 92.0f * safeScale))
                .filter(chip -> !chip.isBlank())
                .toList();
        float textWidth = Math.max(BitmapFont.textWidth(title, titleScale), BitmapFont.textWidth(action, actionScale));
        if (!detail.isBlank()) {
            textWidth = Math.max(textWidth, BitmapFont.textWidth(detail, detailScale));
        }
        float chipItemsWidth = (float) chips.stream()
                .mapToDouble(chip -> BitmapFont.textWidth(chip, 0.62f * safeScale) + 14.0f * safeScale)
                .sum();
        float chipsWidth = chipItemsWidth > 0.0f ? chipItemsWidth + Math.max(0, chips.size() - 1) * 5.0f * safeScale : 0.0f;
        float width = Math.min(maxWidth, Math.max(210.0f * safeScale, Math.max(textWidth, chipsWidth) + 34.0f * safeScale));
        boolean progressVisible = hint.progress() > 0.0f;
        float height = detail.isBlank() ? 42.0f * safeScale : 58.0f * safeScale;
        if (progressVisible) {
            height += 12.0f * safeScale;
        }
        if (!chips.isEmpty()) {
            height += 18.0f * safeScale;
        }
        return new InteractionHudCard(
                title,
                action,
                detail,
                chips,
                hint.tone(),
                hint.progress(),
                width,
                height,
                titleScale,
                actionScale,
                detailScale,
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
