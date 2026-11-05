package com.hollowsignal.registry;

import com.hollowsignal.HollowSignal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HollowSignal.MODID);

    /** Debug/creative only: a Listener spawned from an egg stalks the nearest player. */
    public static final DeferredItem<DeferredSpawnEggItem> LISTENER_SPAWN_EGG = ITEMS.register("listener_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LISTENER, 0x0c0c0e, 0x4a4a52, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WATCHER_SPAWN_EGG = ITEMS.register("watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WATCHER, 0xb8b8bc, 0x101012, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> REPORTER_SPAWN_EGG = ITEMS.register("reporter_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.REPORTER, 0x1a1a1e, 0xc81e1e, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_SPAWN_EGG = ITEMS.register("hollow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HOLLOW, 0x050505, 0x5a2a6e, new Item.Properties()));

    public static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(LISTENER_SPAWN_EGG);
            event.accept(WATCHER_SPAWN_EGG);
            event.accept(REPORTER_SPAWN_EGG);
            event.accept(HOLLOW_SPAWN_EGG);
        }
    }
}
