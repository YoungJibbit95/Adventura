package dev.voxelgame.client.ui;

public record UiButton(float x, float y, float width, float height, String label, boolean enabled) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
