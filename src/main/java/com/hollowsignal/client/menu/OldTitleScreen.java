package com.hollowsignal.client.menu;

import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.gui.ModListScreen;

/**
 * Title screen in the spirit of the Alpha/Beta era: dark tiled dirt, a logo made of stone blocks
 * that tumble into place, a bouncing yellow splash, grey version text in the corner and a plain
 * vertical button column. Everything is drawn from the game's current textures — with the
 * Programmer Art pack enabled they're the classic ones.
 */
public class OldTitleScreen extends Screen {
    private static final ResourceLocation DIRT = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    private static final ResourceLocation STONE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    private static final int SPLASH_COUNT = 16;

    /** Our own 5-row block font for the logo. */
    private static final String[][] LETTERS = {
            {"#...#", "##.##", "#.#.#", "#...#", "#...#"}, // M
            {"###", ".#.", ".#.", ".#.", "###"},           // I
            {"#..#", "##.#", "#.##", "#..#", "#..#"},      // N
            {"###", "#..", "##.", "#..", "###"},           // E
            {".##", "#..", "#..", "#..", ".##"},           // C
            {"##.", "#.#", "##.", "#.#", "#.#"},           // R
            {".#.", "#.#", "###", "#.#", "#.#"},           // A
            {"###", "#..", "##.", "#..", "#.."},           // F
            {"###", ".#.", ".#.", ".#.", ".#."},           // T
    };

    /** Fake version strings, intentionally a little broken. */
    private static final String[] VERSIONS = {
            "Minecraft still watch for you",
            "Minecraft version not find",
            "Minecraft ?.?.?",
            "Minecraft Alpha v0.0.0",
            "Minecraft is listening",
            "Minecraft build null",
            "Minecraft signal lost",
            "Minecraft you are not alone",
            "Minecraft version: unknown (do not look)",
            "Minecraft 1.21.1 1.21.1 1.21.1",
            "Minecraft player count: 2",
            "Minecraft can hear you",
    };

    private static boolean logoAnimationPlayed;

    private String version = VERSIONS[0];
    private String flickerVersion = VERSIONS[0];
    private long nextVersionSwap;
    private long flickerUntil;

    private final RandomSource random = RandomSource.create();
    private final int splash;
    private final boolean[][] logo;
    private final float[][] fallDelay;
    private long openedAt;
    private final boolean animateLogo;
    /** Rare: one block of the logo is simply missing. */
    private final int missingBlock;

    public OldTitleScreen() {
        super(Component.translatable("narrator.screen.title"));
        this.splash = random.nextInt(SPLASH_COUNT);
        this.logo = buildLogo();
        this.fallDelay = new float[logo.length][logo[0].length];
        for (float[] row : fallDelay) for (int i = 0; i < row.length; i++) row[i] = random.nextFloat() * 0.9f;
        this.animateLogo = !logoAnimationPlayed;
        logoAnimationPlayed = true;
        this.missingBlock = random.nextInt(25) == 0 ? random.nextInt(countBlocks()) : -1;
        this.version = VERSIONS[random.nextInt(VERSIONS.length)];
    }

    private String pickVersion(String not) {
        String v;
        do v = VERSIONS[random.nextInt(VERSIONS.length)]; while (v.equals(not));
        return v;
    }

