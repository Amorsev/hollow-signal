package com.hollowsignal.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared old-era drawing helpers. */
public final class OldStyle {
    private OldStyle() {}

    private static final ResourceLocation DIRT = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");

    /** Dark tiled dirt, like menus in the Alpha/Beta days. */
    public static void dirt(GuiGraphics g, int width, int height, float brightness) {
        g.setColor(brightness, brightness, brightness, 1f);
        for (int x = 0; x < width; x += 32) {
            for (int y = 0; y < height; y += 32) {
                g.blit(DIRT, x, y, 32, 32, 0, 0, 16, 16, 16, 16);
            }
        }
        g.setColor(1f, 1f, 1f, 1f);
    }
}
