package com.hollowsignal.world;

import com.hollowsignal.chat.ContactManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One of three receivers on the islands of the_static. Right-click to turn it on. */
public class ReceiverBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ReceiverBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(ACTIVE)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            server.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.3, 0.5, 0.3, 0.05);
            int active = StaticArena.activeReceivers(server);
            for (ServerPlayer p : server.players()) {
                p.displayClientMessage(Component.translatable("hollowsignal.receiver.active", active, StaticArena.RECEIVERS), true);
            }
            ContactManager.say(sp, active >= StaticArena.RECEIVERS ? "core_ready" : "receiver", 20);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
