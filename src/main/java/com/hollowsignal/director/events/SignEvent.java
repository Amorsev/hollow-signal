package com.hollowsignal.director.events;

import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;

/** A sign appears behind the player, facing them. Text is translatable (shown in each player's language). */
public final class SignEvent implements HorrorEvent {
    /** Number of lines in each message: hollowsignal.sign.<i>.<line>. */
    private static final int[] MESSAGES = {1, 2, 2, 2, 2, 2, 2, 1, 2, 2};

    @Override public String id() { return "sign"; }
    @Override public int minStage() { return Stage.TRACES; }
    @Override public long cooldownTicks() { return 10 * 60 * 20; }
    @Override public int dreadGain() { return 20; }

    @Override
    public int weight(EventContext ctx) {
        return HSConfig.SERVER.allowWorldEdits.get() ? 6 : 0;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (!HSConfig.SERVER.allowWorldEdits.get()) return false;
        ServerLevel level = ctx.level();
        for (int attempt = 0; attempt < 30; attempt++) {
            BlockPos base = WorldUtil.behind(ctx.player(), 5, 12, 70);
            BlockPos pos = WorldUtil.findStandingSpot(level, base, 4);
            if (pos == null || !WorldUtil.unseenByAll(level, pos)) continue;

            // "Placed by someone standing where the player is" → front faces the player.
            float yaw = WorldUtil.yawTowards(ctx.player().position(), Vec3.atCenterOf(pos));
            BlockState state = Blocks.OAK_SIGN.defaultBlockState()
                    .setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(yaw + 180f));
            level.setBlock(pos, state, Block.UPDATE_ALL);
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;

            int msg = ctx.random().nextInt(MESSAGES.length);
            SignText text = new SignText();
            for (int line = 0; line < MESSAGES[msg]; line++) { // lines 1..2: vertically centred
                text = text.setMessage(line + 1,Component.translatable("hollowsignal.sign." + msg + "." + line));
            }
            sign.setText(text, true);
            sign.setWaxed(true); // the player can't edit it — only break it
            sign.getPersistentData().putBoolean(com.hollowsignal.chat.ContactManager.TRACE_TAG, true);
            sign.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            return true;
        }
        return false;
    }
}
