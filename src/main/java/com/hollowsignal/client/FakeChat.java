package com.hollowsignal.client;

import com.hollowsignal.net.FakeChatPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Shows the entity's chat lines exactly like real chat / join / leave / server messages. */
public final class FakeChat {
    private FakeChat() {}

    private static final RandomSource RANDOM = RandomSource.create();
    private static final char[] BLOCKS = {'▓', '▒', '░', '█', '#', '?'};
    private static final net.minecraft.resources.ResourceLocation ALT_FONT =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("alt");

    public static void show(FakeChatPayload p) {
        Component line = switch (p.kind()) {
            case FakeChatPayload.JOIN -> Component.translatable("multiplayer.player.joined", p.name()).withStyle(ChatFormatting.YELLOW);
            case FakeChatPayload.LEAVE -> Component.translatable("multiplayer.player.left", p.name()).withStyle(ChatFormatting.YELLOW);
            case FakeChatPayload.SYSTEM -> Component.literal(distort(I18n.get(p.key()), p.distortion()))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            case FakeChatPayload.ECHO -> Component.translatable("chat.type.text", Component.literal(p.name()),
                    Component.literal(distort(p.key(), p.distortion())));
            // Enchanting-table glyphs (vanilla "minecraft:alt" font): readable to nobody.
            case FakeChatPayload.CURSED -> Component.translatable("chat.type.text", Component.literal(p.name()),
                    Component.literal(toLatin(I18n.get(p.key())))
                            .withStyle(s -> s.withFont(ALT_FONT).withColor(ChatFormatting.DARK_PURPLE)));
            default -> Component.translatable("chat.type.text", Component.literal(p.name()),
                    Component.literal(distort(I18n.get(p.key()), p.distortion())));
        };
        Minecraft.getInstance().gui.getChat().addMessage(line);
    }

    private static final String CYR = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String[] LAT = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p",
            "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "", "y", "", "e", "yu", "ya"};

    /** The glyph font only covers Latin letters, so Cyrillic is transliterated first. */
    static String toLatin(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toLowerCase().toCharArray()) {
            int i = CYR.indexOf(c);
            sb.append(i >= 0 ? LAT[i] : String.valueOf(c));
        }
        return sb.toString();
    }

    /** Corrupts text: swaps letters for block glyphs, flips case, adds combining "zalgo" marks. */
    public static String distort(String text, float amount) {
        if (amount <= 0f) return text;
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || RANDOM.nextFloat() > amount) {
                sb.append(c);
                continue;
            }
            switch (RANDOM.nextInt(3)) {
                case 0 -> sb.append(BLOCKS[RANDOM.nextInt(BLOCKS.length)]);
                case 1 -> sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
                default -> {
                    sb.append(c);
                    int marks = 1 + RANDOM.nextInt(3);
                    for (int m = 0; m < marks; m++) sb.append((char) (0x0300 + RANDOM.nextInt(0x70)));
                }
            }
        }
        return sb.toString();
    }
}
