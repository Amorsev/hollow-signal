package com.hollowsignal.entity;

import com.hollowsignal.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base for the secondary "signal" entities: bound to one victim, temporary, never saved,
 * and every encounter ends with the Listener's interference (screen static + quiet noise).
 */
public abstract class SignalMob extends PathfinderMob {
    @Nullable protected UUID targetId;
    protected int life;
    protected int maxLife = 60 * 20;

    protected SignalMob(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setup(ServerPlayer target, int maxLifeTicks) {
        this.targetId = target.getUUID();
        this.maxLife = maxLifeTicks;
    }

    @Nullable
    public UUID targetId() { return targetId; }

    /** Called every server tick with a valid, living victim. */
    protected abstract void serverTick(ServerLevel level, ServerPlayer target);

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || isRemoved() || isDeadOrDying()) return;
        life++;
        ServerPlayer target = resolveTarget(server);
        if (target == null || life > maxLife) {
            vanish(false);
            return;
        }
        serverTick(server, target);
    }

    @Nullable
    protected ServerPlayer resolveTarget(ServerLevel server) {
        if (targetId == null) {
            Player p = server.getNearestPlayer(this, 64);
            if (p instanceof ServerPlayer sp) targetId = sp.getUUID();
            else return null;
        }
        Player p = server.getPlayerByUUID(targetId);
        return p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator() ? sp : null;
    }

    public boolean isLookedAtBy(Player p) {
        Vec3 look = p.getViewVector(1.0f).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double d = to.length();
        if (d < 1e-3) return true;
        double halfAngle = Math.atan2(getBbHeight() * 0.65, d) + Math.toRadians(4);
        return look.dot(to.scale(1 / d)) > Math.cos(halfAngle) && p.hasLineOfSight(this);
    }

    protected void glitchLookers(ServerLevel server) {
        for (ServerPlayer p : server.players()) {
            if (!p.isSpectator() && p.distanceToSqr(this) < 128 * 128 && isLookedAtBy(p)) ListenerEntity.glitch(p);
        }
    }

    public void vanish(boolean withSound) {
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 16, 0.25, getBbHeight() / 3, 0.25, 0.01);
        if (withSound) server.playSound(null, getX(), getY(), getZ(), ModSounds.LISTENER_VANISH.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        discard();
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    /** Generic spawn helper: places, faces the victim, binds it and adds it to the world. */
    @Nullable
    public static <T extends SignalMob> T spawn(EntityType<T> type, ServerPlayer target, BlockPos pos, int lifeTicks) {
        ServerLevel level = target.serverLevel();
        T e = type.create(level);
        if (e == null) return null;
        float yaw = com.hollowsignal.director.WorldUtil.yawTowards(Vec3.atBottomCenterOf(pos), target.position());
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setup(target, lifeTicks);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(e);
        return e;
    }

    /** True if this victim already has a live entity of this type bound to them. */
    public static boolean hasFor(EntityType<? extends SignalMob> type, ServerPlayer player) {
        return !player.serverLevel().getEntities(type, player.getBoundingBox().inflate(160),
                e -> player.getUUID().equals(e.targetId())).isEmpty();
    }
}