    private static boolean[][] buildLogo() {
        int cols = -1;
        for (String[] l : LETTERS) cols += l[0].length() + 1;
        boolean[][] grid = new boolean[5][cols];
        int x = 0;
        for (String[] l : LETTERS) {
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < l[r].length(); c++) grid[r][x + c] = l[r].charAt(c) == '#';
            }
            x += l[0].length() + 1;
        }
        return grid;
    }

    private int countBlocks() {
        int n = 0;
        for (boolean[] row : logo) for (boolean b : row) if (b) n++;
        return n;
    }

    @Override
    protected void init() {
        openedAt = Util.getMillis();
        int cx = width / 2;
        int y = height / 4 + 48;

        addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"),
                b -> minecraft.setScreen(new SelectWorldScreen(this))).bounds(cx - 100, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"),
                b -> minecraft.setScreen(new JoinMultiplayerScreen(this))).bounds(cx - 100, y + 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("fml.menu.mods"),
                b -> minecraft.setScreen(new ModListScreen(this))).bounds(cx - 100, y + 48, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("options.resourcepack"),
                b -> minecraft.setScreen(new PackSelectionScreen(minecraft.getResourcePackRepository(), repo -> {
                    minecraft.options.updateResourcePacks(repo);
                    minecraft.setScreen(this);
                }, minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title"))))
                .bounds(cx + 2, y + 48, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.options"),
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))).bounds(cx - 100, y + 84, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.quit"),
                b -> minecraft.stop()).bounds(cx + 2, y + 84, 98, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Classic dark tiled dirt instead of the panorama + blur.
        g.setColor(0.25f, 0.25f, 0.25f, 1f);
        for (int x = 0; x < width; x += 32) {
            for (int y = 0; y < height; y += 32) {
                g.blit(DIRT, x, y, 32, 32, 0, 0, 16, 16, 16, 16);
            }
        }
        g.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // background + buttons
        float t = (Util.getMillis() - openedAt) / 1000f;

        int logoBottom = renderLogo(g, t);
        renderSplash(g, logoBottom);

        // Grey "version" text, top-left. It is never the real version, and now and then it changes by itself.
        long now = Util.getMillis();
        if (now >= nextVersionSwap) {
            if (flickerUntil > 0) {
                flickerUntil = 0;
            } else if (random.nextInt(4) == 0) {
                flickerVersion = pickVersion(version);
                flickerUntil = now + 120 + random.nextInt(180);
            }
            nextVersionSwap = flickerUntil > 0 ? flickerUntil : now + 2000 + random.nextInt(4000);
        }
        g.drawString(font, flickerUntil > 0 ? flickerVersion : version, 2, 2, 0x505050, false);

        Component copyright = Component.literal("Copyright Mojang AB. Do not distribute!");
        g.drawString(font, copyright, width - font.width(copyright) - 2, height - 10, 0xFFFFFF, true);
    }

    /** @return y of the logo's bottom edge */
    private int renderLogo(GuiGraphics g, float t) {
        int cols = logo[0].length;
        int size = Mth.clamp((width - 40) / cols, 4, 9);
        int left = (width - cols * size) / 2;
        int top = 30;
        int index = 0;

        // Pass 1: drop shadow (gives the old "3D blocks" feeling).
        // Pass 2: stone faces with a light top/left edge.
        for (int pass = 0; pass < 2; pass++) {
            index = 0;
            for (int r = 0; r < logo.length; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!logo[r][c]) continue;
                    if (index++ == missingBlock) continue;
                    float drop = 0f;
                    if (animateLogo) {
                        float p = Mth.clamp((t - fallDelay[r][c]) / 0.6f, 0f, 1f);
                        drop = (1f - p * p) * -(top + size * 6); // ease-in fall from above the screen
                    }
                    int x = left + c * size;
                    int y = top + r * size + Math.round(drop);
                    if (pass == 0) {
                        g.fill(x + 2, y + 2, x + size + 2, y + size + 2, 0xC0000000);
                    } else {
                        int u = (c * 5 + r * 3) % (16 - Math.min(size, 16));
                        g.blit(STONE, x, y, size, size, u, (r * 4) % 8, size, size, 16, 16);
                        g.fill(x, y, x + size, y + 1, 0x50FFFFFF);
                        g.fill(x, y, x + 1, y + size, 0x30FFFFFF);
                        g.fill(x, y + size - 1, x + size, y + size, 0x50000000);
                    }
                }
            }
        }
        return top + logo.length * size;
    }

    private void renderSplash(GuiGraphics g, int logoBottom) {
        Component text = Component.translatable("hollowsignal.splash." + splash);
        float pulse = 1.8f - Mth.abs(Mth.sin((Util.getMillis() % 1000L) / 1000f * Mth.TWO_PI) * 0.1f);
        float scale = pulse * 100f / (font.width(text) + 32);
        g.pose().pushPose();
        g.pose().translate(width / 2f + 90f, logoBottom - 6f, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-20f));
        g.pose().scale(scale, scale, scale);
        g.drawCenteredString(font, text, 0, -8, 0xFFFF00);
        g.pose().popPose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
