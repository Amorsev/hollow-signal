package com.hollowsignal.entity;

import com.hollowsignal.director.WorldUtil;
import com.hollowsignal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ListenerSpawner {
    private ListenerSpawner() {}

    /** Only one Listener per victim at a time. */
    public static boolean hasListenerFor(ServerPlayer player) {
        return !player.serverLevel().getEntities(ModEntities.LISTENER.get(),
                player.getBoundingBox().inflate(160), e -> player.getUUID().equals(e.targetId())).isEmpty();
    }

    @Nullable
    public static ListenerEntity spawn(ServerPlayer target, BlockPos pos, ListenerEntity.Mode mode, int lifeTicks) {
        ServerLevel level = target.serverLevel();
        ListenerEntity e = ModEntities.LISTENER.get().create(level);
        if (e == null) return null;
        float yaw = WorldUtil.yawTowards(Vec3.atBottomCenterOf(pos), target.position());
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setup(target, mode, lifeTicks);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(e);
        return e;
    }

    /** True if a standing Listener at pos would have clear line of sight to the player's eyes. */
    public static boolean canSeePlayerFrom(ServerPlayer player, BlockPos pos) {
        Vec3 eye = Vec3.atBottomCenterOf(pos).add(0, 1.62, 0);
        var hit = player.serverLevel().clip(new ClipContext(eye, player.getEyePosition(),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** A spot at the given angle from the player's look direction (degrees) and distance. */
    public static BlockPos at(ServerPlayer player, float angleFromLook, double dist) {
        double rad = Math.toRadians(player.getYRot() + angleFromLook);
        return BlockPos.containing(player.getX() - Math.sin(rad) * dist, player.getY(), player.getZ() + Math.cos(rad) * dist);
    }
}
