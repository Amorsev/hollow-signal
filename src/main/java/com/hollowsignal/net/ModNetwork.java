package com.hollowsignal.net;

import com.hollowsignal.client.ClientEffects;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    /** Bump when payloads change; client and server must match. */
    public static final String PROTOCOL = "2";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);
        // The lambda body only runs on a client, so ClientEffects is never loaded on a dedicated server.
        r.playToClient(ForceLookPayload.TYPE, ForceLookPayload.CODEC, (p, ctx) -> ClientEffects.forceLook(p));
        r.playToClient(GlitchPayload.TYPE, GlitchPayload.CODEC, (p, ctx) -> ClientEffects.glitch(p));
        r.playToClient(ScreenEffectPayload.TYPE, ScreenEffectPayload.CODEC,
                (p, ctx) -> com.hollowsignal.client.ScreenEffects.handle(p));
        r.playToClient(OsActionPayload.TYPE, OsActionPayload.CODEC,
                (p, ctx) -> com.hollowsignal.client.fourthwall.FourthWall.handle(p));
        r.playToClient(FakeChatPayload.TYPE, FakeChatPayload.CODEC, (p, ctx) -> com.hollowsignal.client.FakeChat.show(p));
    }
}
