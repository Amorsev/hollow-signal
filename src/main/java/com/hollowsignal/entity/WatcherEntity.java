package com.hollowsignal.entity;

import com.hollowsignal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Watcher appears when you stand still for too long. It doesn't move, it just observes.
 * Keep standing still and it calls a Reporter. Look at it long enough, or walk away, and it's gone.
 */
public class WatcherEntity extends SignalMob {
    /** Ticks the victim must stay (almost) still while the Watcher exists before it calls a Reporter. */
    private static final int STILL_TO_REPORT = 15 * 20;

    private Vec3 anchor;
    private int stillTicks;
    private int watchedTicks;

    public WatcherEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void serverTick(ServerLevel level, ServerPlayer target) {
        getNavigation().stop();
        getLookControl().setLookAt(target, 180f, 90f);
        if (anchor == null) anchor = target.position();

        // Walked away: it loses interest.
        if (target.position().distanceToSqr(anchor) > 6 * 6) { vanish(true); return; }

        if (target.position().distanceToSqr(anchor) < 2 * 2) stillTicks++;
        else stillTicks = Math.max(0, stillTicks - 2);

        if (isLookedAtBy(target)) {
            if (++watchedTicks > 30) { glitchLookers(level); vanish(false); return; }
        } else {
            watchedTicks = Math.max(0, watchedTicks - 1);
        }

        if (stillTicks >= STILL_TO_REPORT) callReporter(level, target);
    }

    private void callReporter(ServerLevel level, ServerPlayer target) {
        // A short burst of "signal" particles and the Reporter steps out of it.
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.3, 0.8, 0.3, 0.05);
        BlockPos at = blockPosition();
        if (!SignalMob.hasFor(ModEntities.REPORTER.get(), target)) {
            SignalMob.spawn(ModEntities.REPORTER.get(), target, at, 20 * 20);
        }
        vanish(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide) {
            if (source.getEntity() instanceof ServerPlayer p) ListenerEntity.glitch(p);
            vanish(false);
        }
        return false;
    }

    @Override public boolean isPushable() { return false; }
}
