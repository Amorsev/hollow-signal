package com.hollowsignal.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Ending text on black: lines fade in one after another, then everything fades out.
 * variant 0 = released (warm white), 1 = loop (grey).
 */
public class EndingScreen extends Screen {
    private static final int LINES = 6;
    private final int total;
    private final int variant;
    private int age;

    public EndingScreen(int ticks, int variant) {
        super(Component.empty());
        this.total = Math.max(100, ticks);
        this.variant = variant;
    }

    @Override
    public void tick() {
        if (++age >= total) onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        int alpha = (int) (255 * Mth.clamp(t / 30f, 0f, 1f));
        g.fill(0, 0, width, height, alpha << 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float t = age + partialTick;
        float perLine = (total - 60) / (float) LINES;
        float fadeOut = Mth.clamp((total - t) / 30f, 0f, 1f);
        int baseColor = variant == 0 ? 0xFFF4E8 : 0xB0B0B0;
        String type = variant == 0 ? "released" : "loop";

        int y = height / 2 - LINES * 7;
        for (int i = 0; i < LINES; i++) {
            float lt = t - 30 - i * perLine;
            if (lt <= 0) break;
            float a = Mth.clamp(lt / 25f, 0f, 1f) * fadeOut;
            if (a > 0.02f) {
                List<FormattedCharSequence> lines = font.split(Component.translatable("hollowsignal.ending." + type + "." + i), width - 60);
                for (FormattedCharSequence l : lines) {
                    g.drawCenteredString(font, l, width / 2, y, ((int) (a * 255) << 24) | baseColor);
                    y += 11;
                }
            } else {
                y += 11;
            }
            y += 4;
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
