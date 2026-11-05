package com.hollowsignal.entity;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.director.HorrorDirector;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.HorrorEvents;
import com.hollowsignal.director.Scheduler;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldHorrorData;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A small, fast thing that runs away from you. If it escapes, it "reports" your location:
 * some time later something comes for you. Catch it (kill it) first and nothing happens.
 */
public class ReporterEntity extends SignalMob {
    private static final int ESCAPE_TICKS = 5 * 20;

    public ReporterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void serverTick(ServerLevel level, ServerPlayer target) {
        if (life == 1) ListenerEntity.glitch(target);
        // Flee from the victim.
        if (getNavigation().isDone() || life % 20 == 0) {
            Vec3 away = DefaultRandomPos.getPosAway(this, 16, 7, target.position());
            if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.5);
        }
        if (life >= ESCAPE_TICKS) report(level, target);
    }

    private void report(ServerLevel level, ServerPlayer target) {
        // A distorted chirp from where it escaped — only the victim hears it.
        target.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.LISTENER_STATIC.get()),
                SoundSource.HOSTILE, getX(), getY(), getZ(), 0.5f, 1.6f, random.nextLong()));
        var data = ModAttachments.of(target);
        data.addDread(25);

        // The report: something comes for the victim in 20–40 s.
        UUID id = target.getUUID();
        Scheduler.later(400 + random.nextInt(400), server -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) return;
            int stage = Stage.compute(server, WorldHorrorData.get(server));
            HorrorEvent e = HorrorEvents.get(stage >= Stage.HOSTILE ? "hollow" : "glance");
            if (e != null && !HorrorDirector.forceRun(e, p)) {
                HollowSignal.LOGGER.debug("Reported, but no spot for {}", e.id());
            }
        });
        vanish(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isDeadOrDying() && source.getEntity() instanceof ServerPlayer p) {
            ListenerEntity.glitch(p); // caught it — the report never goes out
        }
        return hurt;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // drops nothing
    }
}
