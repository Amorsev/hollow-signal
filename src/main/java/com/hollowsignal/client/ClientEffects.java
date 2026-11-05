package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.net.ForceLookPayload;
import com.hollowsignal.net.GlitchPayload;
import com.hollowsignal.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client-side execution of server-requested effects. */
@EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
public final class ClientEffects {
    private ClientEffects() {}

    private static final RandomSource RANDOM = RandomSource.create();
    /** Static is meant to be quiet but clearly audible. */
    private static final float STATIC_VOLUME = 0.35f;

    private static Vec3 lookTarget;
    private static int lookTicks;

    private static int glitchTicks;
    private static int glitchTotal;
    private static float glitchStrength;

    public static void forceLook(ForceLookPayload p) {
        lookTarget = new Vec3(p.x(), p.y(), p.z());
        lookTicks = p.ticks();
    }

    public static void glitch(GlitchPayload p) {
        glitchTicks = glitchTotal = Math.max(1, p.ticks());
        glitchStrength = Mth.clamp(p.intensity(), 0f, 1f);
        if (p.withSound()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    ModSounds.LISTENER_STATIC.get(), 0.9f + RANDOM.nextFloat() * 0.2f, STATIC_VOLUME));
        }
    }

    /** Current distortion strength 0..1, with a fast attack, slow fade and random flicker. */
    public static float glitchIntensity(float partialTick) {
        if (glitchTicks <= 0) return 0f;
        float remaining = (glitchTicks - partialTick) / glitchTotal;      // 1 -> 0
        float envelope = Mth.clamp(remaining * 1.6f, 0f, 1f);              // hold, then fade
        float flicker = 0.75f + RANDOM.nextFloat() * 0.25f;
        return glitchStrength * envelope * flicker;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (glitchTicks > 0) glitchTicks--;
        if (lookTicks <= 0) return;
        lookTicks--;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || lookTarget == null) { lookTicks = 0; return; }

        Vec3 d = lookTarget.subtract(player.getEyePosition());
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(d.y, horiz) * Mth.RAD_TO_DEG);

        float newYaw = player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * 0.6f;
        float newPitch = player.getXRot() + (pitch - player.getXRot()) * 0.6f;
        player.setYRot(newYaw);
        player.setXRot(Mth.clamp(newPitch, -90f, 90f));
        player.setYHeadRot(newYaw);
    }

    /** Camera jitter while the glitch is active. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float i = glitchIntensity((float) event.getPartialTick());
        if (i <= 0f) return;
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5f) * 7f * i);
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5f) * 1.6f * i);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5f) * 1.2f * i);
    }
}
