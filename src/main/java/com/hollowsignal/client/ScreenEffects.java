package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.client.screen.EndingScreen;
import com.hollowsignal.client.screen.FakeCrashScreen;
import com.hollowsignal.client.screen.FakeDisconnectScreen;
import com.hollowsignal.client.screen.FakeErrorScreen;
import com.hollowsignal.client.screen.FakeLoadingScreen;
import com.hollowsignal.net.ScreenEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Fake screens, fog, HUD flicker and "slowed" world sounds. */
@EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
public final class ScreenEffects {
    private ScreenEffects() {}

    private static final RandomSource RANDOM = RandomSource.create();

    private static int hudTicks;
    private static boolean hudWasHidden;

    private static int fogTicks, fogTotal;
    private static int swapTicks;
    private static int guiBlockTicks;

    public static void handle(ScreenEffectPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (p.effect()) {
            case ScreenEffectPayload.FAKE_LOADING -> { if (canInterrupt(mc)) mc.setScreen(new FakeLoadingScreen(p.ticks(), p.variant())); }
            case ScreenEffectPayload.FAKE_DISCONNECT -> { if (canInterrupt(mc)) mc.setScreen(new FakeDisconnectScreen(p.variant())); }
            case ScreenEffectPayload.FAKE_ERROR -> { if (canInterrupt(mc)) mc.setScreen(new FakeErrorScreen(p.variant())); }
            case ScreenEffectPayload.HUD_FLICKER -> {
                if (hudTicks <= 0) hudWasHidden = mc.options.hideGui;
                hudTicks = p.ticks();
                mc.options.hideGui = true;
            }
            case ScreenEffectPayload.FOG -> fogTicks = fogTotal = Math.max(1, p.ticks());
            case ScreenEffectPayload.SOUND_SWAP -> swapTicks = p.ticks();
            // Endings always take over the screen, even from chat.
            case ScreenEffectPayload.ENDING -> mc.setScreen(new EndingScreen(p.ticks(), p.variant()));
            case ScreenEffectPayload.FAKE_CRASH -> mc.setScreen(new FakeCrashScreen(p.ticks()));
            case ScreenEffectPayload.GUI_BLOCK -> {
                if (guiBlockTicks <= 0) mc.gui.setOverlayMessage(Component.translatable("hollowsignal.gui_blocked"), false);
                guiBlockTicks = Math.max(guiBlockTicks, p.ticks());
                if (mc.screen instanceof AbstractContainerScreen<?>) mc.setScreen(null);
            }
            default -> { }
        }
    }

    /** Never yank the player out of typing a chat message. */
    private static boolean canInterrupt(Minecraft mc) {
        return !(mc.screen instanceof ChatScreen);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (hudTicks > 0 && --hudTicks == 0) mc.options.hideGui = hudWasHidden;
        if (fogTicks > 0) fogTicks--;
        if (swapTicks > 0) swapTicks--;
        if (guiBlockTicks > 0) guiBlockTicks--;
        if (mc.player == null) { hudTicks = 0; fogTicks = 0; swapTicks = 0; guiBlockTicks = 0; }
    }

    /** While the Hollow is close, inventories and containers refuse to open. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (guiBlockTicks > 0 && event.getNewScreen() instanceof AbstractContainerScreen<?>) {
            event.setCanceled(true);
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("hollowsignal.gui_blocked"), false);
        }
    }

    /** 0..1: fade in over the first 10 s, fade out over the last 10 s. */
    private static float fogFactor(float partial) {
        if (fogTicks <= 0) return 0f;
        float elapsed = fogTotal - fogTicks + partial;
        float in = Mth.clamp(elapsed / 200f, 0f, 1f);
        float out = Mth.clamp((fogTicks - partial) / 200f, 0f, 1f);
        return Math.min(in, out);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float f = fogFactor((float) event.getPartialTick());
        if (f <= 0f || event.getType() != FogType.NONE) return;
        event.setFarPlaneDistance(Mth.lerp(f, event.getFarPlaneDistance(), 22f));
        event.setNearPlaneDistance(Mth.lerp(f, event.getNearPlaneDistance(), 0f));
        event.setCanceled(true); // required for the new distances to apply
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float f = fogFactor((float) event.getPartialTick());
        if (f <= 0f) return;
        event.setRed(Mth.lerp(f, event.getRed(), 0.16f));
        event.setGreen(Mth.lerp(f, event.getGreen(), 0.16f));
        event.setBlue(Mth.lerp(f, event.getBlue(), 0.18f));
    }

    /** While active, every world sound plays slowed down and deep — as if the world is "wrong". */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (swapTicks <= 0) return;
        SoundInstance s = event.getSound();
        if (s == null || s.getSource() == SoundSource.MUSIC || s.getSource() == SoundSource.MASTER
                || HollowSignal.MODID.equals(s.getLocation().getNamespace())) return;
        // The event fires before the instance is resolved; resolve it to read its real volume.
        if (s.resolve(Minecraft.getInstance().getSoundManager()) == null) return;
        float volume = Mth.clamp(s.getVolume(), 0f, 1f);
        event.setSound(new SimpleSoundInstance(s.getLocation(), s.getSource(), volume, 0.5f + RANDOM.nextFloat() * 0.1f,
                RANDOM, s.isLooping(), s.getDelay(), s.getAttenuation(), s.getX(), s.getY(), s.getZ(), s.isRelative()));
    }
}
