package com.hollowsignal.director;

import com.hollowsignal.config.HSConfig;
import net.minecraft.server.MinecraftServer;

/**
 * 0 — silence (grace days)
 * 1 — traces  (torches, signs, chest gifts, tunnels)
 * 2 — watching (stalking at fog edge)
 * 3 — contact  (fake player, chat)
 * 4 — hostility (glitches, aggressive entities)
 * 5 — finale   (the_static, endings)
 */
public final class Stage {
    private Stage() {}

    public static final int SILENCE = 0, TRACES = 1, WATCHING = 2, CONTACT = 3, HOSTILE = 4, FINALE = 5;
    private static final int[] THRESHOLDS = {150, 350, 600, 850};

    public static long worldDay(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L;
    }

    public static int compute(MinecraftServer server, WorldHorrorData world) {
        if ("released".equals(world.ending())) return SILENCE; // good ending: it's quiet now, forever
        if ("erased".equals(world.ending())) return FINALE;    // bad ending: it never stops
        if (worldDay(server) < HSConfig.SERVER.graceDays.get()) return SILENCE;
        int dread = world.globalDread();
        int stage = TRACES;
        for (int t : THRESHOLDS) {
            if (dread >= t) stage++;
        }
        return stage;
    }
}
