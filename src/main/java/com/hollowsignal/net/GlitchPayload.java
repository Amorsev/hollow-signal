package com.hollowsignal.net;

import com.hollowsignal.HollowSignal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: screen distortion + interference sound for `ticks`, strength 0..1. */
public record GlitchPayload(int ticks, float intensity, boolean withSound) implements CustomPacketPayload {
    public static final Type<GlitchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "glitch"));

    public static final StreamCodec<ByteBuf, GlitchPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GlitchPayload::ticks,
            ByteBufCodecs.FLOAT, GlitchPayload::intensity,
            ByteBufCodecs.BOOL, GlitchPayload::withSound,
            GlitchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
