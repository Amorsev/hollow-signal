package com.hollowsignal.registry;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.entity.HollowEntity;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.entity.ReporterEntity;
import com.hollowsignal.entity.WatcherEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HollowSignal.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ListenerEntity>> LISTENER = ENTITIES.register("listener",
            () -> EntityType.Builder.of(ListenerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(16) // chunks: visible from far away, at the fog edge
                    .build("listener"));

    public static final DeferredHolder<EntityType<?>, EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.1f)
                    .eyeHeight(1.95f)
                    .clientTrackingRange(10)
                    .build("watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<ReporterEntity>> REPORTER = ENTITIES.register("reporter",
            () -> EntityType.Builder.of(ReporterEntity::new, MobCategory.MISC)
                    .sized(0.4f, 1.0f)
                    .eyeHeight(0.85f)
                    .clientTrackingRange(10)
                    .build("reporter"));

    public static final DeferredHolder<EntityType<?>, EntityType<HollowEntity>> HOLLOW = ENTITIES.register("hollow",
            () -> EntityType.Builder.of(HollowEntity::new, MobCategory.MISC)
                    .sized(0.7f, 2.05f)
                    .eyeHeight(1.85f)
                    .clientTrackingRange(10)
                    .build("hollow"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(LISTENER.get(), ListenerEntity.createAttributes().build());
        event.put(WATCHER.get(), WatcherEntity.createAttributes().build());
        event.put(REPORTER.get(), ReporterEntity.createAttributes().build());
        event.put(HOLLOW.get(), HollowEntity.createAttributes().build());
    }
}
