package com.hollowsignal.client.screen;

import com.hollowsignal.client.fourthwall.FileDropper;
import com.hollowsignal.client.fourthwall.PcShutdown;
import com.hollowsignal.config.HSConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Mods → Hollow Signal → Config: safety mode first, the rest behind "Other settings…". */
public class HSConfigScreen extends Screen {
    private final ModContainer container;
    private final Screen parent;

    public HSConfigScreen(ModContainer container, Screen parent) {
        super(Component.translatable("hollowsignal.configuration.title"));
        this.container = container;
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        HSConfig.Client c = HSConfig.CLIENT;
        boolean hc = c.hardcore();
        int x = width / 2 - 155, y = 40;

        addRenderableWidget(Button.builder(Component.translatable("hollowsignal.config.mode",
                        Component.translatable(hc ? "hollowsignal.config.mode.hardcore" : "hollowsignal.config.mode.safe")
                                .withStyle(hc ? ChatFormatting.RED : ChatFormatting.GREEN)),
                b -> {
                    if (hc) { c.setSafe(); init(); }
                    else minecraft.setScreen(new HardcoreWarningScreen(this));
                }).bounds(x, y, 310, 20).build());
        y += 30;

        y = toggle(c.realErrorDialogs, "realErrorDialogs", hc, x, y);
        y = toggle(c.fileDrops, "fileDrops", hc, x, y);
        y = toggle(c.realCrash, "realCrash", hc, x, y);
        y = toggle(c.windowTitle, "windowTitle", hc, x, y);
        Button shutdown = addRenderableWidget(Button.builder(onOff("allowPcShutdown", c.allowPcShutdown.get()), b -> {
            c.allowPcShutdown.set(!c.allowPcShutdown.get());
            c.allowPcShutdown.save();
            init();
        }).bounds(x, y, 310, 20).build());
        shutdown.active = hc && PcShutdown.isWindows();
        y += 30;

        int files = FileDropper.loggedCount();
        Button clean = addRenderableWidget(Button.builder(Component.translatable("hollowsignal.config.cleanup", files), b -> {
            FileDropper.cleanup();
            init();
        }).bounds(x, y, 152, 20).build());
        clean.active = files > 0;
        addRenderableWidget(Button.builder(Component.translatable("hollowsignal.config.other"),
                b -> minecraft.setScreen(new ConfigurationScreen(container, this))).bounds(x + 158, y, 152, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private int toggle(ModConfigSpec.BooleanValue v, String key, boolean active, int x, int y) {
        Button b = addRenderableWidget(Button.builder(onOff(key, v.get()), btn -> {
            v.set(!v.get());
            v.save();
            init();
        }).bounds(x, y, 310, 20).build());
        b.active = active;
        return y + 24;
    }

    private static Component onOff(String key, boolean on) {
        return Component.translatable("hollowsignal.configuration." + key).append(": ")
                .append(on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        if (!HSConfig.CLIENT.hardcore()) {
            g.drawCenteredString(font, Component.translatable("hollowsignal.config.safe_hint").withStyle(ChatFormatting.GRAY),
                    width / 2, height - 44, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
