package com.hollowsignal.entity;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.net.ScreenEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Hollow — the only one that fights. It hunts its victim, digs through walls to reach them,
 * and while it's close your hands won't obey (no inventory, no chests). It can be killed —
 * and it can't stand bright light: torches are your defence.
 */
public class HollowEntity extends SignalMob {
    private static final int BRIGHT_LIGHT = 11;
    private int stuckTicks;
    private boolean announced;

    public HollowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    protected void serverTick(ServerLevel level, ServerPlayer target) {
        setTarget(target);
        if (!announced) { announced = true; ListenerEntity.glitch(target); }

        // Bright light burns it away.
        if (level.getBrightness(LightLayer.BLOCK, blockPosition()) >= BRIGHT_LIGHT
                || level.getBrightness(LightLayer.BLOCK, blockPosition().above()) >= BRIGHT_LIGHT) {
            level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
            if (distanceToSqr(target) < 48 * 48) ListenerEntity.glitch(target);
            vanish(true);
            return;
        }

        // Close by: your hands won't obey.
        if (life % 20 == 0) {
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || p.distanceToSqr(this) > 10 * 10) continue;
                PacketDistributor.sendToPlayer(p, new ScreenEffectPayload(ScreenEffectPayload.GUI_BLOCK, 40, 0));
                if (p.containerMenu != p.inventoryMenu) p.closeContainer();
            }
        }

        // Dig towards the victim when the path is blocked.
        if (getNavigation().isDone() && distanceToSqr(target) > 2.5 * 2.5) stuckTicks++;
        else if (getNavigation().isStuck()) stuckTicks += 2;
        else stuckTicks = 0;
        if (stuckTicks > 30 && life % 10 == 0) digTowards(level, target);
    }

    private void digTowards(ServerLevel level, ServerPlayer target) {
        if (!HSConfig.SERVER.allowWorldEdits.get() || !HSConfig.SERVER.hollowBreaksBlocks.get()
                || !EventHooks.canEntityGrief(level, this)) return;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos front = blockPosition().relative(dir);
        int dy = (int) Math.signum(Math.round(target.getY() - getY()));
        boolean broke = false;
        for (BlockPos p : new BlockPos[]{front.above(dy > 0 ? 1 : 0), front.above(dy > 0 ? 2 : 1), dy < 0 ? front.below() : null, dy > 0 ? blockPosition().above(2) : null}) {
            if (p != null && breakable(level, p)) {
                level.destroyBlock(p, true, this);
                broke = true;
            }
        }
        if (broke) stuckTicks = 0;
    }

    private static boolean breakable(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity()) return false;
        float hardness = s.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= 3.0f;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        if (!HSConfig.SERVER.hollowCanAttack.get()) return false;
        return super.doHurtTarget(entity);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer p) ListenerEntity.glitch(p);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // drops nothing
    }
}
