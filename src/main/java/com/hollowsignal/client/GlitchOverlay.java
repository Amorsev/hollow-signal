package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Full-screen "lost signal" distortion drawn above the HUD:
 * TV static, horizontal tearing bands, red/cyan channel split, scanlines and darkening.
 */
public final class GlitchOverlay {
    private GlitchOverlay() {}

    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "glitch");
    private static final ResourceLocation NOISE_ID = ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "dynamic/glitch_noise");
    private static final int NW = 160, NH = 90;
    private static final RandomSource RANDOM = RandomSource.create();

    private static DynamicTexture noise;
    private static int frame;

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, GlitchOverlay::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        float i = ClientEffects.glitchIntensity(delta.getGameTimeDeltaPartialTick(false));
        if (i <= 0.01f) return;
        int w = g.guiWidth(), h = g.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // The world must stay visible: this only gets in the way, it never covers the screen.

        // 1) faint static veil over everything (regenerated every other frame)
        if (noise == null) {
            noise = new DynamicTexture(NW, NH, false);
            Minecraft.getInstance().getTextureManager().register(NOISE_ID, noise);
        }
        if ((frame++ & 1) == 0) regenerateNoise();
        g.setColor(1f, 1f, 1f, 0.10f * i);
        g.blit(NOISE_ID, 0, 0, w, h, 0, 0, NW, NH, NW, NH);

        // 2) a few denser static patches that jump around
        int patches = 1 + (int) (3 * i);
        g.setColor(1f, 1f, 1f, 0.28f * i);
        for (int p = 0; p < patches; p++) {
            int pw = w / 6 + RANDOM.nextInt(w / 4);
            int ph = 6 + RANDOM.nextInt(Math.max(7, h / 8));
            int px = RANDOM.nextInt(Math.max(1, w - pw));
            int py = RANDOM.nextInt(Math.max(1, h - ph));
            g.blit(NOISE_ID, px, py, pw, ph, RANDOM.nextInt(NW / 2), RANDOM.nextInt(NH / 2), NW / 2, NH / 4, NW, NH);
        }
        g.setColor(1f, 1f, 1f, 1f);

        // 3) thin horizontal tears with a light red/cyan split
        int bands = 1 + (int) (4 * i);
        for (int b = 0; b < bands; b++) {
            int y = RANDOM.nextInt(h);
            int bh = 1 + RANDOM.nextInt(Math.max(2, (int) (5 * i)));
            int shift = (int) ((RANDOM.nextFloat() - 0.5f) * 24 * i);
            g.fill(0, y, w, y + bh, argb((int) (45 * i), 10, 10, 12));
            g.fill(shift - 2, y, w + shift - 2, y + bh, argb((int) (28 * i), 255, 20, 30));
            g.fill(shift + 2, y, w + shift + 2, y + bh, argb((int) (28 * i), 20, 230, 255));
        }

        // 4) very light scanlines
        int scan = argb((int) (14 * i), 0, 0, 0);
        for (int y = frame % 3; y < h; y += 3) g.fill(0, y, w, y + 1, scan);

        RenderSystem.disableBlend();
    }

    private static void regenerateNoise() {
        NativeImage img = noise.getPixels();
        if (img == null) return;
        for (int y = 0; y < NH; y++) {
            boolean bright = RANDOM.nextInt(12) == 0; // occasional bright rolling line
            for (int x = 0; x < NW; x++) {
                int v = RANDOM.nextInt(256);
                if (bright) v = Math.min(255, v + 90);
                int a = 140 + RANDOM.nextInt(116);
                img.setPixelRGBA(x, y, (a << 24) | (v << 16) | (v << 8) | v); // ABGR; grey so order doesn't matter
            }
        }
        noise.upload();
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b;
    }
}
