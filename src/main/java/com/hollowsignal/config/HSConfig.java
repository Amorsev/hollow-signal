package com.hollowsignal.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class HSConfig {
    private HSConfig() {}

    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Client, ModConfigSpec> c = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = c.getLeft();
        CLIENT_SPEC = c.getRight();
        Pair<Server, ModConfigSpec> s = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = s.getLeft();
        SERVER_SPEC = s.getRight();
    }

    /** Lives on the player's PC. The server can never change these. */
    public static final class Client {
        public final ModConfigSpec.EnumValue<SafetyMode> safetyMode;
        public final ModConfigSpec.BooleanValue realErrorDialogs;
        public final ModConfigSpec.BooleanValue fileDrops;
        public final ModConfigSpec.BooleanValue realCrash;
        public final ModConfigSpec.BooleanValue windowTitle;
        public final ModConfigSpec.BooleanValue allowPcShutdown;
        public final ModConfigSpec.IntValue shutdownDelaySeconds;
        public final ModConfigSpec.BooleanValue oldMainMenu;
        public final ModConfigSpec.BooleanValue oldTextures;
        public final ModConfigSpec.BooleanValue oldTexturesApplied;

        Client(ModConfigSpec.Builder b) {
            b.comment("Fourth-wall settings. SAFE keeps everything inside the game window.").push("fourth_wall");
            safetyMode = b
                    .comment("SAFE (default) or HARDCORE. HARDCORE enables the effects below that are set to true.")
                    .defineEnum("safetyMode", SafetyMode.SAFE);
            realErrorDialogs = b
                    .comment("HARDCORE: show real OS message boxes carrying messages from the entity.")
                    .define("realErrorDialogs", true);
            fileDrops = b
                    .comment("HARDCORE: create NEW .txt notes in random folders under C:\\Users\\<you>\\.",
                            "Never overwrites or deletes your files. Remove them with /hollowsignal cleanup.")
                    .define("fileDrops", true);
            realCrash = b
                    .comment("HARDCORE: actually close the game (world is saved first).")
                    .define("realCrash", true);
            windowTitle = b
                    .comment("HARDCORE: change the game window title.")
                    .define("windowTitle", true);
            allowPcShutdown = b
                    .comment("HARDCORE ONLY, separate opt-in: schedule a real, cancellable PC shutdown",
                            "(Windows: 'shutdown /a' cancels). Off even in HARDCORE unless you enable it.")
                    .define("allowPcShutdown", false);
            shutdownDelaySeconds = b
                    .comment("Seconds of warning before the shutdown, so you can save your work.")
                    .defineInRange("shutdownDelaySeconds", 60, 30, 600);
            hardcoreAcknowledged = b
                    .comment("Internal: set only by the in-game HARDCORE warning screen. Without it, HARDCORE acts like SAFE.")
                    .define("hardcoreAcknowledged", false);
            b.pop();

            b.comment("Look and feel").push("visuals");
            oldMainMenu = b
                    .comment("Replace the title screen with an old-style (Alpha/Beta era) main menu.")
                    .define("oldMainMenu", true);
            oldTextures = b
                    .comment("Enable the built-in 'Programmer Art' resource pack (classic pre-1.14 textures).",
                            "Applied once; if you later disable the pack manually, the mod won't force it back.")
                    .define("oldTextures", true);
            oldTexturesApplied = b
                    .comment("Internal: whether oldTextures has already been applied. Don't edit.")
                    .define("oldTexturesApplied", false);
            b.pop();
        }

        public final ModConfigSpec.BooleanValue hardcoreAcknowledged;

        /** HARDCORE counts only after the player confirmed it on the warning screen. */
        public boolean hardcore() {
            return safetyMode.get() == SafetyMode.HARDCORE && hardcoreAcknowledged.get();
        }

        public void setSafe() {
            safetyMode.set(SafetyMode.SAFE);
            hardcoreAcknowledged.set(false);
            safetyMode.save();
        }

        public void setHardcoreConfirmed() {
            safetyMode.set(SafetyMode.HARDCORE);
            hardcoreAcknowledged.set(true);
            safetyMode.save();
        }
    }

    /** Per-world settings, synced to clients. */
    public static final class Server {
        public final ModConfigSpec.BooleanValue allowHardcoreEffects;
        public final ModConfigSpec.BooleanValue mimicEnabled;
        public final ModConfigSpec.BooleanValue fakeLeaveEnabled;
        public final ModConfigSpec.IntValue graceDays;
        public final ModConfigSpec.IntValue directorIntervalSeconds;
        public final ModConfigSpec.BooleanValue allowWorldEdits;
        public final ModConfigSpec.DoubleValue intensity;
        public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> disabledEvents;
        public final ModConfigSpec.BooleanValue creepEnabled;
        public final ModConfigSpec.BooleanValue listenerCanAttack;
        public final ModConfigSpec.DoubleValue listenerAttackDamage;
        public final ModConfigSpec.BooleanValue hollowCanAttack;
        public final ModConfigSpec.BooleanValue hollowBreaksBlocks;
        public final ModConfigSpec.BooleanValue hollowDeathTakesToStatic;

        Server(ModConfigSpec.Builder b) {
            b.push("director");
            graceDays = b
                    .comment("In-game days of silence before anything happens.")
                    .defineInRange("graceDays", 3, 0, 100);
            directorIntervalSeconds = b
                    .comment("How often the horror director rolls for an event.")
                    .defineInRange("directorIntervalSeconds", 60, 5, 3600);
            allowWorldEdits = b
                    .comment("Allow events that change blocks (stealing torches, signs, tunnels).")
                    .define("allowWorldEdits", true);
            intensity = b
                    .comment("Overall pace and frequency. 0.5 = slow burn, 1.0 = default, 2.0 = intense.",
                            "Scales how often events happen, their cooldowns and how fast the story advances.")
                    .defineInRange("intensity", 1.0, 0.25, 3.0);
            disabledEvents = b
                    .comment("Event ids that never happen on this server, e.g. [\"hollow\", \"fake_disconnect\"].",
                            "See /hs events for the list.")
                    .defineListAllowEmpty("disabledEvents", java.util.List.of(), () -> "", o -> o instanceof String);
            b.pop();
            b.push("listener");
            creepEnabled = b
                    .comment("Late stages: the Listener sneaks up behind players.")
                    .define("creepEnabled", true);
            listenerCanAttack = b
                    .comment("If the Listener reaches a player unnoticed, it also hits them (off by default: scare only).")
                    .define("listenerCanAttack", false);
            listenerAttackDamage = b
                    .defineInRange("listenerAttackDamage", 4.0, 0.0, 40.0);
            b.pop();
            b.push("hollow");
            hollowCanAttack = b
                    .comment("The Hollow (late stages) attacks players in melee.")
                    .define("hollowCanAttack", true);
            hollowBreaksBlocks = b
                    .comment("The Hollow digs through blocks (hardness <= 3, no containers) to reach its victim.",
                            "Also requires allowWorldEdits and the mobGriefing gamerule.")
                    .define("hollowBreaksBlocks", true);
            hollowDeathTakesToStatic = b
                    .comment("Being killed by the Hollow doesn't kill you — it takes you to the_static (the finale).")
                    .define("hollowDeathTakesToStatic", true);
            b.pop();
            b.push("multiplayer");
            allowHardcoreEffects = b
                    .comment("If false, no client on this server receives hardcore fourth-wall effects,",
                            "even players who enabled HARDCORE locally.")
                    .define("allowHardcoreEffects", true);
            mimicEnabled = b
                    .comment("The Listener may impersonate another online player (their name and skin), seen only by its victim.")
                    .define("mimicEnabled", true);
            fakeLeaveEnabled = b
                    .comment("A victim may see a fake '<friend> left the game' and lose that friend from their Tab list for a minute or two.")
                    .define("fakeLeaveEnabled", true);
            b.pop();
        }
    }
}
