package com.hollowsignal.director.events;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/** Torches out of sight disappear, or turn into dim redstone torches. */
public final class TorchTheftEvent implements HorrorEvent {
    private static final int RADIUS = 20;

    @Override public String id() { return "torch_theft"; }
    @Override public int minStage() { return Stage.TRACES; }
    @Override public long cooldownTicks() { return 5 * 60 * 20; }
    @Override public int dreadGain() { return 15; }

    @Override
    public int weight(EventContext ctx) {
        if (!HSConfig.SERVER.allowWorldEdits.get()) return 0;
        return ctx.dark() ? 14 : 10;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (!HSConfig.SERVER.allowWorldEdits.get()) return false;
        ServerLevel level = ctx.level();
        BlockPos center = ctx.player().blockPosition();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS / 2; y <= RADIUS / 2; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    m.setWithOffset(center, x, y, z);
                    if (isTorch(level.getBlockState(m)) && WorldUtil.unseenByAll(level, m)) found.add(m.immutable());
                }
            }
        }
        if (found.isEmpty()) return false;

        int count = Math.min(found.size(), 1 + ctx.random().nextInt(3 + ctx.stage()));
        boolean replace = ctx.random().nextBoolean(); // one "signature" per event
        for (int i = 0; i < count; i++) {
            BlockPos pos = found.remove(ctx.random().nextInt(found.size()));
            BlockState s = level.getBlockState(pos);
            level.setBlock(pos, replace ? redstoneVersion(s) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    private static boolean isTorch(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH);
    }

    private static BlockState redstoneVersion(BlockState s) {
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, s.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return Blocks.REDSTONE_TORCH.defaultBlockState();
    }
}
