package com.hollowsignal.client.screen;

import com.hollowsignal.config.HSConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

/**
 * The only way to turn HARDCORE on: lists exactly what can happen outside the game,
 * then requires typing a confirmation phrase and waiting a few seconds.
 */
public class HardcoreWarningScreen extends Screen {
    private static final int WAIT_TICKS = 100;
    private final Screen parent;
    private EditBox input;
    private Button confirm;
    private int age;
    private List<FormattedCharSequence> body = List.of();

    public HardcoreWarningScreen(Screen parent) {
        super(Component.translatable("hollowsignal.hardcore.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        this.parent = parent;
    }

    private String phrase() {
        return I18n.get("hollowsignal.hardcore.phrase");
    }

    @Override
    protected void init() {
        body = font.split(Component.translatable("hollowsignal.hardcore.body", phrase()), Math.min(380, width - 40));
        int y = Math.min(height - 70, 40 + body.size() * 10 + 10);
        input = new EditBox(font, width / 2 - 100, y, 200, 20, Component.empty());
        input.setMaxLength(40);
        addRenderableWidget(input);
        confirm = addRenderableWidget(Button.builder(Component.translatable("hollowsignal.hardcore.confirm"), b -> {
            HSConfig.CLIENT.setHardcoreConfirmed();
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 102, y + 26, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 + 2, y + 26, 100, 20).build());
        setInitialFocus(input);
        updateButton();
    }

    private void updateButton() {
        boolean typed = input != null && input.getValue().trim().toUpperCase(Locale.ROOT).equals(phrase().toUpperCase(Locale.ROOT));
        confirm.active = typed && age >= WAIT_TICKS;
        int left = Math.max(0, (WAIT_TICKS - age + 19) / 20);
        confirm.setMessage(left > 0 ? Component.literal(left + "…") : Component.translatable("hollowsignal.hardcore.confirm"));
    }

    @Override
    public void tick() {
        age++;
        updateButton();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        int y = 40;
        for (FormattedCharSequence line : body) {
            g.drawCenteredString(font, line, width / 2, y, 0xE0E0E0);
            y += 10;
        }
    }

    /** Cancelling (or closing) always leaves you in SAFE mode. */
    @Override
    public void onClose() {
        if (!HSConfig.CLIENT.hardcore()) HSConfig.CLIENT.setSafe();
        minecraft.setScreen(parent);
    }
}
