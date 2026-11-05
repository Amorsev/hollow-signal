package com.hollowsignal.world;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.chat.Dialogue;
import com.hollowsignal.director.PlayerHorrorData;
import com.hollowsignal.director.Scheduler;
import com.hollowsignal.director.WorldHorrorData;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.net.ScreenEffectPayload;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModWorld;
import com.hollowsignal.director.events.FourthWallEvents;
import com.hollowsignal.net.OsActionPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The three endings, decided by the GROUP's relationship (average over everyone in the_static):
 *  RELEASED (≥ 30)  — it lets you go; the world is quiet forever.
 *  LOOP     (-30..30) — everything starts over, a little worse each time.
 *  ERASED   (≤ -30) — your record is erased: a (fake, in SAFE mode) crash, and it never leaves.
 */
public final class Endings {
    private Endings() {}

    public enum Type { RELEASED, LOOP, ERASED }

    public static final int ENDING_TICKS = 16 * 20;
    private static boolean running;

    public static Type decide(List<ServerPlayer> group) {
        if (group.isEmpty()) return Type.LOOP;
        int sum = 0;
        for (ServerPlayer p : group) sum += ModAttachments.of(p).relationship();
        return switch (Dialogue.band(sum / group.size())) {
            case FRIENDLY -> Type.RELEASED;
            case HOSTILE -> Type.ERASED;
            default -> Type.LOOP;
        };
    }

    public static void trigger(ServerLevel staticLevel) {
        List<ServerPlayer> group = new ArrayList<>();
        for (ServerPlayer p : staticLevel.players()) if (!p.isSpectator()) group.add(p);
        trigger(staticLevel.getServer(), group, decide(group));
    }

    public static void trigger(MinecraftServer server, List<ServerPlayer> group, Type type) {
        if (running || group.isEmpty()) return;
        running = true;
        HollowSignal.LOGGER.info("Ending {} for {} player(s)", type, group.size());

        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer p : group) ids.add(p.getUUID());

        if (type == Type.ERASED) {
            // Everything is applied (and the players sent home) BEFORE the crash, so in HARDCORE the
            // world is saved in its final state when the client leaves and closes the game.
            finish(server, ids, type);
            for (ServerPlayer p : group) {
                ListenerEntity.glitch(p);
                FourthWallEvents.send(p, OsActionPayload.CRASH, 0);
            }
            return;
        }

        for (ServerPlayer p : group) {
            ListenerEntity.glitch(p);
            PacketDistributor.sendToPlayer(p, new ScreenEffectPayload(ScreenEffectPayload.ENDING, ENDING_TICKS, type.ordinal()));
        }
        Scheduler.later(ENDING_TICKS, s -> finish(s, ids, type));
    }

    private static void finish(MinecraftServer s, List<UUID> ids, Type type) {
        running = false;
        WorldHorrorData world = WorldHorrorData.get(s);
        for (UUID id : ids) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) continue;
            ContactManager.leave(p);
            apply(p, type);
            if (ModWorld.isStatic(p.level())) StaticArena.leave(p);
        }
        switch (type) {
            case RELEASED -> world.setEnding("released");
            case ERASED -> world.setEnding("erased");
            case LOOP -> {
                world.setEnding("");
                world.addLoop();
                // Starts over — but each loop begins a bit further along.
                world.setGlobalDread(Math.min(300, 60 * world.loops()));
            }
        }
        ServerLevel stat = StaticArena.level(s);
        if (stat != null && type != Type.RELEASED) StaticArena.reset(stat);
    }

    private static void apply(ServerPlayer p, Type type) {
        PlayerHorrorData d = ModAttachments.of(p);
        switch (type) {
            case RELEASED -> {
                d.setDread(0);
                ItemStack keepsake = new ItemStack(Items.PAPER);
                keepsake.set(DataComponents.CUSTOM_NAME, Component.translatable("hollowsignal.ending.keepsake.name"));
                keepsake.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("hollowsignal.ending.keepsake.lore"))));
                if (!p.getInventory().add(keepsake)) p.drop(keepsake, false);
            }
            case LOOP -> {
                d.setDread(0);
                d.setRelationship(0);
            }
            case ERASED -> {
                d.setDread(PlayerHorrorData.MAX_DREAD);
                d.setRelationship(-100);
            }
        }
    }

    /** The scheduled finish is dropped when the server stops; forget we were running. */
    public static void reset() {
        running = false;
    }
}
