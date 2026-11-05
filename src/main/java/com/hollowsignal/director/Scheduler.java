package com.hollowsignal.director;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Tiny server-thread task scheduler (tick-based). */
public final class Scheduler {
    private Scheduler() {}

    private record Task(long due, Consumer<MinecraftServer> action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    public static void later(int delayTicks, Consumer<MinecraftServer> action) {
        TASKS.add(new Task(now + Math.max(1, delayTicks), action));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.due() > now) return false;
            due.add(t);
            return true;
        });
        for (Task t : due) t.action().accept(event.getServer());
    }

    public static void clear() {
        TASKS.clear();
    }
}
