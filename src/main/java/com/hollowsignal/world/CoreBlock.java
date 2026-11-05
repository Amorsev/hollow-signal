package com.hollowsignal.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The core on the central platform. Works only when all receivers are on AND every player
 * in the_static stands next to it (co-op: you finish together). Then the ending plays.
 */
public class CoreBlock extends Block {
    public static final double GATHER_RADIUS = 8.0;

    public CoreBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            int active = StaticArena.activeReceivers(server);
            if (active < StaticArena.RECEIVERS) {
                sp.displayClientMessage(Component.translatable("hollowsignal.core.inactive", active, StaticArena.RECEIVERS), true);
            } else {
                int away = 0;
                for (ServerPlayer p : server.players()) {
                    if (!p.isSpectator() && p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > GATHER_RADIUS * GATHER_RADIUS) away++;
                }
                if (away > 0) {
                    sp.displayClientMessage(Component.translatable("hollowsignal.core.gather", away), true);
                } else {
                    Endings.trigger(server);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
