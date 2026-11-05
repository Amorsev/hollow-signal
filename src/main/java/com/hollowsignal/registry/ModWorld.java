package com.hollowsignal.registry;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.world.CoreBlock;
import com.hollowsignal.world.ReceiverBlock;
import com.hollowsignal.world.StaticDebrisFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** the_static dimension: its key, the debris feature, and the receiver/core blocks. */
public final class ModWorld {
    private ModWorld() {}

    public static final ResourceKey<Level> THE_STATIC = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "the_static"));

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, HollowSignal.MODID);
    public static final DeferredHolder<Feature<?>, StaticDebrisFeature> STATIC_DEBRIS =
            FEATURES.register("static_debris", () -> new StaticDebrisFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HollowSignal.MODID);

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f)
                .noLootTable().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<ReceiverBlock> RECEIVER = BLOCKS.register("signal_receiver",
            () -> new ReceiverBlock(unbreakable().lightLevel(s -> s.getValue(ReceiverBlock.ACTIVE) ? 10 : 1)));
    public static final DeferredBlock<CoreBlock> CORE = BLOCKS.register("signal_core",
            () -> new CoreBlock(unbreakable().lightLevel(s -> 7)));

    public static boolean isStatic(Level level) {
        return level.dimension() == THE_STATIC;
    }
}
