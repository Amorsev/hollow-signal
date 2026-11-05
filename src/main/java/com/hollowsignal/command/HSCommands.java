package com.hollowsignal.command;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.registry.ModWorld;
import com.hollowsignal.world.Endings;
import com.hollowsignal.world.StaticArena;
import com.hollowsignal.world.StaticEntry;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.HorrorDirector;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.HorrorEvents;
import com.hollowsignal.director.PlayerHorrorData;
import com.hollowsignal.director.Stage;
import com.hollowsignal.director.WorldHorrorData;
import com.hollowsignal.net.GlitchPayload;
import com.hollowsignal.registry.ModAttachments;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Server-side admin/debug commands (op level 2):
 * /hs status
 * /hs dread get <player> | set <players> <v> | add <players> <v>
 * /hs global set <v>
 * /hs trigger <event> [players]
 * /hs reset [players]
 */
public final class HSCommands {
    private HSCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("hs")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("status").executes(HSCommands::status))
                .then(Commands.literal("events")
                        .executes(ctx -> events(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> events(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("dread")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    PlayerHorrorData data = ModAttachments.of(p);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.dread.get",
                                            p.getDisplayName(), data.dread(), data.relationship()), false);
                                    return data.dread();
                                })))
                        .then(Commands.literal("set").then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, PlayerHorrorData.MAX_DREAD))
                                        .executes(ctx -> modifyDread(ctx, PlayerHorrorData::setDread)))))
                        .then(Commands.literal("add").then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-PlayerHorrorData.MAX_DREAD, PlayerHorrorData.MAX_DREAD))
                                        .executes(ctx -> modifyDread(ctx, PlayerHorrorData::addDread))))))
                .then(Commands.literal("global").then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, WorldHorrorData.MAX_DREAD)).executes(ctx -> {
                            WorldHorrorData w = WorldHorrorData.get(ctx.getSource().getServer());
                            w.setGlobalDread(IntegerArgumentType.getInteger(ctx, "value"));
                            return status(ctx);
                        }))))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(HorrorEvents.ids(), b))
                                .executes(ctx -> trigger(ctx, List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> trigger(ctx, EntityArgument.getPlayers(ctx, "players"))))))
                .then(Commands.literal("relationship").then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100))
                                        .executes(ctx -> modifyDread(ctx, PlayerHorrorData::setRelationship))))))
                .then(Commands.literal("say")
                        .then(Commands.argument("topic", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        List.of("greet", "idle", "unknown", "gift", "sign_broken", "system", "who", "where", "want"), b))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    return ContactManager.say(p, StringArgumentType.getString(ctx, "topic"), 0) ? 1 : 0;
                                })))
                .then(Commands.literal("leave").executes(ctx -> {
                    ContactManager.leave(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("static")
                        .then(Commands.literal("enter").executes(ctx -> {
                            StaticEntry.pull(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!ModWorld.isStatic(p.level())) return 0;
                            StaticArena.leave(p);
                            return 1;
                        }))
                        .then(Commands.literal("reset").executes(ctx -> {
                            var lvl = StaticArena.level(ctx.getSource().getServer());
                            if (lvl == null) return 0;
                            StaticArena.ensureBuilt(lvl);
                            StaticArena.reset(lvl);
                            return 1;
                        })))
                .then(Commands.literal("ending")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("released", "loop", "erased"), b))
                                .executes(ctx -> {
                                    Endings.Type type;
                                    try {
                                        type = Endings.Type.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase(java.util.Locale.ROOT));
                                    } catch (IllegalArgumentException e) {
                                        return 0;
                                    }
                                    Endings.trigger(ctx.getSource().getServer(), List.of(ctx.getSource().getPlayerOrException()), type);
                                    return 1;
                                })))
                .then(Commands.literal("glitch")
                        .executes(ctx -> glitch(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> glitch(ctx, EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx, ctx.getSource().getServer().getPlayerList().getPlayers()))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> reset(ctx, EntityArgument.getPlayers(ctx, "players"))))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        WorldHorrorData w = WorldHorrorData.get(server);
        long day = Stage.worldDay(server);
        int stage = Stage.compute(server, w);
        int grace = HSConfig.SERVER.graceDays.get();
        boolean hc = HSConfig.SERVER.allowHardcoreEffects.get();
        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.status",
                day, stage, Component.translatable("hollowsignal.stage." + stage), w.globalDread(), grace,
                Component.translatable(hc ? "hollowsignal.on" : "hollowsignal.off")), false);
        return stage;
    }

    /**
     * Balance view: every event with its minimum stage, weight for this player right now
     * and remaining cooldown. Green = could be picked now.
     */
    private static int events(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MinecraftServer server = ctx.getSource().getServer();
        int stage = Stage.compute(server, WorldHorrorData.get(server));
        long now = server.overworld().getGameTime();
        var ectx = com.hollowsignal.director.EventContext.of(player, stage);
        PlayerHorrorData d = ModAttachments.of(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.events.header",
                player.getDisplayName(), stage, String.format(java.util.Locale.ROOT, "%.2f", HorrorDirector.intensity())), false);
        for (HorrorEvent e : HorrorEvents.all()) {
            long left = Math.max(0, HorrorDirector.cooldown(e) - (now - d.lastRun(e.id()))) / 20;
            int w = e.weight(ectx);
            boolean off = HorrorDirector.disabled(e);
            boolean ready = !off && e.minStage() <= stage && w > 0 && left == 0;
            String line = String.format(java.util.Locale.ROOT, "%-16s st%d  w%-3d %s", e.id(), e.minStage(), w,
                    off ? "OFF" : left > 0 ? (left / 60) + "m" + (left % 60) + "s" : "-");
            ChatFormatting color = off ? ChatFormatting.DARK_GRAY : ready ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(color), false);
        }
        return 1;
    }

    private static int modifyDread(CommandContext<CommandSourceStack> ctx, BiConsumer<PlayerHorrorData, Integer> op)
            throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        for (ServerPlayer p : players) op.accept(ModAttachments.of(p), value);
        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.dread.changed", players.size()), true);
        return players.size();
    }

    private static int trigger(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String id = StringArgumentType.getString(ctx, "event");
        HorrorEvent e = HorrorEvents.get(id);
        if (e == null) {
            ctx.getSource().sendFailure(Component.translatable("hollowsignal.command.trigger.unknown", id));
            return 0;
        }
        int ok = 0;
        for (ServerPlayer p : players) {
            if (HorrorDirector.forceRun(e, p)) ok++;
        }
        int done = ok;
        if (done > 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.trigger.ok", id, done), true);
        } else {
            ctx.getSource().sendFailure(Component.translatable("hollowsignal.command.trigger.fail", id));
        }
        return done;
    }

    /** Debug: play the stare-back interference (screen distortion + static) on players. */
    private static int glitch(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) PacketDistributor.sendToPlayer(p, new GlitchPayload(28, 1.0f, true));
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) ModAttachments.of(p).reset();
        WorldHorrorData.get(ctx.getSource().getServer()).reset();
        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.reset", players.size()), true);
        return players.size();
    }
}
