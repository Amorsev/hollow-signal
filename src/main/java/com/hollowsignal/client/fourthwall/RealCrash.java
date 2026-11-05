package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.config.HSConfig;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * HARDCORE: the bad ending really closes the game.
 * 1) leave the world normally — in singleplayer this SAVES the world and waits for the server to stop;
 * 2) (optional, separate opt-in) schedule a cancellable PC shutdown;
 * 3) a couple of seconds later, crash the game with our own crash report.
 */
@EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
public final class RealCrash {
    private RealCrash() {}

    /** Thrown into the crash report so it reads like a real one. */
    public static final class SignalLostException extends RuntimeException {
        public SignalLostException(String msg) { super(msg); }
    }

    private static int countdown = -1;

    public static void begin() {
        Minecraft mc = Minecraft.getInstance();
        HollowSignal.LOGGER.info("[HARDCORE] real crash requested: saving and leaving the world first");
        if (HSConfig.CLIENT.allowPcShutdown.get()) {
            PcShutdown.schedule(HSConfig.CLIENT.shutdownDelaySeconds.get(), I18n.get("hollowsignal.fourthwall.shutdown_message"));
        }
        if (mc.level != null) {
            mc.level.disconnect();
            mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        }
        countdown = 40;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (countdown < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) return; // still saving — wait
        if (--countdown == 0) {
            countdown = -1;
            mc.delayCrash(new CrashReport("Player record erased", new SignalLostException("it was listening")));
        }
    }
}
