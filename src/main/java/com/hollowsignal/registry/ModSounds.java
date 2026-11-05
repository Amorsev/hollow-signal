package com.hollowsignal.registry;

import com.hollowsignal.HollowSignal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events. For now sounds.json points at re-pitched vanilla files; swap in custom .ogg later. */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, HollowSignal.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LISTENER_VANISH = register("listener.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTENER_SCARE = register("listener.scare");
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTENER_PRESENCE = register("listener.presence");
    /** Original generated radio static (tools/gen_static_sound.sh). */
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTENER_STATIC = register("listener.static");
    /** Original generated two-tone "error" chime for the fake error dialog. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_ERROR = register("ui.error");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, name)));
    }
}
