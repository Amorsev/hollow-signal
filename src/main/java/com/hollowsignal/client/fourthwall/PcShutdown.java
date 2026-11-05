package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;

import java.nio.file.Path;
import java.util.Locale;

/**
 * HARDCORE + separate opt-in only: schedules a real, CANCELLABLE Windows shutdown with a delay
 * (default 60 s) and drops "cancel_shutdown.bat" on the Desktop. `shutdown /a` cancels it.
 * Other operating systems: not supported (shutdown there needs admin rights) — nothing happens.
 */
public final class PcShutdown {
    private PcShutdown() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean schedule(int seconds, String message) {
        if (!isWindows()) return false;
        String msg = message.replace('"', '\'');
        if (msg.length() > 500) msg = msg.substring(0, 500);
        try {
            new ProcessBuilder("shutdown", "/s", "/t", Integer.toString(seconds), "/c", msg).inheritIO().start();
            Path desktop = FileDropper.desktop();
            if (desktop != null) {
                FileDropper.write(desktop, "cancel_shutdown.bat",
                        "@echo off\r\nshutdown /a\r\necho Shutdown cancelled. / Выключение отменено.\r\npause\r\n");
            }
            HollowSignal.LOGGER.info("[HARDCORE] PC shutdown scheduled in {}s (cancel: shutdown /a)", seconds);
            return true;
        } catch (Exception e) {
            HollowSignal.LOGGER.warn("[HARDCORE] could not schedule shutdown: {}", e.toString());
            return false;
        }
    }

    public static void cancel() {
        if (!isWindows()) return;
        try {
            new ProcessBuilder("shutdown", "/a").start();
        } catch (Exception ignored) {
        }
    }
}
