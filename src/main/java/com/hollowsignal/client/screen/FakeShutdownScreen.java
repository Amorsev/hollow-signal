package com.hollowsignal.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * SAFE stand-in for a PC shutdown: a Windows 10-style "Shutting down" screen
 * (blue-black background, spinning dots), then black — and the game comes back.
 */
public class FakeShutdownScreen extends Screen {
    private final int total;
    private int age;

    public FakeShutdownScreen(int ticks) {
        super(Component.translatable("hollowsignal.fourthwall.shutting_down"));
        this.total = Math.max(60, ticks);
    }

    @Override
    public void tick() {
        if (++age >= total) onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean black = age > total - 30;
        g.fill(0, 0, width, height, black ? 0xFF000000 : 0xFF0A1A3A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (age > total - 30) return;
        int cx = width / 2, cy = height / 2;
        // Windows-style spinner: 5 dots chasing around a circle
        float t = (age + partialTick) / 20f;
        for (int i = 0; i < 5; i++) {
            float a = t * 5.0f - i * 0.35f;
            int x = cx + (int) (Mth.cos(a) * 10), y = cy - 12 + (int) (Mth.sin(a) * 10);
            g.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
        }
        g.drawCenteredString(font, title, cx, cy + 8, 0xFFFFFF);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
