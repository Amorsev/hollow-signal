package com.hollowsignal.director;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide shared state (stored with the overworld). Drives the stage for everyone. */
public final class WorldHorrorData extends SavedData {
    public static final int MAX_DREAD = 1000;
    private static final String NAME = "hollowsignal_world";

    private int globalDread;
    private long lastGlobalEventTick = Long.MIN_VALUE / 2;
    private String entityName = "";
    /** "" (story ongoing), "released", "loop" (cleared after it restarts), "erased". */
    private String ending = "";
    private int loops;

    public String ending() { return ending; }
    public void setEnding(String e) { ending = e; setDirty(); }
    public int loops() { return loops; }
    public void addLoop() { loops++; setDirty(); }

    private static final String[] NAMES = {
            "n0_signal", "h0llow", "st4tic", "listener_", "nobody", "______", "player_0", "lost_frequency", "quiet_one"
    };

    public static WorldHorrorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WorldHorrorData::new, WorldHorrorData::load, null), NAME);
    }

    private static WorldHorrorData load(CompoundTag t, HolderLookup.Provider provider) {
        WorldHorrorData d = new WorldHorrorData();
        d.globalDread = t.getInt("globalDread");
        d.lastGlobalEventTick = t.contains("lastEvent") ? t.getLong("lastEvent") : Long.MIN_VALUE / 2;
        d.entityName = t.getString("entityName");
        d.ending = t.getString("ending");
        d.loops = t.getInt("loops");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider provider) {
        t.putInt("globalDread", globalDread);
        t.putLong("lastEvent", lastGlobalEventTick);
        t.putString("entityName", entityName);
        t.putString("ending", ending);
        t.putInt("loops", loops);
        return t;
    }

    public int globalDread() { return globalDread; }
    public void setGlobalDread(int v) { globalDread = Mth.clamp(v, 0, MAX_DREAD); setDirty(); }
    public void addGlobalDread(int v) { setGlobalDread(globalDread + v); }

    /** The name the entity uses in chat. Chosen once per world. */
    public String entityName(MinecraftServer server) {
        if (entityName.isEmpty()) {
            var random = server.overworld().getRandom();
            for (int i = 0; i < 20; i++) {
                String candidate = NAMES[random.nextInt(NAMES.length)];
                if (server.getPlayerList().getPlayerByName(candidate) == null) { entityName = candidate; break; }
            }
            if (entityName.isEmpty()) entityName = "n0_signal";
            setDirty();
        }
        return entityName;
    }

    public void setEntityName(String name) { entityName = name; setDirty(); }

    public long lastGlobalEventTick() { return lastGlobalEventTick; }
    public void setLastGlobalEventTick(long t) { lastGlobalEventTick = t; setDirty(); }

    public void reset() {
        globalDread = 0;
        lastGlobalEventTick = Long.MIN_VALUE / 2;
        ending = "";
        setDirty();
    }
}
