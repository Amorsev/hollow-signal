package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.config.SafetyMode;
import com.hollowsignal.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point. Never loaded on a dedicated server. */
@Mod(value = HollowSignal.MODID, dist = Dist.CLIENT)
public final class HSClient {
    public HSClient(IEventBus modBus, ModContainer container) {
        // Mods → Hollow Signal → Config: our screen (safety mode + HARDCORE warning), generic settings behind it.
        container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) com.hollowsignal.client.screen.HSConfigScreen::new);
        modBus.addListener(HSClient::onRegisterRenderers);
        modBus.addListener(GlitchOverlay::register);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LISTENER.get(), ListenerRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), ctx -> new SignalMobRenderer<>(ctx, "watcher", 1.15f));
        event.registerEntityRenderer(ModEntities.REPORTER.get(), ctx -> new SignalMobRenderer<>(ctx, "reporter", 0.55f));
        event.registerEntityRenderer(ModEntities.HOLLOW.get(), ctx -> new SignalMobRenderer<>(ctx, "hollow", 1.12f));
    }

    @EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        /** /hollowsignal safemode — always works instantly, on any server, without op. */
        @SubscribeEvent
        public static void onClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("hollowsignal")
                    .then(Commands.literal("cleanup").executes(ctx -> {
                        int n = com.hollowsignal.client.fourthwall.FileDropper.cleanup();
                        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.cleanup", n), false);
                        return n;
                    }))
                    .then(Commands.literal("safemode").executes(ctx -> {
                        HSConfig.CLIENT.setSafe();
                        com.hollowsignal.client.fourthwall.WindowTitle.restore();
                        ctx.getSource().sendSuccess(() -> Component.translatable("hollowsignal.command.safemode"), false);
                        return 1;
                    }))
                    .then(Commands.literal("mode").executes(ctx -> {
                        SafetyMode mode = HSConfig.CLIENT.safetyMode.get();
                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                "hollowsignal.command.mode", mode.name()), false);
                        return 1;
                    })));
        }
    }
}
