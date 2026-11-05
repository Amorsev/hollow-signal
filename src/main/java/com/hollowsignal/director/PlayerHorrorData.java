package com.hollowsignal.director;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

/** Per-player hidden state. Stored on the player (survives death and relog). */
public final class PlayerHorrorData implements INBTSerializable<CompoundTag> {
    public static final int MAX_DREAD = 1000;

    private int dread;
    /** -100 (hostile) .. 100 (friendly). Used from stage 4 (chat) on. */
    private int relationship;
    private long lastEventTick = Long.MIN_VALUE / 2;
    private CompoundTag cooldowns = new CompoundTag();
    private BlockPos lastPos = BlockPos.ZERO;
    private int idleChecks;

    public int dread() { return dread; }
    public void setDread(int v) { dread = Mth.clamp(v, 0, MAX_DREAD); }
    public void addDread(int v) { setDread(dread + v); }

    public int relationship() { return relationship; }
    public void setRelationship(int v) { relationship = Mth.clamp(v, -100, 100); }

    public long lastEventTick() { return lastEventTick; }
    public void setLastEventTick(long t) { lastEventTick = t; }

    public long lastRun(String eventId) {
        return cooldowns.contains(eventId) ? cooldowns.getLong(eventId) : Long.MIN_VALUE / 2;
    }
    public void markRun(String eventId, long tick) { cooldowns.putLong(eventId, tick); }

    /** Returns how many director checks in a row the player has stood still. */
    public int updateIdle(BlockPos pos) {
        if (pos.distManhattan(lastPos) <= 1) idleChecks++;
        else idleChecks = 0;
        lastPos = pos;
        return idleChecks;
    }

    private String returnDimension;
    private BlockPos returnPos;

    public void setReturnPoint(String dimension, BlockPos pos) { returnDimension = dimension; returnPos = pos; }
    public String returnDimension() { return returnDimension; }
    public BlockPos returnPos() { return returnPos; }
    public void clearReturnPoint() { returnDimension = null; returnPos = null; }

    /** Director checks in a row the player has stood still (one check per director interval). */
    public int idleChecks() { return idleChecks; }

    public void reset() {
        dread = 0;
        relationship = 0;
        lastEventTick = Long.MIN_VALUE / 2;
        cooldowns = new CompoundTag();
        idleChecks = 0;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putInt("dread", dread);
        t.putInt("relationship", relationship);
        t.putLong("lastEvent", lastEventTick);
        t.put("cooldowns", cooldowns.copy());
        if (returnDimension != null && returnPos != null) {
            t.putString("returnDim", returnDimension);
            t.putLong("returnPos", returnPos.asLong());
        }
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        dread = t.getInt("dread");
        relationship = t.getInt("relationship");
        lastEventTick = t.contains("lastEvent") ? t.getLong("lastEvent") : Long.MIN_VALUE / 2;
        cooldowns = t.getCompound("cooldowns").copy();
        if (t.contains("returnDim")) {
            returnDimension = t.getString("returnDim");
            returnPos = BlockPos.of(t.getLong("returnPos"));
        }
    }
}
