package com.example.worldgen;

import com.example.ExampleMod;
import com.example.block.CannabisCropBlock;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class CannabisPatchFeature extends Feature<DefaultFeatureConfig> {

    private static final int TRIES = 8;
    private static final int SPREAD = 4;

    public CannabisPatchFeature(Codec<DefaultFeatureConfig> codec) {
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

            // Find surface at the offset position
            BlockPos lowerPos = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(origin.getX() + dx, 0, origin.getZ() + dz));
            BlockPos upperPos = lowerPos.up();
            BlockState floor = world.getBlockState(lowerPos.down());

            if (!isValidSoil(floor)) continue;
            if (!world.isAir(lowerPos) || !world.isAir(upperPos) || !world.isAir(lowerPos.up(2)) || lowerPos.getY() + 3 > world.getTopY()) continue;

            BlockState lowerState = ExampleMod.CANNABIS_CROP.getDefaultState()
                    .with(CannabisCropBlock.AGE, CannabisCropBlock.MAX_AGE)
                    .with(CannabisCropBlock.PART, 0);
            BlockState upperState = ExampleMod.CANNABIS_CROP.getDefaultState()
                    .with(CannabisCropBlock.AGE, CannabisCropBlock.MAX_AGE)
                    .with(CannabisCropBlock.PART, 1);

            world.setBlockState(lowerPos, lowerState, Block.NOTIFY_LISTENERS);
            world.setBlockState(upperPos, upperState, Block.NOTIFY_LISTENERS);
            world.setBlockState(lowerPos.up(2), upperState.with(CannabisCropBlock.PART, 2), Block.NOTIFY_LISTENERS);
            placed = true;
        }

        return placed;
    }

    private boolean isValidSoil(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.ROOTED_DIRT)
                || state.isOf(Blocks.MUD);
    }
}

