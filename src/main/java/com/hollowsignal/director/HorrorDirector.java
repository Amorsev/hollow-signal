package com.hollowsignal.director;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side scheduler. Every N seconds it:
 * 1) grows dread (personal: idle/dark/underground/alone; global: time),
 * 2) rolls whether something happens,
 * 3) picks a victim (lonely, scared, underground players are preferred),
 * 4) picks an event by weight respecting stage and cooldowns.
 */
public final class HorrorDirector {
    private HorrorDirector() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        long interval = HSConfig.SERVER.directorIntervalSeconds.get() * 20L;
        if (now % interval != 0) return;

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && p.isAlive()) players.add(p);
        }
        if (players.isEmpty()) return;

        WorldHorrorData world = WorldHorrorData.get(server);
        int stage = Stage.compute(server, world);
        if (stage == Stage.SILENCE) return;

        RandomSource random = server.overworld().getRandom();
        double intensity = intensity();
        growDread(players, world, intensity, random);

        double chance = Math.min(0.95, (0.25 + 0.08 * stage) * intensity);
        if (random.nextDouble() > chance) return;

        // Per-player gap grows with player count, so a full server isn't spammed.
        long playerGap = (long) (interval * (2 + players.size() / 2) / intensity);
        ServerPlayer victim = pickVictim(players, now, playerGap, random);
        if (victim == null) return;

        // If the chosen event finds no valid spot, try up to two others this round.
        EventContext ctx = EventContext.of(victim, stage);
        List<HorrorEvent> tried = new ArrayList<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            HorrorEvent chosen = pickEvent(ctx, now, random, tried);
            if (chosen == null || execute(chosen, ctx, world, now)) return;
            tried.add(chosen);
        }
    }

    public static double intensity() {
        return HSConfig.SERVER.intensity.get();
    }

    public static boolean disabled(HorrorEvent e) {
        return HSConfig.SERVER.disabledEvents.get().contains(e.id());
    }

    /** Cooldown after intensity scaling. */
    public static long cooldown(HorrorEvent e) {
        return (long) (e.cooldownTicks() / intensity());
    }

    /** Scales an amount by intensity with random rounding, so small values still average out right. */
    private static int scaled(int amount, double intensity, RandomSource random) {
        double v = amount * intensity;
        int whole = (int) Math.floor(v);
        return whole + (random.nextDouble() < v - whole ? 1 : 0);
    }

    private static void growDread(List<ServerPlayer> players, WorldHorrorData world, double intensity, RandomSource random) {
        for (ServerPlayer p : players) {
            PlayerHorrorData d = ModAttachments.of(p);
            int idle = d.updateIdle(p.blockPosition());
            int gain = 1;
            if (idle >= 2) gain += 2;
            if (WorldUtil.isDark(p.serverLevel(), p.blockPosition())) gain++;
            if (WorldUtil.isUnderground(p.serverLevel(), p.blockPosition())) gain++;
            if (players.size() > 1 && WorldUtil.isAlone(p, 48)) gain++;
            d.addDread(scaled(gain, intensity, random));
        }
        world.addGlobalDread(scaled(1 + players.size() / 3, intensity, random));
    }

    private static ServerPlayer pickVictim(List<ServerPlayer> players, long now, long gap, RandomSource random) {
        List<ServerPlayer> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (ServerPlayer p : players) {
            PlayerHorrorData d = ModAttachments.of(p);
            if (now - d.lastEventTick() < gap) continue;
            int w = 2 + d.dread() / 200;
            if (players.size() > 1 && WorldUtil.isAlone(p, 48)) w += 3;
            if (WorldUtil.isDark(p.serverLevel(), p.blockPosition())) w++;
            if (WorldUtil.isUnderground(p.serverLevel(), p.blockPosition())) w++;
            pool.add(p);
            weights.add(w);
            total += w;
        }
        if (pool.isEmpty()) return null;
        int roll = random.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private static HorrorEvent pickEvent(EventContext ctx, long now, RandomSource random, List<HorrorEvent> exclude) {
        PlayerHorrorData d = ModAttachments.of(ctx.player());
        List<HorrorEvent> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (HorrorEvent e : HorrorEvents.all()) {
            if (e.minStage() > ctx.stage() || exclude.contains(e) || disabled(e)) continue;
            if (now - d.lastRun(e.id()) < cooldown(e)) continue;
            int w = e.weight(ctx);
            if (w <= 0) continue;
            // Together with friends: fewer jumpscares, more traces everyone can find.
            if (!ctx.alone() && ctx.player().getServer().getPlayerList().getPlayerCount() > 1) {
                if (e.scare()) w = Math.max(1, w / 2);
                else if (e.minStage() == Stage.TRACES) w = w * 3 / 2;
            }
            pool.add(e);
            weights.add(w);
            total += w;
        }
        if (pool.isEmpty()) return null;
        int roll = random.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private static boolean execute(HorrorEvent e, EventContext ctx, WorldHorrorData world, long now) {
        boolean ok;
        try {
            ok = e.run(ctx);
        } catch (Exception ex) {
            HollowSignal.LOGGER.error("Horror event {} failed", e.id(), ex);
            return false;
        }
        if (!ok) return false;
        PlayerHorrorData d = ModAttachments.of(ctx.player());
        d.markRun(e.id(), now);
        d.setLastEventTick(now);
        d.addDread(e.dreadGain());
        world.addGlobalDread(scaled(Math.max(1, e.dreadGain() / 3), intensity(), ctx.random()));
        world.setLastGlobalEventTick(now);
        HollowSignal.LOGGER.debug("Event {} -> {}", e.id(), ctx.player().getGameProfile().getName());
        return true;
    }

    /** Debug: run an event now, ignoring stage and cooldowns (but it still needs a valid spot). */
    public static boolean forceRun(HorrorEvent e, ServerPlayer player) {
        MinecraftServer server = player.getServer();
        WorldHorrorData world = WorldHorrorData.get(server);
        EventContext ctx = EventContext.of(player, Math.max(Stage.TRACES, Stage.compute(server, world)));
        return execute(e, ctx, world, server.overworld().getGameTime());
    }
}
