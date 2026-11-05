package com.hollowsignal.world;

import com.hollowsignal.director.PlayerHorrorData;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The finale arena in the_static: a dark central platform with the core, and three receivers on
 * islands around it, reachable over broken bridges (single-block gaps you have to jump).
 */
public final class StaticArena {
    private StaticArena() {}

    public static final int RECEIVERS = 3;
    public static final BlockPos CENTER = new BlockPos(0, 100, 0);
    private static final int RADIUS = 28;
    private static final int[] ISLAND_DY = {-3, 2, 5};

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(ModWorld.THE_STATIC);
    }

    public static BlockPos receiverPos(int i) {
        double a = Math.toRadians(90 + i * 120);
        return new BlockPos((int) Math.round(Math.cos(a) * RADIUS), CENTER.getY() + ISLAND_DY[i], (int) Math.round(Math.sin(a) * RADIUS));
    }

    public static int activeReceivers(ServerLevel level) {
        int n = 0;
        for (int i = 0; i < RECEIVERS; i++) {
            BlockState s = level.getBlockState(receiverPos(i));
            if (s.is(ModWorld.RECEIVER.get()) && s.getValue(ReceiverBlock.ACTIVE)) n++;
        }
        return n;
    }

    /** Builds the arena if it isn't there yet (idempotent). */
    public static void ensureBuilt(ServerLevel level) {
        if (level.getBlockState(CENTER).is(ModWorld.CORE.get())) return;
        RandomSource r = RandomSource.create(1234L);
        // central platform
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            set(level, CENTER.offset(dx, -1, dz), (Math.abs(dx) == 3 || Math.abs(dz) == 3) ? Blocks.GRAY_CONCRETE : Blocks.BLACK_CONCRETE);
        }
        set(level, CENTER, ModWorld.CORE.get());
        // islands + broken bridges
        for (int i = 0; i < RECEIVERS; i++) {
            BlockPos rp = receiverPos(i);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                set(level, rp.offset(dx, -1, dz), r.nextFloat() < 0.3 ? Blocks.COBBLESTONE : Blocks.GRAY_CONCRETE);
            }
            level.setBlock(rp, ModWorld.RECEIVER.get().defaultBlockState(), Block.UPDATE_ALL);
            bridge(level, CENTER.below(), rp.below(), r);
        }
    }

    private static void bridge(ServerLevel level, BlockPos from, BlockPos to, RandomSource r) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        boolean lastGap = true;
        for (int s = 4; s <= (int) len - 3; s++) {
            double t = s / len;
            BlockPos p = new BlockPos((int) Math.round(from.getX() + dx * t),
                    (int) Math.round(Mth.lerp(t, from.getY(), to.getY())), (int) Math.round(from.getZ() + dz * t));
            boolean gap = !lastGap && r.nextFloat() < 0.22; // never two gaps in a row: always jumpable
            if (!gap) set(level, p, Blocks.GRAY_CONCRETE);
            lastGap = gap;
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Turns every receiver off again (after a loop / erased ending). */
    public static void reset(ServerLevel level) {
        for (int i = 0; i < RECEIVERS; i++) {
            BlockState s = level.getBlockState(receiverPos(i));
            if (s.is(ModWorld.RECEIVER.get())) level.setBlock(receiverPos(i), s.setValue(ReceiverBlock.ACTIVE, false), Block.UPDATE_ALL);
        }
    }

    /** Takes a player into the_static, remembering where they came from. */
    public static boolean enter(ServerPlayer player) {
        ServerLevel stat = level(player.getServer());
        if (stat == null || ModWorld.isStatic(player.level())) return false;
        PlayerHorrorData d = ModAttachments.of(player);
        d.setReturnPoint(player.level().dimension().location().toString(), player.blockPosition());
        ensureBuilt(stat);
        player.teleportTo(stat, CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 2.5, 180f, 0f);
        return true;
    }

    /** Sends a player back to where they were taken from (or world spawn). */
    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        PlayerHorrorData d = ModAttachments.of(player);
        ServerLevel target = null;
        BlockPos pos = null;
        if (d.returnDimension() != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.returnDimension()));
            target = server.getLevel(key);
            pos = d.returnPos();
        }
        if (target == null || pos == null) {
            target = server.overworld();
            pos = target.getSharedSpawnPos();
        }
        d.clearReturnPoint();
        player.fallDistance = 0;
        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0f);
    }
}
