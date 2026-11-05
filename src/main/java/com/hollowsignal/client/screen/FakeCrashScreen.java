package com.hollowsignal.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The bad ending's crash: a crash report types itself out on black, the screen "dies".
 * Then `after` runs: in HARDCORE the real crash, in SAFE the fake shutdown screen.
 */
public class FakeCrashScreen extends Screen {
    private static final String[] REPORT = {
            "---- Minecraft Crash Report ----",
            "// it was listening.",
            "",
            "Time: %TIME%",
            "Description: Player record erased",
            "",
            "java.lang.NullPointerException: Cannot invoke \"Player.exists()\" because \"player\" is null",
            "\tat net.minecraft.world.level.Level.hear(Level.java:0)",
            "\tat net.minecraft.server.level.ServerLevel.listen(ServerLevel.java:0)",
            "\tat net.minecraft.server.MinecraftServer.tickChildren(MinecraftServer.java:0)",
            "\tat the_static.Signal.erase(Signal.java:0)",
            "",
            "A detailed walkthrough of the error, its code path and all known details is as follows:",
            "---------------------------------------------------------------------------------------",
            "",
            "-- Head --",
            "Thread: Listener thread",
            "Stacktrace:",
            "\tat you",
            "",
            "-- Affected level --",
            "Level name: (erased)",
            "Players: 0 / ? (1 unknown)",
    };

    private final int total;
    private int age;
    @Nullable private Runnable after;
    private final String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public FakeCrashScreen(int ticks) {
        this(ticks, null);
    }

    public FakeCrashScreen(int ticks, @Nullable Runnable after) {
        super(Component.empty());
        this.total = Math.max(80, ticks);
        this.after = after;
    }

    @Override
    public void tick() {
        if (++age >= total) finish();
    }

    private void finish() {
        Runnable a = after;
        after = null;
        if (a != null) a.run();
        else onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int shown = Math.min(REPORT.length, age / 2);
        if (age > total - 40) return; // the screen goes dead for the last two seconds
        int y = 6;
        for (int i = 0; i < shown; i++) {
            String line = REPORT[i].replace("%TIME%", time).replace("\t", "    ");
            int color = i == 0 ? 0xFFFFFF : (line.startsWith("java.") ? 0xFF5555 : 0xBBBBBB);
            g.drawString(font, line, 6, y, color, false);
            y += 10;
        }
        if (shown == REPORT.length && (age / 10) % 2 == 0) g.drawString(font, "_", 6, y, 0xBBBBBB, false);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
