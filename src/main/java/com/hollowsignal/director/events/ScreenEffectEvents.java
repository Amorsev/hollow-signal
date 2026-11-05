package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.net.GlitchPayload;
import com.hollowsignal.net.ScreenEffectPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Stage-5 client glitches: fake screens, HUD flicker, fog, slowed sounds. All in-game (SAFE). */
public final class ScreenEffectEvents {
    private ScreenEffectEvents() {}

    public static List<HorrorEvent> all() {
        return List.of(new FakeLoading(), new FakeDisconnect(), new FakeError(), new HudFlicker(), new Fog(), new SoundSwap());
    }

    private static void send(EventContext ctx, int effect, int ticks) {
        PacketDistributor.sendToPlayer(ctx.player(), new ScreenEffectPayload(effect, ticks, ctx.random().nextInt(1000)));
    }

    static final class FakeLoading implements HorrorEvent {
        @Override public String id() { return "fake_loading"; }
        @Override public int minStage() { return Stage.CONTACT; }
        @Override public long cooldownTicks() { return 20 * 60 * 20; }
        @Override public int dreadGain() { return 20; }
        @Override public int weight(EventContext ctx) { return 3; }
        @Override public boolean run(EventContext ctx) {
            send(ctx, ScreenEffectPayload.FAKE_LOADING, 60 + ctx.random().nextInt(80));
            return true;
        }
    }

    static final class FakeDisconnect implements HorrorEvent {
        @Override public String id() { return "fake_disconnect"; }
        @Override public int minStage() { return Stage.HOSTILE; }
        @Override public long cooldownTicks() { return 40 * 60 * 20; }
        @Override public int dreadGain() { return 30; }
        @Override public int weight(EventContext ctx) { return 2; }
        @Override public boolean run(EventContext ctx) {
            PacketDistributor.sendToPlayer(ctx.player(), new GlitchPayload(16, 0.8f, true));
            send(ctx, ScreenEffectPayload.FAKE_DISCONNECT, 0);
            return true;
        }
    }

    static final class FakeError implements HorrorEvent {
        @Override public String id() { return "fake_error"; }
        @Override public int minStage() { return Stage.HOSTILE; }
        @Override public long cooldownTicks() { return 20 * 60 * 20; }
        @Override public int dreadGain() { return 20; }
        @Override public int weight(EventContext ctx) { return 3; }
        @Override public boolean run(EventContext ctx) {
            // Real OS dialog in HARDCORE, the Windows 10-style in-game dialog otherwise.
            FourthWallEvents.send(ctx.player(), com.hollowsignal.net.OsActionPayload.ERROR_DIALOG, ctx.random().nextInt(1000));
            return true;
        }
    }

    static final class HudFlicker implements HorrorEvent {
        @Override public String id() { return "hud_flicker"; }
        @Override public int minStage() { return Stage.WATCHING; }
        @Override public long cooldownTicks() { return 8 * 60 * 20; }
        @Override public int dreadGain() { return 8; }
        @Override public int weight(EventContext ctx) { return 4; }
        @Override public boolean run(EventContext ctx) {
            int ticks = 30 + ctx.random().nextInt(40);
            PacketDistributor.sendToPlayer(ctx.player(), new GlitchPayload(Math.min(ticks, 20), 0.35f, false));
            send(ctx, ScreenEffectPayload.HUD_FLICKER, ticks);
            return true;
        }
    }

    /** The fog closes in — and something stands right at its edge. */
    static final class Fog implements HorrorEvent {
        @Override public String id() { return "fog"; }
        @Override public int minStage() { return Stage.WATCHING; }
        @Override public long cooldownTicks() { return 25 * 60 * 20; }
        @Override public int dreadGain() { return 25; }
        @Override public int weight(EventContext ctx) { return ctx.underground() ? 0 : 4; }
        @Override public boolean run(EventContext ctx) {
            int ticks = (70 + ctx.random().nextInt(41)) * 20;
            send(ctx, ScreenEffectPayload.FOG, ticks);
            StalkEvent.spawnStalk(ctx, 18, 24, ticks - 200); // optional: the fog works without it
            return true;
        }
    }

    static final class SoundSwap implements HorrorEvent {
        @Override public String id() { return "sound_swap"; }
        @Override public int minStage() { return Stage.HOSTILE; }
        @Override public long cooldownTicks() { return 15 * 60 * 20; }
        @Override public int dreadGain() { return 15; }
        @Override public int weight(EventContext ctx) { return 3; }
        @Override public boolean run(EventContext ctx) {
            send(ctx, ScreenEffectPayload.SOUND_SWAP, (25 + ctx.random().nextInt(20)) * 20);
            return true;
        }
    }
}
