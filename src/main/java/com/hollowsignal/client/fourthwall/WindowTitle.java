package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** HARDCORE: temporarily replaces the game window's title (the game resets it on its own, so we re-apply). */
@EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
public final class WindowTitle {
    private WindowTitle() {}

    private static String title;
    private static int ticks;

    public static void set(String newTitle, int durationTicks) {
        title = newTitle;
        ticks = durationTicks;
        apply();
    }

    private static void apply() {
        if (title != null) GLFW.glfwSetWindowTitle(Minecraft.getInstance().getWindow().getWindow(), title);
    }

    public static void restore() {
        if (title == null) return;
        title = null;
        ticks = 0;
        Minecraft.getInstance().updateTitle();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (title == null) return;
        if (--ticks <= 0) restore();
        else if (ticks % 20 == 0) apply();
    }
}
