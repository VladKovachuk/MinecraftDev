package com.example.worldgen;

import com.example.ExampleMod;
import com.example.block.OpiumPoppyCropBlock;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class OpiumPoppyPatchFeature extends Feature<DefaultFeatureConfig> {
    private static final int TRIES = 8;
    private static final int SPREAD = 4;

    public OpiumPoppyPatchFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        StructureWorldAccess world = context.getWorld();
        boolean placed = false;

        for (int i = 0; i < TRIES; i++) {
            int dx = random.nextBetween(-SPREAD, SPREAD);
            int dz = random.nextBetween(-SPREAD, SPREAD);
            BlockPos lowerPos = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(origin.getX() + dx, 0, origin.getZ() + dz));
            BlockPos upperPos = lowerPos.up();

            if (!OpiumPoppyCropBlock.isWildSoil(world.getBlockState(lowerPos.down()))) {
                continue;
            }
            if (!world.isAir(lowerPos) || !world.isAir(upperPos)
                    || lowerPos.getY() + 2 > world.getTopY()) {
                continue;
            }

            BlockState lowerState = ExampleMod.OPIUM_POPPY.getDefaultState()
                    .with(OpiumPoppyCropBlock.AGE, OpiumPoppyCropBlock.MAX_AGE)
                    .with(OpiumPoppyCropBlock.HALF, DoubleBlockHalf.LOWER);
            BlockState upperState = ExampleMod.OPIUM_POPPY.getDefaultState()
                    .with(OpiumPoppyCropBlock.AGE, OpiumPoppyCropBlock.MAX_AGE)
                    .with(OpiumPoppyCropBlock.HALF, DoubleBlockHalf.UPPER);

            world.setBlockState(lowerPos, lowerState, Block.NOTIFY_LISTENERS);
            world.setBlockState(upperPos, upperState, Block.NOTIFY_LISTENERS);
            placed = true;
        }

        return placed;
    }
}
