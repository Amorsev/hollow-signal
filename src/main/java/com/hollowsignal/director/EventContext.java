package com.hollowsignal.director;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public record EventContext(ServerPlayer player, ServerLevel level, int stage, RandomSource random,
                           boolean underground, boolean dark, boolean alone) {

    public static EventContext of(ServerPlayer player, int stage) {
        ServerLevel level = player.serverLevel();
        return new EventContext(player, level, stage, level.getRandom(),
                WorldUtil.isUnderground(level, player.blockPosition()),
                WorldUtil.isDark(level, player.blockPosition()),
                WorldUtil.isAlone(player, 48));
    }
}
