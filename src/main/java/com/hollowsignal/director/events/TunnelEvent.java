package com.hollowsignal.director.events;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A perfectly straight 2x1 tunnel appears in nearby stone/dirt, where the player isn't looking. */
public final class TunnelEvent implements HorrorEvent {
    @Override public String id() { return "tunnel"; }
    @Override public int minStage() { return Stage.TRACES; }
    @Override public long cooldownTicks() { return 20 * 60 * 20; }
    @Override public int dreadGain() { return 20; }

    @Override
    public int weight(EventContext ctx) {
        if (!HSConfig.SERVER.allowWorldEdits.get()) return 0;
        return ctx.underground() ? 9 : 3;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (!HSConfig.SERVER.allowWorldEdits.get()) return false;
        ServerLevel level = ctx.level();
        BlockPos origin = ctx.player().blockPosition();
        Vec3 look = ctx.player().getLookAngle();

        List<Direction> dirs = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (look.x * d.getStepX() + look.z * d.getStepZ() < 0.3) dirs.add(d); // away from the gaze
        }
        while (!dirs.isEmpty()) {
            Direction dir = dirs.remove(ctx.random().nextInt(dirs.size()));
            BlockPos start = findWall(level, origin, dir);
            if (start == null || !WorldUtil.unseenByAll(level, start)) continue;
            if (carve(level, start, dir, 8 + ctx.random().nextInt(11), ctx.random().nextInt(4) == 0) >= 4) return true;
        }
        return false;
    }

    private static BlockPos findWall(ServerLevel level, BlockPos origin, Direction dir) {
        BlockPos.MutableBlockPos p = origin.mutable();
        for (int i = 1; i <= 10; i++) {
            p.move(dir);
            if (carvable(level.getBlockState(p)) && carvable(level.getBlockState(p.above()))) return p.immutable();
            if (!WorldUtil.isOpen(level, p)) return null; // something else is in the way
        }
        return null;
    }

    /** @return number of carved segments. Stops before ever exposing fluids. */
    private static int carve(ServerLevel level, BlockPos start, Direction dir, int length, boolean endTorch) {
        BlockPos.MutableBlockPos p = start.mutable();
        int carved = 0;
        for (int i = 0; i < length; i++) {
            BlockPos head = p.above();
            if (!carvable(level.getBlockState(p)) || !carvable(level.getBlockState(head))) break;
            if (touchesFluid(level, p) || touchesFluid(level, head)) break;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(head, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            carved++;
            p.move(dir);
        }
        if (endTorch && carved >= 4) {
            BlockPos last = p.relative(dir.getOpposite());
            if (level.getBlockState(last.below()).isFaceSturdy(level, last.below(), Direction.UP)) {
                level.setBlock(last, Blocks.REDSTONE_TORCH.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return carved;
    }

    private static boolean carvable(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL);
    }

    private static boolean touchesFluid(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (!level.getFluidState(pos.relative(d)).isEmpty()) return true;
        }
        return false;
    }
}
