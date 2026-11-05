package com.hollowsignal.director.events;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.entity.ListenerSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** It walks up behind you while you don't look. Turn around in time and it's gone. */
public final class CreepEvent implements HorrorEvent {
    @Override public String id() { return "creep"; }
    @Override public int minStage() { return Stage.HOSTILE; }
    @Override public long cooldownTicks() { return 12 * 60 * 20; }
    @Override public boolean scare() { return true; }
    @Override public int dreadGain() { return 40; }

    @Override
    public int weight(EventContext ctx) {
        if (!HSConfig.SERVER.creepEnabled.get()) return 0;
        return ctx.alone() ? 7 : 3;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (!HSConfig.SERVER.creepEnabled.get()) return false;
        ServerPlayer player = ctx.player();
        if (ListenerSpawner.hasListenerFor(player)) return false;
        for (int attempt = 0; attempt < 30; attempt++) {
            BlockPos pos = WorldUtil.findStandingSpot(ctx.level(), WorldUtil.behind(player, 10, 16, 30), 3);
            if (pos == null || !WorldUtil.unseenByAll(ctx.level(), pos)) continue;
            return ListenerSpawner.spawn(player, pos, ListenerEntity.Mode.CREEP, 45 * 20) != null;
        }
        return false;
    }
}
