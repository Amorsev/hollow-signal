package com.hollowsignal.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Looks like "Connection Lost". The button says "Back to Title Screen"… but you're still in the world.
 * The world keeps running underneath.
 */
public class FakeDisconnectScreen extends Screen {
    private final Component reason;

    public FakeDisconnectScreen(int variant) {
        super(Component.translatable("disconnect.lost"));
        this.reason = Component.translatable("hollowsignal.disconnect." + (variant % 4)).withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.toTitle"), b -> onClose())
                .bounds(width / 2 - 100, height / 2 + 30, 200, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        OldStyle.dirt(g, width, height, 0.25f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 30, 0xAAAAAA);
        g.drawCenteredString(font, reason, width / 2, height / 2 - 10, 0xFFFFFF);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
