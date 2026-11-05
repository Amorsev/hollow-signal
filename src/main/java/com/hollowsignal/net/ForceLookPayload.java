package com.hollowsignal.net;

import com.hollowsignal.HollowSignal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: turn the player's camera towards a point over a few ticks. */
public record ForceLookPayload(double x, double y, double z, int ticks) implements CustomPacketPayload {
    public static final Type<ForceLookPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "force_look"));

    public static final StreamCodec<ByteBuf, ForceLookPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ForceLookPayload::x,
            ByteBufCodecs.DOUBLE, ForceLookPayload::y,
            ByteBufCodecs.DOUBLE, ForceLookPayload::z,
            ByteBufCodecs.VAR_INT, ForceLookPayload::ticks,
            ForceLookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
