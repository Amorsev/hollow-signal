package com.hollowsignal.director.events;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.net.OsActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Stage-8 fourth-wall events. The server only sends an intent; each client decides
 * (HARDCORE confirmed locally + toggle on + server allows → real; otherwise the SAFE version).
 */
public final class FourthWallEvents {
    private FourthWallEvents() {}

    public static void send(ServerPlayer player, int action, int variant) {
        PacketDistributor.sendToPlayer(player, new OsActionPayload(action, variant,
                ContactManager.name(player.getServer()), HSConfig.SERVER.allowHardcoreEffects.get()));
    }

    public static List<HorrorEvent> all() {
        return List.of(new FileDrop(), new WindowTitleEvent());
    }

    /** A note appears in one of the player's folders (HARDCORE) — or it "tries to" (SAFE). */
    static final class FileDrop implements HorrorEvent {
        @Override public String id() { return "file_drop"; }
        @Override public int minStage() { return Stage.CONTACT; }
        @Override public long cooldownTicks() { return 30 * 60 * 20; }
        @Override public int dreadGain() { return 20; }
        @Override public int weight(EventContext ctx) { return 3; }
        @Override public boolean run(EventContext ctx) {
            send(ctx.player(), OsActionPayload.FILE_DROP, ctx.random().nextInt(1000));
            return true;
        }
    }

    static final class WindowTitleEvent implements HorrorEvent {
        @Override public String id() { return "window_title"; }
        @Override public int minStage() { return Stage.WATCHING; }
        @Override public long cooldownTicks() { return 15 * 60 * 20; }
        @Override public int dreadGain() { return 5; }
        @Override public int weight(EventContext ctx) { return 2; }
        @Override public boolean run(EventContext ctx) {
            send(ctx.player(), OsActionPayload.WINDOW_TITLE, ctx.random().nextInt(1000));
            return true;
        }
    }
}
