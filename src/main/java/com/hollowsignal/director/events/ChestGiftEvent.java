package com.hollowsignal.director.events;

import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** Something leaves an unsettling "gift" in one of the player's chests or barrels. */
public final class ChestGiftEvent implements HorrorEvent {
    private static final double RADIUS = 32;
    private static final Item[] GIFTS = {
            Items.REDSTONE_TORCH, Items.ROTTEN_FLESH, Items.PAPER, Items.BONE,
            Items.COMPASS, Items.POPPY, Items.STRING, Items.CLOCK
    };

    @Override public String id() { return "chest_gift"; }
    @Override public int minStage() { return Stage.TRACES; }
    @Override public long cooldownTicks() { return 15 * 60 * 20; }
    @Override public int dreadGain() { return 25; }
    @Override public int weight(EventContext ctx) { return 5; }

    @Override
    public boolean run(EventContext ctx) {
        ServerLevel level = ctx.level();
        BlockPos center = ctx.player().blockPosition();
        List<RandomizableContainerBlockEntity> containers = new ArrayList<>();
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)) continue;
                    RandomizableContainerBlockEntity c = (RandomizableContainerBlockEntity) be;
                    if (c.getLootTable() != null) continue; // untouched world-gen loot: not "the player's"
                    if (be.getBlockPos().distSqr(center) > RADIUS * RADIUS) continue;
                    if (!WorldUtil.unseenByAll(level, be.getBlockPos())) continue;
                    containers.add(c);
                }
            }
        }
        while (!containers.isEmpty()) {
            RandomizableContainerBlockEntity c = containers.remove(ctx.random().nextInt(containers.size()));
            List<Integer> empty = new ArrayList<>();
            for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) empty.add(i);
            if (empty.isEmpty()) continue;
            c.setItem(empty.get(ctx.random().nextInt(empty.size())), makeGift(ctx.random().nextInt(GIFTS.length)));
            c.setChanged();
            return true;
        }
        return false;
    }

    private static ItemStack makeGift(int i) {
        ItemStack stack = new ItemStack(GIFTS[i]);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("hollowsignal.gift." + i + ".name"));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("hollowsignal.gift." + i + ".lore"))));
        return stack;
    }
}
