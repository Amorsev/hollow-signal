package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import com.hollowsignal.entity.ListenerSpawner;
import com.hollowsignal.entity.SignalMob;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;

import java.util.List;

/** Stage-6 events: Watcher (→ Reporter → report) and the Hollow. */
public final class SignalMobEvents {
    private SignalMobEvents() {}

    public static List<HorrorEvent> all() {
        return List.of(new Watcher(), new Hollow());
    }

    /** Appears behind a player who has been standing still. */
    static final class Watcher implements HorrorEvent {
        @Override public String id() { return "watcher"; }
        @Override public int minStage() { return Stage.WATCHING; }
        @Override public long cooldownTicks() { return 10 * 60 * 20; }
        @Override public int dreadGain() { return 20; }

        @Override
        public int weight(EventContext ctx) {
            return ModAttachments.of(ctx.player()).idleChecks() >= 1 ? 12 : 2;
        }

        @Override
        public boolean run(EventContext ctx) {
            ServerPlayer player = ctx.player();
            if (SignalMob.hasFor(ModEntities.WATCHER.get(), player)) return false;
            for (int attempt = 0; attempt < 30; attempt++) {
                BlockPos pos = WorldUtil.findStandingSpot(ctx.level(), WorldUtil.behind(player, 8, 14, 50), 5);
                if (pos == null || !WorldUtil.isOpen(ctx.level(), pos.above(2))) continue; // it's tall
                if (!WorldUtil.unseenByAll(ctx.level(), pos) || !ListenerSpawner.canSeePlayerFrom(player, pos)) continue;
                return SignalMob.spawn(ModEntities.WATCHER.get(), player, pos, 90 * 20) != null;
            }
            return false;
        }
    }

    /** Comes out of the dark and hunts. Light drives it away. */
    static final class Hollow implements HorrorEvent {
        @Override public boolean scare() { return true; }
        @Override public String id() { return "hollow"; }
        @Override public int minStage() { return Stage.HOSTILE; }
        @Override public long cooldownTicks() { return 15 * 60 * 20; }
        @Override public int dreadGain() { return 40; }

        @Override
        public int weight(EventContext ctx) {
            if (!ctx.dark()) return 1;
            return ctx.underground() ? 8 : 5;
        }

        @Override
        public boolean run(EventContext ctx) {
            ServerPlayer player = ctx.player();
            if (SignalMob.hasFor(ModEntities.HOLLOW.get(), player)) return false;
            for (int attempt = 0; attempt < 40; attempt++) {
                BlockPos pos = WorldUtil.findStandingSpot(ctx.level(), WorldUtil.behind(player, 12, 20, 70), 6);
                if (pos == null || !WorldUtil.isOpen(ctx.level(), pos.above(2))) continue;
                if (ctx.level().getBrightness(LightLayer.BLOCK, pos) >= 8) continue; // only from the dark
                if (!WorldUtil.unseenByAll(ctx.level(), pos)) continue;
                return SignalMob.spawn(ModEntities.HOLLOW.get(), player, pos, 90 * 20) != null;
            }
            return false;
        }
    }
}
