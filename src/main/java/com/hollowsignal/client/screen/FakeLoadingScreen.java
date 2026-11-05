package com.hollowsignal.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * An old-style "Loading world / Building terrain" screen that appears in the middle of play.
 * The game is NOT paused behind it — you just can't see what's happening.
 */
public class FakeLoadingScreen extends Screen {
    private final int total;
    private int left;
    private final int variant;

    public FakeLoadingScreen(int ticks, int variant) {
        super(Component.translatable("hollowsignal.loading.title"));
        this.total = Math.max(20, ticks);
        this.left = this.total;
        this.variant = variant;
    }

    @Override
    public void tick() {
        if (--left <= 0) onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        OldStyle.dirt(g, width, height, 0.25f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cy = height / 2;
        g.drawCenteredString(font, title, width / 2, cy - 30, 0xFFFFFF);
        // Halfway through, the subtitle changes to something it shouldn't say.
        boolean wrong = left < total / 2;
        String sub = wrong ? "hollowsignal.loading.wrong." + (variant % 3) : "hollowsignal.loading.sub";
        g.drawCenteredString(font, Component.translatable(sub), width / 2, cy + 6, 0xFFFFFF);

        // Classic thin progress bar: grey track, green fill — it stalls near the end.
        float progress = Math.min(0.93f, (total - left + partialTick) / (float) total * 1.4f);
        int bw = 100, x = width / 2 - bw / 2, y = cy + 20;
        g.fill(x, y, x + bw, y + 2, 0xFF808080);
        g.fill(x, y, x + (int) (bw * progress), y + 2, 0xFF80FF80);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
