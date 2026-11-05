package com.hollowsignal.director.events;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.net.FakeChatPayload;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Events that only exist when friends play together. Each one targets ONE victim and is
 * invisible to everybody else (split reality).
 *  - mimic:       the Listener stands far away wearing a friend's name and skin, and whispers as them;
 *  - fake_leave:  "<friend> left the game" — and the friend vanishes from the victim's Tab list for a while;
 *  - echo:        a friend's recent chat message repeats, slightly wrong, a while later.
 */
public final class MultiplayerEvents {
    private MultiplayerEvents() {}

    public static List<HorrorEvent> all() {
        return List.of(new Mimic(), new FakeLeave(), new Echo());
    }

    /** Another online player in the same game, preferring ones far from the victim. */
    @Nullable
    static ServerPlayer pickFriend(ServerPlayer victim, RandomSource random, double preferFartherThan) {
        List<ServerPlayer> far = new ArrayList<>(), any = new ArrayList<>();
        for (ServerPlayer p : victim.getServer().getPlayerList().getPlayers()) {
            if (p == victim || p.isSpectator()) continue;
            any.add(p);
            if (p.level() != victim.level() || p.distanceToSqr(victim) > preferFartherThan * preferFartherThan) far.add(p);
        }
        List<ServerPlayer> pool = far.isEmpty() ? any : far;
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    static boolean multiplayer(EventContext ctx) {
        return ctx.player().getServer().getPlayerList().getPlayerCount() > 1;
    }

    // ------------------------------------------------------------------ mimic

    static final class Mimic implements HorrorEvent {
        @Override public String id() { return "mimic"; }
        @Override public int minStage() { return Stage.CONTACT; }
        @Override public long cooldownTicks() { return 20 * 60 * 20; }
        @Override public int dreadGain() { return 30; }

        @Override
        public int weight(EventContext ctx) {
            return HSConfig.SERVER.mimicEnabled.get() && multiplayer(ctx) ? 6 : 0;
        }

        @Override
        public boolean run(EventContext ctx) {
            if (!HSConfig.SERVER.mimicEnabled.get()) return false;
            ServerPlayer friend = pickFriend(ctx.player(), ctx.random(), 48);
            if (friend == null) return false;
            ListenerEntity e = StalkEvent.spawnPeripheral(ctx, 22, 40, 50 * 20, ListenerEntity.Mode.MIMIC);
            if (e == null) return false;
            e.setMimic(friend);
            ContactManager.sayAs(ctx.player(), friend.getGameProfile().getName(), "mimic", 60 + ctx.random().nextInt(80));
            return true;
        }
    }

    // ------------------------------------------------------------------ fake leave

    private record Faked(UUID friend, String friendName, long until) {}
    private static final Map<UUID, Faked> FAKED = new HashMap<>();

    static final class FakeLeave implements HorrorEvent {
        @Override public String id() { return "fake_leave"; }
        @Override public int minStage() { return Stage.CONTACT; }
        @Override public long cooldownTicks() { return 30 * 60 * 20; }
        @Override public int dreadGain() { return 20; }

        @Override
        public int weight(EventContext ctx) {
            return HSConfig.SERVER.fakeLeaveEnabled.get() && multiplayer(ctx) && !FAKED.containsKey(ctx.player().getUUID()) ? 4 : 0;
        }

        @Override
        public boolean run(EventContext ctx) {
            ServerPlayer victim = ctx.player();
            if (!HSConfig.SERVER.fakeLeaveEnabled.get() || FAKED.containsKey(victim.getUUID())) return false;
            // Only a friend the victim's client isn't currently tracking — otherwise their body would vanish too.
            double safe = safeDistance(victim.getServer());
            ServerPlayer friend = pickFriend(victim, ctx.random(), safe);
            if (friend == null || (friend.level() == victim.level() && friend.distanceToSqr(victim) <= safe * safe)) return false;

            String name = friend.getGameProfile().getName();
            victim.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(friend.getUUID())));
            PacketDistributor.sendToPlayer(victim, new FakeChatPayload(FakeChatPayload.LEAVE, name, "", 0f));
            long until = victim.getServer().overworld().getGameTime() + (60 + ctx.random().nextInt(61)) * 20L;
            FAKED.put(victim.getUUID(), new Faked(friend.getUUID(), name, until));
            return true;
        }
    }

    /** Beyond the server's view distance (+2 chunks margin) the victim's client can't be tracking the friend. */
    private static double safeDistance(MinecraftServer server) {
        return (server.getPlayerList().getViewDistance() + 2) * 16.0;
    }

    private static void restore(MinecraftServer server, UUID victimId, Faked f) {
        ServerPlayer victim = server.getPlayerList().getPlayer(victimId);
        ServerPlayer friend = server.getPlayerList().getPlayer(f.friend());
        if (victim == null || friend == null) return; // friend really left meanwhile: nothing to restore
        victim.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(friend)));
        PacketDistributor.sendToPlayer(victim, new FakeChatPayload(FakeChatPayload.JOIN, f.friendName(), "", 0f));
    }

    // ------------------------------------------------------------------ echo

    private record Said(UUID who, String name, String text, long tick) {}
    private static final List<Said> RECENT = new ArrayList<>();

    static final class Echo implements HorrorEvent {
        @Override public String id() { return "echo"; }
        @Override public int minStage() { return Stage.CONTACT; }
        @Override public long cooldownTicks() { return 10 * 60 * 20; }
        @Override public int dreadGain() { return 10; }

        @Override
        public int weight(EventContext ctx) {
            return multiplayer(ctx) && pick(ctx) != null ? 4 : 0;
        }

        @Nullable
        private static Said pick(EventContext ctx) {
            long now = ctx.player().getServer().overworld().getGameTime();
            List<Said> pool = new ArrayList<>();
            for (Said s : RECENT) {
                if (!s.who().equals(ctx.player().getUUID()) && now - s.tick() < 5 * 60 * 20) pool.add(s);
            }
            return pool.isEmpty() ? null : pool.get(ctx.random().nextInt(pool.size()));
        }

        @Override
        public boolean run(EventContext ctx) {
            Said s = pick(ctx);
            if (s == null) return false;
            PacketDistributor.sendToPlayer(ctx.player(), new FakeChatPayload(FakeChatPayload.ECHO, s.name(), s.text(), 0.18f));
            return true;
        }
    }

    // ------------------------------------------------------------------ hooks

    /**
     * A signed chat message from a player the victim's client doesn't know gets the victim disconnected
     * ("chat validation failed"). So before a faked friend's message goes out, put them back first.
     * Runs synchronously, so the restore packets reach the victim before the chat packet.
     */
    private static void restoreFriend(ServerPlayer friend) {
        MinecraftServer server = friend.getServer();
        for (Iterator<Map.Entry<UUID, Faked>> it = FAKED.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (!e.getValue().friend().equals(friend.getUUID())) continue;
            it.remove();
            restore(server, e.getKey(), e.getValue());
        }
    }

    /** Commands like /msg, /me, /say also send signed chat — restore before any command from a faked friend. */
    public static void onCommand(net.neoforged.neoforge.event.CommandEvent event) {
        if (!FAKED.isEmpty() && event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer p) restoreFriend(p);
    }

    public static void onChat(ServerChatEvent event) {
        if (!FAKED.isEmpty()) restoreFriend(event.getPlayer());
        String text = event.getRawText();
        if (text.length() > 200) text = text.substring(0, 200);
        long tick = event.getPlayer().getServer().overworld().getGameTime();
        RECENT.add(new Said(event.getPlayer().getUUID(), event.getPlayer().getGameProfile().getName(), text, tick));
        while (RECENT.size() > 30) RECENT.remove(0);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now % 20 != 0 || FAKED.isEmpty()) return;
        double safe = safeDistance(server);
        for (Iterator<Map.Entry<UUID, Faked>> it = FAKED.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer victim = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer friend = server.getPlayerList().getPlayer(e.getValue().friend());
            // Restore early if they're about to come into view of each other.
            boolean closing = victim != null && friend != null && friend.level() == victim.level()
                    && friend.distanceToSqr(victim) < (safe - 32) * (safe - 32);
            if (now >= e.getValue().until() || closing || victim == null || friend == null) {
                it.remove();
                restore(server, e.getKey(), e.getValue());
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        FAKED.remove(id);
        RECENT.removeIf(s -> s.who().equals(id));
    }

    public static void clear() {
        FAKED.clear();
        RECENT.clear();
    }
}
