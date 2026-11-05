package com.hollowsignal.world;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Scheduler;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.events.StalkEvent;
import com.hollowsignal.entity.HollowEntity;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.net.ScreenEffectPayload;
import com.hollowsignal.registry.ModWorld;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/** Ways into the_static: the finale "call" event, and being killed by the Hollow. */
public final class StaticEntry {
    private StaticEntry() {}

    /** Glitch → fake loading screen → you wake up in the_static. */
    public static void pull(ServerPlayer player) {
        if (ModWorld.isStatic(player.level())) return;
        ListenerEntity.glitch(player);
        PacketDistributor.sendToPlayer(player, new ScreenEffectPayload(ScreenEffectPayload.FAKE_LOADING, 80, 1));
        UUID id = player.getUUID();
        Scheduler.later(40, server -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && StaticArena.enter(p)) {
                ContactManager.say(p, "static_arrive", 100);
                // Something is already waiting out there.
                Scheduler.later(200, s2 -> {
                    ServerPlayer p2 = s2.getPlayerList().getPlayer(id);
                    if (p2 != null && ModWorld.isStatic(p2.level())) {
                        StalkEvent.spawnStalk(EventContext.of(p2, Stage.FINALE), 20, 30, 60 * 20);
                    }
                });
            }
        });
    }

    /** Killed by the Hollow (stage 4+) → you don't die; you're taken. */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getSource().getEntity() instanceof HollowEntity)) return;
        if (!HSConfig.SERVER.hollowDeathTakesToStatic.get() || ModWorld.isStatic(player.level())) return;
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth() / 2);
        player.removeAllEffects();
        player.clearFire();
        event.getSource().getEntity().discard();
        pull(player);
    }

    /** Finale event: "the call". */
    public static final class CallEvent implements HorrorEvent {
        @Override public String id() { return "the_call"; }
        @Override public int minStage() { return Stage.FINALE; }
        @Override public long cooldownTicks() { return 30 * 60 * 20; }
        @Override public int dreadGain() { return 0; }
        @Override public int weight(EventContext ctx) { return ModWorld.isStatic(ctx.level()) ? 0 : 6; }

        @Override
        public boolean run(EventContext ctx) {
            if (ModWorld.isStatic(ctx.level()) || StaticArena.level(ctx.player().getServer()) == null) return false;
            ContactManager.say(ctx.player(), "the_call", 0);
            pull(ctx.player());
            return true;
        }
    }
}
