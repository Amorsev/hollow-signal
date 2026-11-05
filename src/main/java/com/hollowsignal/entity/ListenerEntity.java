package com.hollowsignal.entity;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.net.ForceLookPayload;
import com.hollowsignal.net.GlitchPayload;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Listener. Never fights fair, never stays:
 * STALK  — stands far away and stares; vanishes when stared back at or approached.
 * GLANCE — appears behind you, your head is turned towards it, it's gone a moment later.
 * CREEP  — walks up behind you while you don't look; looking makes it vanish.
 * Never saved to disk: every appearance is temporary.
 */
public class ListenerEntity extends PathfinderMob {
    /** MIMIC — stands far away wearing a friend's name and skin (multiplayer). */
    public enum Mode { STALK, GLANCE, CREEP, MIMIC }

    /** UUID of the player whose skin we wear (MIMIC); synced so the client renderer can use it. */
    private static final EntityDataAccessor<Optional<UUID>> MIMIC_OF =
            SynchedEntityData.defineId(ListenerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private Mode mode = Mode.STALK;
    @Nullable private UUID targetId;
    private int life;
    private int maxLife = 60 * 20;
    private int watchedTicks;
    /** Split reality: if true, only the victim's client even knows this entity exists. */
    private boolean privateToTarget;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MIMIC_OF, Optional.empty());
    }

    public Optional<UUID> mimicOf() {
        return entityData.get(MIMIC_OF);
    }

    /** Wear another player's name and skin. */
    public void setMimic(ServerPlayer friend) {
        entityData.set(MIMIC_OF, Optional.of(friend.getUUID()));
        setCustomName(Component.literal(friend.getGameProfile().getName()));
        setCustomNameVisible(true);
    }

    /** The server only sends this entity to the players allowed to see it. */
    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        if (privateToTarget && targetId != null) return player.getUUID().equals(targetId);
        return super.broadcastToPlayer(player);
    }

    /** Players who can see us: just the victim when private, otherwise everyone in the level. */
    private List<ServerPlayer> viewers(ServerLevel server) {
        if (privateToTarget && targetId != null) {
            ServerPlayer t = server.getPlayerByUUID(targetId) instanceof ServerPlayer sp ? sp : null;
            return t == null ? List.of() : List.of(t);
        }
        return server.players();
    }

    public ListenerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    public void setup(ServerPlayer target, Mode mode, int maxLifeTicks) {
        this.targetId = target.getUUID();
        this.mode = mode;
        this.maxLife = maxLifeTicks;
        // Scares and impersonation are for the victim's eyes only; a distant stalker can be seen by anyone.
        this.privateToTarget = mode != Mode.STALK;
    }

    public boolean isPrivateToTarget() { return privateToTarget; }

    @Nullable
    public UUID targetId() { return targetId; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) return;
        life++;

        ServerPlayer target = resolveTarget(server);
        if (target == null || life > maxLife) {
            vanish(false);
            return;
        }
        getLookControl().setLookAt(target, 180f, 90f);

        switch (mode) {
            case STALK, MIMIC -> tickStalk(server, target);
            case GLANCE -> tickGlance(target);
            case CREEP -> tickCreep(server, target);
        }
        if (!isRemoved() && mode != Mode.GLANCE && life % 40 == 0) animalsReact(server);
    }

    private void tickStalk(ServerLevel server, ServerPlayer target) {
        getNavigation().stop();
        for (ServerPlayer p : viewers(server)) {
            if (p.isSpectator()) continue;
            if (p.distanceToSqr(this) < 14 * 14) { glitch(p); vanish(false); return; }
        }
        if (anyoneLooking(server)) {
            if (++watchedTicks > 12) {
                glitchLookers(server);
                vanish(false);
            }
        } else {
            watchedTicks = Math.max(0, watchedTicks - 1);
        }
    }

    private void tickGlance(ServerPlayer target) {
        getNavigation().stop();
        if (life == 2) scare(target);
        if (life >= 35) vanish(false);
    }

    private void tickCreep(ServerLevel server, ServerPlayer target) {
        if (anyoneLooking(server)) { glitchLookers(server); vanish(false); return; }
        getNavigation().moveTo(target, 0.55);
        if (distanceToSqr(target) < 2.2 * 2.2) {
            scare(target);
            if (HSConfig.SERVER.listenerCanAttack.get()) {
                target.hurt(damageSources().mobAttack(this), HSConfig.SERVER.listenerAttackDamage.get().floatValue());
            }
            vanish(false);
        }
    }

    /** Turns the victim's camera to us and plays the sting — only for the victim. */
    private void scare(ServerPlayer target) {
        PacketDistributor.sendToPlayer(target, new ForceLookPayload(getX(), getEyeY(), getZ(), 12));
        playTo(target, ModSounds.LISTENER_SCARE.get(), 1.0f);
        glitch(target);
    }

    /** The Listener's signature on every encounter: light screen interference + quiet static. */
    public static void glitch(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new GlitchPayload(28, 1.0f, true));
    }

    /** Everyone who is looking at us right now gets the interference. */
    private void glitchLookers(ServerLevel server) {
        for (ServerPlayer p : viewers(server)) {
            if (!p.isSpectator() && p.distanceToSqr(this) < 128 * 128 && isLookedAtBy(p)) glitch(p);
        }
    }

    private static void playTo(ServerPlayer player, SoundEvent sound, float volume) {
        player.playNotifySound(sound, SoundSource.HOSTILE, volume, 1.0f);
    }

    public boolean isLookedAtBy(Player p) {
        Vec3 look = p.getViewVector(1.0f).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double d = to.length();
        if (d < 1e-3) return true;
        double halfAngle = Math.atan2(1.2, d) + Math.toRadians(4); // body size + a little slack
        return look.dot(to.scale(1 / d)) > Math.cos(halfAngle) && p.hasLineOfSight(this);
    }

    private boolean anyoneLooking(ServerLevel server) {
        for (ServerPlayer p : viewers(server)) {
            if (!p.isSpectator() && p.distanceToSqr(this) < 128 * 128 && isLookedAtBy(p)) return true;
        }
        return false;
    }

    private void animalsReact(ServerLevel server) {
        var box = getBoundingBox().inflate(24);
        for (Wolf w : server.getEntitiesOfClass(Wolf.class, box)) {
            w.getLookControl().setLookAt(this);
            w.playSound(SoundEvents.WOLF_GROWL, 1.0f, 0.8f + random.nextFloat() * 0.2f);
        }
        for (Cat c : server.getEntitiesOfClass(Cat.class, box)) {
            c.getLookControl().setLookAt(this);
            c.playSound(SoundEvents.CAT_HISS, 1.0f, 1.0f);
        }
    }

    @Nullable
    private ServerPlayer resolveTarget(ServerLevel server) {
        if (targetId == null) {
            // Spawned by egg / command: adopt the nearest player.
            Player p = server.getNearestPlayer(this, 64);
            if (p instanceof ServerPlayer sp) targetId = sp.getUUID();
            else return null;
        }
        Player p = server.getPlayerByUUID(targetId);
        return p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator() ? sp : null;
    }

    public void vanish(boolean withSound) {
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 16, 0.25, 0.7, 0.25, 0.01);
        if (withSound) server.playSound(null, getX(), getY(), getZ(), ModSounds.LISTENER_VANISH.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide) {
            if (source.getEntity() instanceof ServerPlayer attacker) {
                var data = ModAttachments.of(attacker);
                data.setRelationship(data.relationship() - 10);
                glitch(attacker);
                vanish(false);
            } else {
                vanish(true);
            }
        }
        return false;
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(net.minecraft.world.entity.Entity entity) {}
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
}
