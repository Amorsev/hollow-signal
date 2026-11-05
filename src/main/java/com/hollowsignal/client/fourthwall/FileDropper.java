package com.hollowsignal.client.fourthwall;

import com.hollowsignal.HollowSignal;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * HARDCORE: writes NEW text files into the player's own folders under their user profile
 * (C:\Users\<you>\ on Windows): Desktop, Documents, Downloads, Pictures, Music, Videos and random
 * sub-folders of those. Never the game folder, never AppData or hidden/system folders.
 *
 * Safety rules:
 *  - files are created with CREATE_NEW: an existing file is never overwritten;
 *  - every created file is logged (path + SHA-256) in .minecraft/hollowsignal/created_files.log;
 *  - cleanup deletes ONLY logged files whose content is still exactly what the mod wrote.
 */
public final class FileDropper {
    private FileDropper() {}

    private static final Random RANDOM = new Random();
    private static final String[] KNOWN = {
            "Desktop", "OneDrive/Desktop", "OneDrive/Рабочий стол", "Documents", "OneDrive/Documents", "OneDrive/Документы",
            "Downloads", "Pictures", "OneDrive/Pictures", "Music", "Videos"
    };
    private static final Set<String> SKIP = Set.of("appdata", "application data", "local settings", "my music",
            "my pictures", "my videos", "minecraft", ".minecraft", "node_modules", "$recycle.bin");

    public record Drop(Path file, String folderName) {}

    private static Path home() {
        return Path.of(System.getProperty("user.home"));
    }

    private static Path logFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("hollowsignal").resolve("created_files.log");
    }

    /** Desktop if we can find it (OneDrive-redirected desktops included). */
    @Nullable
    public static Path desktop() {
        for (String k : new String[]{"Desktop", "OneDrive/Desktop", "OneDrive/Рабочий стол"}) {
            Path p = home().resolve(k);
            if (Files.isDirectory(p) && Files.isWritable(p)) return p;
        }
        return null;
    }

    /** Candidate folders: known user folders + their sub-folders (depth ≤ 2), filtered. */
    static List<Path> candidates() {
        Path game = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        for (String k : KNOWN) {
            Path p = home().resolve(k);
            if (usable(p, game)) roots.add(p);
        }
        List<Path> all = new ArrayList<>(roots);
        for (Path r : roots) collect(r, 1, all, game);
        return all;
    }

    private static void collect(Path dir, int depth, List<Path> out, Path game) {
        if (depth > 2 || out.size() > 300) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path sub : ds) {
                if (out.size() > 300) return;
                if (!usable(sub, game)) continue;
                out.add(sub);
                collect(sub, depth + 1, out, game);
            }
        } catch (IOException | SecurityException ignored) {
        }
    }

    private static boolean usable(Path p, Path game) {
        try {
            if (!Files.isDirectory(p) || Files.isSymbolicLink(p) || !Files.isWritable(p) || Files.isHidden(p)) return false;
            String name = p.getFileName() == null ? "" : p.getFileName().toString();
            if (name.startsWith(".") || name.startsWith("$") || SKIP.contains(name.toLowerCase(Locale.ROOT))) return false;
            Path abs = p.toAbsolutePath().normalize();
            return !abs.startsWith(game); // never inside the game folder
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    /** Drops a note into a random folder (Desktop ~40% of the time). Runs file IO on the calling thread. */
    @Nullable
    public static Drop dropNote(String fileName, String content) {
        Path dir = null;
        Path desk = desktop();
        if (desk != null && RANDOM.nextInt(5) < 2) dir = desk;
        if (dir == null) {
            List<Path> c = candidates();
            if (c.isEmpty()) return null;
            dir = c.get(RANDOM.nextInt(c.size()));
        }
        Path file = write(dir, fileName, content);
        return file == null ? null : new Drop(file, dir.getFileName().toString());
    }

    /** Creates a NEW file (never overwrites), logs it, returns its path or null. */
    @Nullable
    public static Path write(Path dir, String fileName, String content) {
        String base = fileName, ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) { base = fileName.substring(0, dot); ext = fileName.substring(dot); }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < 50; i++) {
            Path target = dir.resolve(i == 0 ? fileName : base + "_" + (i + 1) + ext);
            try {
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                log(target, bytes);
                HollowSignal.LOGGER.info("[HARDCORE] created {}", target);
                return target;
            } catch (java.nio.file.FileAlreadyExistsException exists) {
                // try the next name
            } catch (IOException | SecurityException e) {
                HollowSignal.LOGGER.warn("[HARDCORE] could not create note in {}: {}", dir, e.toString());
                return null;
            }
        }
        return null;
    }

    private static synchronized void log(Path file, byte[] bytes) throws IOException {
        Path log = logFile();
        Files.createDirectories(log.getParent());
        Files.writeString(log, file.toAbsolutePath() + "\t" + sha256(bytes) + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Deletes the files the mod created that are still unmodified. Returns how many were removed. */
    public static synchronized int cleanup() {
        Path log = logFile();
        if (!Files.exists(log)) return 0;
        int removed = 0;
        List<String> keep = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(log, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t");
                if (parts.length != 2) continue;
                Path p = Path.of(parts[0]);
                try {
                    if (!Files.isRegularFile(p)) continue;                          // already gone
                    if (!sha256(Files.readAllBytes(p)).equals(parts[1])) { keep.add(line); continue; } // edited by the user: leave it
                    Files.delete(p);
                    removed++;
                } catch (IOException e) {
                    keep.add(line);
                }
            }
            if (keep.isEmpty()) Files.delete(log);
            else Files.write(log, keep, StandardCharsets.UTF_8);
        } catch (IOException e) {
            HollowSignal.LOGGER.warn("[HARDCORE] cleanup failed: {}", e.toString());
        }
        return removed;
    }

    public static int loggedCount() {
        try {
            Path log = logFile();
            return Files.exists(log) ? (int) Files.readAllLines(log).stream().filter(s -> !s.isBlank()).count() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(data));
        }
    }
}
