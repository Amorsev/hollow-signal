package com.hollowsignal.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hollowsignal.HollowSignal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data-driven dialogue: data/<namespace>/dialogue/*.json
 * <pre>
 * { "topics": [
 *   { "id": "who", "keywords": ["who are you", "кто ты"], "rel_delta": 1,
 *     "replies": { "friendly": ["key", ...], "neutral": [...], "hostile": [...] } } ] }
 * </pre>
 * Replies are translation keys (the client translates them). Topics without keywords
 * (greet, idle, unknown, gift, sign_broken, system) are used by events.
 */
public final class Dialogue extends SimpleJsonResourceReloadListener {
    public enum Band { FRIENDLY, NEUTRAL, HOSTILE }

    public record Topic(String id, List<String> keywords, int relDelta, Map<Band, List<String>> replies) {
        @Nullable
        public String pick(Band band, RandomSource random) {
            List<String> list = replies.getOrDefault(band, List.of());
            if (list.isEmpty()) list = replies.getOrDefault(Band.NEUTRAL, List.of());
            return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
        }
    }

    private static final Gson GSON = new GsonBuilder().create();
    public static final Dialogue INSTANCE = new Dialogue();

    private Map<String, Topic> topics = Map.of();

    private Dialogue() {
        super(GSON, "dialogue");
    }

    public static Band band(int relationship) {
        if (relationship >= 30) return Band.FRIENDLY;
        if (relationship <= -30) return Band.HOSTILE;
        return Band.NEUTRAL;
    }

    @Nullable
    public Topic topic(String id) {
        return topics.get(id);
    }

    /** First topic (in file order) with a keyword contained in the message. */
    @Nullable
    public Topic match(String message) {
        // Whole-word phrase match on normalized text: "Как ТВОИ дела???" == "как твои дела".
        String m = " " + normalize(message) + " ";
        for (Topic t : topics.values()) {
            for (String k : t.keywords()) {
                if (m.contains(" " + k + " ")) return t;
            }
        }
        return null;
    }

    /** Lowercase, ё→е, punctuation/emoji → spaces, collapsed whitespace. */
    public static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean space = true;
        for (char c : s.toLowerCase(Locale.ROOT).replace('ё', 'е').toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                space = false;
            } else if (!space) {
                sb.append(' ');
                space = true;
            }
        }
        return sb.toString().trim();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        Map<String, Topic> loaded = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                JsonArray arr = GsonHelper.getAsJsonArray(e.getValue().getAsJsonObject(), "topics");
                for (JsonElement el : arr) {
                    Topic t = parse(el.getAsJsonObject());
                    loaded.put(t.id(), t); // later files may override topics by id
                }
            } catch (Exception ex) {
                HollowSignal.LOGGER.error("Bad dialogue file {}", e.getKey(), ex);
            }
        });
        topics = loaded;
        HollowSignal.LOGGER.info("Loaded {} dialogue topics", loaded.size());
    }

    private static Topic parse(JsonObject o) {
        String id = GsonHelper.getAsString(o, "id");
        List<String> keywords = new ArrayList<>();
        if (o.has("keywords")) {
            for (JsonElement k : GsonHelper.getAsJsonArray(o, "keywords")) {
                String n = normalize(k.getAsString());
                if (!n.isEmpty()) keywords.add(n);
            }
        }
        int rel = GsonHelper.getAsInt(o, "rel_delta", 0);
        Map<Band, List<String>> replies = new EnumMap<>(Band.class);
        JsonObject r = GsonHelper.getAsJsonObject(o, "replies");
        for (Band b : Band.values()) {
            String name = b.name().toLowerCase(Locale.ROOT);
            if (!r.has(name)) continue;
            List<String> list = new ArrayList<>();
            for (JsonElement k : r.getAsJsonArray(name)) list.add(k.getAsString());
            replies.put(b, List.copyOf(list));
        }
        return new Topic(id, List.copyOf(keywords), rel, replies);
    }
}
