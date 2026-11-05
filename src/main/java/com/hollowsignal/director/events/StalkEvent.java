package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.entity.ListenerSpawner;
import com.hollowsignal.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** The Listener stands far away, at the edge of vision, and stares. */
public final class StalkEvent implements HorrorEvent {
    @Override public String id() { return "stalk"; }
    @Override public int minStage() { return Stage.WATCHING; }
    @Override public long cooldownTicks() { return 6 * 60 * 20; }
    @Override public int dreadGain() { return 20; }
    @Override public int weight(EventContext ctx) { return 10; }

    @Override
    public boolean run(EventContext ctx) {
        boolean ok = ctx.underground() ? spawnStalk(ctx, 16, 28, (40 + ctx.random().nextInt(41)) * 20)
                : spawnStalk(ctx, 32, 56, (40 + ctx.random().nextInt(41)) * 20);
        if (ok && ctx.random().nextInt(3) == 0) {
            ctx.player().playNotifySound(ModSounds.LISTENER_PRESENCE.get(), SoundSource.AMBIENT, 0.6f, 1.0f);
        }
        return ok;
    }

    /** Places a staring Listener in the player's peripheral vision at the given distance range. */
    public static boolean spawnStalk(EventContext ctx, int minDist, int maxDist, int lifeTicks) {
        return spawnPeripheral(ctx, minDist, maxDist, lifeTicks, ListenerEntity.Mode.STALK) != null;
    }

    /** Same placement rules, any mode; returns the spawned Listener or null. */
    @org.jetbrains.annotations.Nullable
    public static ListenerEntity spawnPeripheral(EventContext ctx, int minDist, int maxDist, int lifeTicks, ListenerEntity.Mode mode) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        if (ListenerSpawner.hasListenerFor(player)) return null;

        for (int attempt = 0; attempt < 40; attempt++) {
            // Peripheral vision (65°..110° to either side): not spotted on arrival, easy to notice later.
            float angle = (65 + ctx.random().nextFloat() * 45) * (ctx.random().nextBoolean() ? 1 : -1);
            int dist = minDist + ctx.random().nextInt(maxDist - minDist + 1);
            BlockPos pos;
            if (ctx.underground()) {
                pos = WorldUtil.findStandingSpot(level, ListenerSpawner.at(player, angle, dist), 6);
            } else {
                BlockPos xz = ListenerSpawner.at(player, angle, dist);
                pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
                if (Math.abs(pos.getY() - player.getBlockY()) > 20 || !WorldUtil.isOpen(level, pos)) pos = null;
            }
            if (pos == null || !WorldUtil.unseenByAll(level, pos) || !ListenerSpawner.canSeePlayerFrom(player, pos)) continue;
            return ListenerSpawner.spawn(player, pos, mode, lifeTicks);
        }
        return null;
    }
}
