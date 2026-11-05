package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * HARDCORE: a real native message box (LWJGL tinyfd, bundled with Minecraft), shown on its own
 * thread so the game keeps running behind it. Returns false if native dialogs are unavailable.
 */
public final class OsDialog {
    private OsDialog() {}

    public static boolean show(String title, String message) {
        try {
            Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
        } catch (Throwable t) {
            return false;
        }
        String t = sanitize(title), m = sanitize(message);
        Thread th = new Thread(() -> {
            try {
                TinyFileDialogs.tinyfd_messageBox(t, m, "ok", "error", true);
            } catch (Throwable e) {
                HollowSignal.LOGGER.warn("[HARDCORE] native dialog failed: {}", e.toString());
            }
        }, "HollowSignal-Dialog");
        th.setDaemon(true);
        th.start();
        return true;
    }

    /** tinyfd rejects quotes on some platforms; swap them for typographic ones. */
    private static String sanitize(String s) {
        return s.replace('"', '”').replace('\'', '’').replace('`', '’');
    }
}
