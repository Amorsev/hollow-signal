package com.hollowsignal.net;

import com.hollowsignal.HollowSignal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: a chat line from the entity, shown only to this player. The client translates `key`
 * into its own language and applies the distortion, so text can be localized AND corrupted.
 */
public record FakeChatPayload(int kind, String name, String key, float distortion) implements CustomPacketPayload {
    /** ECHO: `key` is literal text (a real player's earlier message), not a translation key. */
    public static final int CHAT = 0, JOIN = 1, LEAVE = 2, SYSTEM = 3, CURSED = 4, ECHO = 5;

    public static final Type<FakeChatPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "fake_chat"));

    public static final StreamCodec<ByteBuf, FakeChatPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FakeChatPayload::kind,
            ByteBufCodecs.STRING_UTF8, FakeChatPayload::name,
            ByteBufCodecs.STRING_UTF8, FakeChatPayload::key,
            ByteBufCodecs.FLOAT, FakeChatPayload::distortion,
            FakeChatPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
