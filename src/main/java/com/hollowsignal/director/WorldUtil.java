package com.hollowsignal.director;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WorldUtil {
    private WorldUtil() {}

    public static boolean isUnderground(ServerLevel level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos) < 4;
    }

    public static boolean isDark(ServerLevel level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) < 7;
    }

    public static boolean isAlone(ServerPlayer player, double radius) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && !other.isSpectator() && other.distanceToSqr(player) < radius * radius) return false;
        }
        return true;
    }

    /**
     * True if NO player in the level can currently see this block:
     * it's outside everyone's ~60° view cone or occluded. Players closer than 4 blocks always "see" it.
     * Multiplayer-safe: the entity never acts in front of any witness.
     */
    public static boolean unseenByAll(ServerLevel level, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            Vec3 eye = p.getEyePosition();
            Vec3 to = target.subtract(eye);
            double dist = to.length();
            if (dist > 96) continue;
            if (dist < 4) return false;
            if (p.getLookAngle().dot(to.scale(1 / dist)) < 0.5) continue;
            BlockHitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) return false;
        }
        return true;
    }

    /** A position the player is facing away from: yaw + 180° ± spreadDeg. */
    public static BlockPos behind(ServerPlayer player, double minDist, double maxDist, float spreadDeg) {
        var r = player.getRandom();
        float yaw = player.getYRot() + 180f + (r.nextFloat() * 2 - 1) * spreadDeg;
        double d = minDist + r.nextDouble() * (maxDist - minDist);
        double rad = Math.toRadians(yaw);
        double x = player.getX() - Math.sin(rad) * d;
        double z = player.getZ() + Math.cos(rad) * d;
        return BlockPos.containing(x, player.getY(), z);
    }

    /** Finds air with a sturdy floor and air above, scanning ±range vertically around base. */
    public static BlockPos findStandingSpot(ServerLevel level, BlockPos base, int range) {
        for (int dy = range; dy >= -range; dy--) {
            BlockPos p = base.offset(0, dy, 0);
            if (isOpen(level, p) && isOpen(level, p.above())
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                return p;
            }
        }
        return null;
    }

    public static boolean isOpen(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    /** Yaw (degrees, MC convention) of the vector from -> to. */
    public static float yawTowards(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }
}
