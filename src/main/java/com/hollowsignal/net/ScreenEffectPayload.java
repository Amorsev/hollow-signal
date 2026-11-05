package com.hollowsignal.net;

import com.hollowsignal.HollowSignal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: a client-side "glitch" effect. All of them stay inside the game window (SAFE). */
public record ScreenEffectPayload(int effect, int ticks, int variant) implements CustomPacketPayload {
    public static final int FAKE_LOADING = 0, FAKE_DISCONNECT = 1, FAKE_ERROR = 2, HUD_FLICKER = 3, FOG = 4, SOUND_SWAP = 5, GUI_BLOCK = 6,
            ENDING = 7, FAKE_CRASH = 8;

    public static final Type<ScreenEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "screen_effect"));

    public static final StreamCodec<ByteBuf, ScreenEffectPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScreenEffectPayload::effect,
            ByteBufCodecs.VAR_INT, ScreenEffectPayload::ticks,
            ByteBufCodecs.VAR_INT, ScreenEffectPayload::variant,
            ScreenEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
