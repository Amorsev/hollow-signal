package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.client.screen.FakeCrashScreen;
import com.hollowsignal.client.screen.FakeErrorScreen;
import com.hollowsignal.client.screen.FakeShutdownScreen;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.net.OsActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Client-side dispatcher for fourth-wall intents. Real effects need ALL of:
 * local HARDCORE confirmed on the warning screen + the specific toggle on + the server allowing it.
 * Everything else falls back to the SAFE in-game version.
 */
public final class FourthWall {
    private FourthWall() {}

    public static final String[] ERROR_TITLES = {
            "javaw.exe - Application Error", "Java Virtual Machine Launcher", "Minecraft", "javaw.exe", "Windows"
    };
    private static final String[] NOTE_FILES = {
            "hello.txt", "you_left_the_light_on.txt", "not_a_virus.txt", "i_know_this_folder.txt", "dont_close_the_game.txt"
    };
    private static final String[] TITLES = {
            "Minecraft* 1.21.1 - Singleplayer (2 players)", "it is listening", "do not close me", "Minecraft* ?.?.? - Signal lost",
    };

    private static boolean real(OsActionPayload p, boolean toggle) {
        return p.serverAllows() && toggle && HSConfig.CLIENT.hardcore();
    }

    public static void handle(OsActionPayload p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.action()) {
            case OsActionPayload.ERROR_DIALOG -> {
                int v = p.variant() % ERROR_TITLES.length;
                if (real(p, HSConfig.CLIENT.realErrorDialogs.get())
                        && OsDialog.show(ERROR_TITLES[v], I18n.get("hollowsignal.error." + v))) return;
                if (!(mc.screen instanceof ChatScreen)) mc.setScreen(new FakeErrorScreen(v));
            }
            case OsActionPayload.FILE_DROP -> {
                if (!real(p, HSConfig.CLIENT.fileDrops.get())) {
                    whisper(p.entityName(), I18n.get("hollowsignal.fourthwall.blocked"));
                    return;
                }
                int v = p.variant() % NOTE_FILES.length;
                String player = mc.player != null ? mc.player.getGameProfile().getName() : "player";
                String content = I18n.get("hollowsignal.note." + v, player).replace("\n", System.lineSeparator());
                String fileName = NOTE_FILES[v];
                Thread t = new Thread(() -> {
                    FileDropper.Drop drop = FileDropper.dropNote(fileName, content);
                    if (drop != null) mc.execute(() -> whisper(p.entityName(), I18n.get("hollowsignal.fourthwall.dropped", drop.folderName())));
                }, "HollowSignal-FileDrop");
                t.setDaemon(true);
                t.start();
            }
            case OsActionPayload.CRASH -> {
                boolean realCrash = real(p, HSConfig.CLIENT.realCrash.get());
                // The crash report always types itself out first; then either the real thing or the SAFE version.
                mc.setScreen(new FakeCrashScreen(realCrash ? 100 : 200, () -> {
                    if (realCrash) RealCrash.begin();
                    else mc.setScreen(new FakeShutdownScreen(140));
                }));
            }
            case OsActionPayload.WINDOW_TITLE -> {
                if (real(p, HSConfig.CLIENT.windowTitle.get())) {
                    WindowTitle.set(TITLES[p.variant() % TITLES.length], 60 * 20);
                }
            }
            default -> HollowSignal.LOGGER.debug("Unknown fourth-wall action {}", p.action());
        }
    }

    private static void whisper(String name, String text) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.translatable("chat.type.text", Component.literal(name), Component.literal(text)));
    }
}
