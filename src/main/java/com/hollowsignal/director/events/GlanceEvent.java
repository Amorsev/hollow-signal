package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.entity.ListenerSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Jumpscare: it appears behind you, your head snaps towards it, it's gone. */
public final class GlanceEvent implements HorrorEvent {
    @Override public String id() { return "glance"; }
    @Override public int minStage() { return Stage.WATCHING; }
    @Override public long cooldownTicks() { return 15 * 60 * 20; }
    @Override public boolean scare() { return true; }
    @Override public int dreadGain() { return 35; }

    @Override
    public int weight(EventContext ctx) {
        return ctx.dark() ? 6 : 3;
    }

    @Override
    public boolean run(EventContext ctx) {
        ServerPlayer player = ctx.player();
        if (ListenerSpawner.hasListenerFor(player)) return false;
        for (int attempt = 0; attempt < 30; attempt++) {
            BlockPos base = WorldUtil.behind(player, 10, 18, 40);
            BlockPos pos = WorldUtil.findStandingSpot(ctx.level(), base, 5);
            if (pos == null || !WorldUtil.unseenByAll(ctx.level(), pos) || !ListenerSpawner.canSeePlayerFrom(player, pos)) continue;
            return ListenerSpawner.spawn(player, pos, ListenerEntity.Mode.GLANCE, 60) != null;
        }
        return false;
    }
}
