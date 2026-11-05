package com.hollowsignal.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Floating fragments of a broken world: small, ragged chunks of terrain and building blocks
 * hanging in the void of the_static, sometimes with a dim redstone torch on top.
 */
public class StaticDebrisFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState[] SURFACE = {
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.SAND.defaultBlockState()
    };
    private static final BlockState[] BODY = {
            Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(), Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.GLASS.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState()
    };

    public StaticDebrisFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        int rx = 2 + r.nextInt(4), rz = 2 + r.nextInt(4), depth = 2 + r.nextInt(4);
        BlockState top = SURFACE[r.nextInt(SURFACE.length)];
        BlockState body = BODY[r.nextInt(BODY.length)];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double nd = (dx * dx) / (double) (rx * rx) + (dz * dz) / (double) (rz * rz);
                if (nd > 1.0 || r.nextFloat() < 0.12) continue; // ragged edges, holes
                // Upside-down cone: thick in the middle, thin at the edges.
                int d = Math.max(1, (int) Math.round(depth * (1.0 - nd)) + r.nextInt(2));
                for (int dy = 0; dy < d; dy++) {
                    p.setWithOffset(origin, dx, -dy, dz);
                    BlockState s = dy == 0 ? top : (r.nextFloat() < 0.2 ? BODY[r.nextInt(BODY.length)] : body);
                    level.setBlock(p, s, 2);
                }
                if (r.nextFloat() < 0.015) level.setBlock(p.setWithOffset(origin, dx, 1, dz), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
            }
        }
        return true;
    }
}
