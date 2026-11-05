package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Scheduler;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Sounds from right behind you — only you hear them, and nothing is there. */
public final class PhantomSoundEvent implements HorrorEvent {
    @Override public String id() { return "phantom_sound"; }
    @Override public int minStage() { return Stage.WATCHING; }
    @Override public long cooldownTicks() { return 3 * 60 * 20; }
    @Override public int dreadGain() { return 10; }
    @Override public int weight(EventContext ctx) { return 8; }

    @Override
    public boolean run(EventContext ctx) {
        ServerPlayer player = ctx.player();
        BlockPos start = WorldUtil.behind(player, 4, 8, 35);
        SoundEvent sound;
        int repeats = 1;
        switch (ctx.random().nextInt(6)) {
            case 0 -> { // footsteps that come closer
                BlockPos below = player.blockPosition().below();
                sound = ctx.level().getBlockState(below).getSoundType(ctx.level(), below, player).getStepSound();
                repeats = 4 + ctx.random().nextInt(3);
            }
            case 1 -> sound = SoundEvents.WOODEN_DOOR_OPEN;
            case 2 -> sound = SoundEvents.CHEST_OPEN;
            case 3 -> sound = SoundEvents.STONE_BREAK;
            case 4 -> { sound = SoundEvents.GENERIC_EAT; repeats = 3; }
            default -> sound = SoundEvents.PLAYER_BREATH;
        }
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        UUID id = player.getUUID();
        Vec3 from = Vec3.atBottomCenterOf(start);
        for (int i = 0; i < repeats; i++) {
            double t = repeats > 1 ? (double) i / repeats * 0.7 : 0; // each step a bit closer
            Scheduler.later(1 + i * 7, server -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) return;
                Vec3 pos = from.lerp(p.position(), t);
                p.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, pos.x, p.getY(), pos.z,
                        0.7f, 0.9f + p.getRandom().nextFloat() * 0.2f, p.getRandom().nextLong()));
            });
        }
        return true;
    }
}
