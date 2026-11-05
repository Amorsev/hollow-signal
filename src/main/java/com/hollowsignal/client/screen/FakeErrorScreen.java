package com.hollowsignal.client.screen;

import com.hollowsignal.registry.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * SAFE stand-in for a real OS error box, drawn to look like a Windows 10 message box:
 * white title bar with a close button that turns red on hover, red round error icon,
 * system-like text, grey footer with a blue-focused "OK" button, soft drop shadow.
 * It's drawn in real screen pixels (ignoring the GUI scale) so it has the proportions of a real dialog,
 * and it can be dragged by its title bar. The world keeps running behind it.
 */
public class FakeErrorScreen extends Screen {
    private static final ResourceLocation UNIFORM = ResourceLocation.withDefaultNamespace("uniform");
    private static final String[] TITLES = {
            "javaw.exe - Application Error",
            "Java Virtual Machine Launcher",
            "Minecraft",
            "javaw.exe",
            "Windows",
    };

    // Windows 10 metrics (pixels)
    private static final int W = 420, TITLE_H = 31, FOOTER_H = 49, CLOSE_W = 46;
    private static final int BTN_W = 88, BTN_H = 26, ICON = 32, PAD = 25;
    private static final float TEXT_SCALE = 1.5f, TITLE_SCALE = 1.3f;

    private final int variant;
    private final Component body;
    private List<FormattedCharSequence> lines = List.of();
    private int h, wx = -1, wy = -1;
    private boolean dragging;
    private double dragDx, dragDy;
    private boolean dingPlayed;

    public FakeErrorScreen(int variant) {
        super(Component.literal(TITLES[variant % TITLES.length]));
        this.variant = variant % TITLES.length;
        this.body = Component.translatable("hollowsignal.error." + this.variant).withStyle(Style.EMPTY.withFont(UNIFORM));
    }

    private float gs() {
        return (float) minecraft.getWindow().getGuiScale();
    }

    @Override
    protected void init() {
        int textW = W - PAD - ICON - 12 - PAD;
        lines = font.split(body, (int) (textW / TEXT_SCALE));
        int textH = (int) (lines.size() * 9 * TEXT_SCALE);
        h = TITLE_H + Math.max(ICON, textH) + 2 * PAD + FOOTER_H - 4;
        if (wx < 0) {
            wx = (minecraft.getWindow().getWidth() - W) / 2;
            wy = (minecraft.getWindow().getHeight() - h) / 2;
        }
        if (!dingPlayed) {
            dingPlayed = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.UI_ERROR.get(), 1.0f, 0.6f));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No dimming: the world stays visible behind the dialog, like a real popup.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float gs = gs();
        double mx = mouseX * gs, my = mouseY * gs;
        g.pose().pushPose();
        g.pose().scale(1 / gs, 1 / gs, 1);

        int x = wx, y = wy, r = wx + W, b = wy + h;
        // soft drop shadow
        for (int i = 1; i <= 8; i++) {
            int a = 22 - i * 2;
            g.fill(x - i + 4, y - i + 6, r + i + 4, b + i + 6, a << 24);
        }
        // 1px window border + white body
        g.fill(x - 1, y - 1, r + 1, b + 1, 0xFF6E6E6E);
        g.fill(x, y, r, b, 0xFFFFFFFF);

        // title bar
        text(g, title.copy().withStyle(Style.EMPTY.withFont(UNIFORM)).getVisualOrderText(), x + 8, y + 9, TITLE_SCALE, 0xFF000000);
        boolean closeHover = in(mx, my, r - CLOSE_W, y, r, y + TITLE_H - 1);
        if (closeHover) g.fill(r - CLOSE_W, y, r, y + TITLE_H - 1, 0xFFE81123);
        cross(g, r - CLOSE_W / 2, y + TITLE_H / 2, 5, 1, closeHover ? 0xFFFFFFFF : 0xFF000000);

        // error icon: red disc with a white X
        int icx = x + PAD + ICON / 2, icy = y + TITLE_H + PAD + ICON / 2 - 4;
        disc(g, icx, icy, ICON / 2, 0xFFE81123);
        disc(g, icx, icy, ICON / 2 - 1, 0xFFF0262E);
        cross(g, icx, icy, 7, 2, 0xFFFFFFFF);

        // message
        int tx = x + PAD + ICON + 12, ty = y + TITLE_H + PAD - 2;
        for (FormattedCharSequence line : lines) {
            text(g, line, tx, ty, TEXT_SCALE, 0xFF000000);
            ty += (int) (9 * TEXT_SCALE);
        }

        // footer with OK button
        int fy = b - FOOTER_H;
        g.fill(x, fy, r, b, 0xFFF0F0F0);
        int bx = r - 11 - BTN_W, by = fy + (FOOTER_H - BTN_H) / 2;
        boolean okHover = in(mx, my, bx, by, bx + BTN_W, by + BTN_H);
        g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF0078D7);                              // focused (default) border
        g.fill(bx + 2, by + 2, bx + BTN_W - 2, by + BTN_H - 2, okHover ? 0xFFE5F1FB : 0xFFE1E1E1);
        var ok = Component.translatable("hollowsignal.error.ok").withStyle(Style.EMPTY.withFont(UNIFORM)).getVisualOrderText();
        int okW = (int) (font.width(ok) * TEXT_SCALE);
        text(g, ok, bx + (BTN_W - okW) / 2, by + 6, TEXT_SCALE, 0xFF000000);

        g.pose().popPose();
    }

    private void text(GuiGraphics g, FormattedCharSequence s, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Diagonal cross centred at (cx, cy). */
    private static void cross(GuiGraphics g, int cx, int cy, int half, int thick, int color) {
        for (int i = -half; i <= half; i++) {
            g.fill(cx + i - thick / 2, cy + i - thick / 2, cx + i - thick / 2 + Math.max(1, thick), cy + i - thick / 2 + Math.max(1, thick), color);
            g.fill(cx + i - thick / 2, cy - i - thick / 2, cx + i - thick / 2 + Math.max(1, thick), cy - i - thick / 2 + Math.max(1, thick), color);
        }
    }

    private static void disc(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy < radius; dy++) {
            int half = (int) Math.round(Math.sqrt(radius * radius - (dy + 0.5) * (dy + 0.5)));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private static boolean in(double mx, double my, int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float gs = gs();
        double mx = mouseX * gs, my = mouseY * gs;
        int r = wx + W, b = wy + h;
        int bx = r - 11 - BTN_W, by = b - FOOTER_H + (FOOTER_H - BTN_H) / 2;
        if (in(mx, my, r - CLOSE_W, wy, r, wy + TITLE_H) || in(mx, my, bx, by, bx + BTN_W, by + BTN_H)) {
            onClose();
            return true;
        }
        if (in(mx, my, wx, wy, r - CLOSE_W, wy + TITLE_H)) {
            dragging = true;
            dragDx = mx - wx;
            dragDy = my - wy;
            return true;
        }
        return true; // modal: swallow clicks
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging) return false;
        float gs = gs();
        wx = (int) (mouseX * gs - dragDx);
        wy = (int) (mouseY * gs - dragDy);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return true;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
