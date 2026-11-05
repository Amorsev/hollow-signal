package com.hollowsignal.net;

import com.hollowsignal.HollowSignal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: an INTENT for a fourth-wall effect. The server never decides what happens on the player's PC:
 * the client runs the real (HARDCORE) version only if the player enabled and confirmed HARDCORE locally
 * AND the server allows it (`serverAllows`); otherwise it plays the in-game SAFE version.
 * All text comes from the mod's own translation files — never from other players.
 */
public record OsActionPayload(int action, int variant, String entityName, boolean serverAllows) implements CustomPacketPayload {
    public static final int ERROR_DIALOG = 0, FILE_DROP = 1, CRASH = 2, WINDOW_TITLE = 3;

    public static final Type<OsActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "os_action"));

    public static final StreamCodec<ByteBuf, OsActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OsActionPayload::action,
            ByteBufCodecs.VAR_INT, OsActionPayload::variant,
            ByteBufCodecs.STRING_UTF8, OsActionPayload::entityName,
            ByteBufCodecs.BOOL, OsActionPayload::serverAllows,
            OsActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
