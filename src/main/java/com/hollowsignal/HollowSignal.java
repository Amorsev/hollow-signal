package com.hollowsignal;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.chat.Dialogue;
import com.hollowsignal.command.HSCommands;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.hollowsignal.config.HSConfig;
import com.hollowsignal.director.HorrorDirector;
import com.hollowsignal.net.ModNetwork;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.registry.ModEntities;
import com.hollowsignal.registry.ModItems;
import com.hollowsignal.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(HollowSignal.MODID)
public final class HollowSignal {
    public static final String MODID = "hollowsignal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HollowSignal(IEventBus modBus, ModContainer container) {
        // CLIENT: safety mode + fourth-wall toggles live ONLY on the player's machine.
        container.registerConfig(ModConfig.Type.CLIENT, HSConfig.CLIENT_SPEC);
        // SERVER: per-world director settings; admin can forbid hardcore effects entirely.
        container.registerConfig(ModConfig.Type.SERVER, HSConfig.SERVER_SPEC);

        ModAttachments.ATTACHMENTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        com.hollowsignal.registry.ModWorld.BLOCKS.register(modBus);
        com.hollowsignal.registry.ModWorld.FEATURES.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        modBus.addListener(ModItems::onCreativeTabs);
        modBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.addListener(HSCommands::register);
        NeoForge.EVENT_BUS.addListener(HorrorDirector::onServerTick);
        NeoForge.EVENT_BUS.addListener(ContactManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(ContactManager::onChat);
        NeoForge.EVENT_BUS.addListener(ContactManager::onToss);
        NeoForge.EVENT_BUS.addListener(ContactManager::onBreak);
        NeoForge.EVENT_BUS.addListener(ContactManager::onLogout);
        NeoForge.EVENT_BUS.addListener(com.hollowsignal.director.Scheduler::onServerTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            ContactManager.clear();
            com.hollowsignal.director.Scheduler.clear();
            com.hollowsignal.world.Endings.reset();
            com.hollowsignal.director.events.MultiplayerEvents.clear();
        });
        NeoForge.EVENT_BUS.addListener(com.hollowsignal.world.StaticEntry::onDeath);
        // Must see chat/commands BEFORE they are broadcast (restores a "faked-out" friend first).
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
                com.hollowsignal.director.events.MultiplayerEvents::onChat);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
                com.hollowsignal.director.events.MultiplayerEvents::onCommand);
        NeoForge.EVENT_BUS.addListener(com.hollowsignal.director.events.MultiplayerEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.hollowsignal.director.events.MultiplayerEvents::onLogout);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(Dialogue.INSTANCE));

        LOGGER.info("Hollow Signal loaded. Something is listening.");
    }
}
