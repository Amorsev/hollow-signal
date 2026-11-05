package com.hollowsignal.chat;

import com.hollowsignal.director.PlayerHorrorData;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldHorrorData;
import com.hollowsignal.net.FakeChatPayload;
import com.hollowsignal.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The fake "player" in chat. Each real player has their own contact session:
 * the join/leave lines and every message are sent only to that player (split reality in multiplayer).
 */
public final class ContactManager {
    private ContactManager() {}

    /** Persistent-data flag on block entities the entity created (e.g. signs). */
    public static final String TRACE_TAG = "hollowsignal_trace";

    private record Session(long onlineUntil) {}
    private record Pending(UUID player, long due, FakeChatPayload payload) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REPLY = new HashMap<>();
    private static final Map<UUID, Long> LAST_GIFT = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    // ---------- public API used by events / commands ----------

    public static boolean isOnline(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** The entity "joins the game" for this player only, then greets them after a few seconds. */
    public static void join(ServerPlayer player, int durationTicks) {
        MinecraftServer server = player.getServer();
        long now = server.overworld().getGameTime();
        String name = name(server);
        SESSIONS.put(player.getUUID(), new Session(now + durationTicks));
        FakeTabEntry.show(player, name);
        send(player, new FakeChatPayload(FakeChatPayload.JOIN, name, "", 0f));
        say(player, "greet", 60 + player.getRandom().nextInt(100));
    }

    public static void leave(ServerPlayer player) {
        if (SESSIONS.remove(player.getUUID()) != null) sendLeave(player);
    }

    private static void sendLeave(ServerPlayer player) {
        String name = name(player.getServer());
        FakeTabEntry.hide(player, name);
        send(player, new FakeChatPayload(FakeChatPayload.LEAVE, name, "", 0f));
    }

    /** Queue a line from a dialogue topic, picked by this player's relationship band. */
    public static boolean say(ServerPlayer player, String topicId, int delayTicks) {
        return say(player, topicId, delayTicks, false);
    }

    /** @param cursed write the line in the enchanting-table glyph font */
    public static boolean say(ServerPlayer player, String topicId, int delayTicks, boolean cursed) {
        Dialogue.Topic topic = Dialogue.INSTANCE.topic(topicId);
        if (topic == null) return false;
        PlayerHorrorData data = ModAttachments.of(player);
        String key = topic.pick(Dialogue.band(data.relationship()), player.getRandom());
        if (key == null) return false;
        int kind = "system".equals(topicId) ? FakeChatPayload.SYSTEM
                : cursed ? FakeChatPayload.CURSED : FakeChatPayload.CHAT;
        MinecraftServer server = player.getServer();
        PENDING.add(new Pending(player.getUUID(), server.overworld().getGameTime() + delayTicks,
                new FakeChatPayload(kind, name(server), key, distortion(player))));
        return true;
    }

    /** A line from a topic, but shown as if another player (e.g. a mimicked friend) wrote it. */
    public static boolean sayAs(ServerPlayer player, String senderName, String topicId, int delayTicks) {
        Dialogue.Topic topic = Dialogue.INSTANCE.topic(topicId);
        if (topic == null) return false;
        String key = topic.pick(Dialogue.band(ModAttachments.of(player).relationship()), player.getRandom());
        if (key == null) return false;
        MinecraftServer server = player.getServer();
        PENDING.add(new Pending(player.getUUID(), server.overworld().getGameTime() + delayTicks,
                new FakeChatPayload(FakeChatPayload.CHAT, senderName, key, 0f)));
        return true;
    }

    public static String name(MinecraftServer server) {
        return WorldHorrorData.get(server).entityName(server);
    }

    /** More corruption when the entity is hostile or the world is deep into the story. */
    private static float distortion(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        int stage = Stage.compute(server, WorldHorrorData.get(server));
        float d = 0f;
        if (Dialogue.band(ModAttachments.of(player).relationship()) == Dialogue.Band.HOSTILE) d += 0.12f;
        if (stage >= Stage.HOSTILE) d += 0.15f;
        if (stage >= Stage.FINALE) d += 0.15f;
        return d;
    }

    private static void send(ServerPlayer player, FakeChatPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void changeRelationship(ServerPlayer player, int delta) {
        if (delta == 0) return;
        PlayerHorrorData d = ModAttachments.of(player);
        d.setRelationship(d.relationship() + delta);
    }

    // ---------- event hooks ----------

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();

        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.due() > now) continue;
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(p.player());
            if (player != null) send(player, p.payload());
        }

        if (now % 20 != 0) return;
        for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (e.getValue().onlineUntil() > now) continue;
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player != null) sendLeave(player);
        }
    }

    /** The entity answers what the player writes in chat (only to them). */
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String text = event.getRawText();
        MinecraftServer server = player.getServer();
        server.execute(() -> handleChat(player, text));
    }

    private static void handleChat(ServerPlayer player, String text) {
        MinecraftServer server = player.getServer();
        long now = server.overworld().getGameTime();
        String name = name(server);
        boolean mentioned = text.toLowerCase().contains(name.toLowerCase());
        RandomSource random = player.getRandom();

        if (!isOnline(player)) {
            // Not "online" — but say its name late in the story and it may still answer.
            int stage = Stage.compute(server, WorldHorrorData.get(server));
            if (!(mentioned && stage >= Stage.CONTACT && random.nextInt(3) == 0)) return;
        }
        if (now - LAST_REPLY.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < 60) return;
        LAST_REPLY.put(player.getUUID(), now);

        ModAttachments.of(player).addDread(2);
        int typingDelay = 40 + random.nextInt(80); // "typing..."
        Dialogue.Topic topic = Dialogue.INSTANCE.match(text);
        if (topic == null) {
            // Not something it has words for: it answers in the cursed glyph font.
            say(player, "unknown", typingDelay, true);
            return;
        }
        changeRelationship(player, topic.relDelta());
        say(player, topic.id(), typingDelay);
    }

    /** Throwing an item while it's online counts as a gift. */
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isOnline(player)) return;
        long now = player.getServer().overworld().getGameTime();
        if (now - LAST_GIFT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < 60 * 20) return;
        LAST_GIFT.put(player.getUUID(), now);
        Dialogue.Topic gift = Dialogue.INSTANCE.topic("gift");
        if (gift != null) changeRelationship(player, gift.relDelta());
        say(player, "gift", 40 + player.getRandom().nextInt(60));
        // It takes the gift: the item never lands in the world.
        event.setCanceled(true);
    }

    /** Breaking something it made upsets it. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be == null || !be.getPersistentData().getBoolean(TRACE_TAG)) return;
        Dialogue.Topic t = Dialogue.INSTANCE.topic("sign_broken");
        if (t != null) changeRelationship(player, t.relDelta());
        if (isOnline(player) || player.getRandom().nextInt(3) == 0) say(player, "sign_broken", 30 + player.getRandom().nextInt(60));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SESSIONS.remove(id);
        LAST_REPLY.remove(id);
        LAST_GIFT.remove(id);
        PENDING.removeIf(p -> p.player().equals(id));
    }

    /** Clear everything when the server stops (singleplayer worlds share the JVM). */
    public static void clear() {
        SESSIONS.clear();
        LAST_REPLY.clear();
        LAST_GIFT.clear();
        PENDING.clear();
    }
}
