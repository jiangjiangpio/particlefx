package dev.particlefx.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** 1.19.2 has no DrawContext; these helpers wrap the MatrixStack-era TextRenderer calls. */
final class GuiRender {
    static void centered(TextRenderer tr, MatrixStack matrices, Text text, int centerX, int y, int color) {
        tr.drawWithShadow(matrices, text, centerX - tr.getWidth(text) / 2f, y, color);
    }
    static void centered(TextRenderer tr, MatrixStack matrices, String text, int centerX, int y, int color) {
        tr.drawWithShadow(matrices, text, centerX - tr.getWidth(text) / 2f, y, color);
    }
    static void left(TextRenderer tr, MatrixStack matrices, Text text, int x, int y, int color) {
        tr.drawWithShadow(matrices, text, x, y, color);
    }
    static void left(TextRenderer tr, MatrixStack matrices, String text, int x, int y, int color) {
        tr.drawWithShadow(matrices, text, x, y, color);
    }
    private GuiRender(){}
}
