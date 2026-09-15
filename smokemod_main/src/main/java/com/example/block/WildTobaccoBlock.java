package com.example.block;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class WildTobaccoBlock extends PlantBlock {

    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public WildTobaccoBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.GRASS_BLOCK)
                || floor.isOf(Blocks.DIRT)
                || floor.isOf(Blocks.COARSE_DIRT)
                || floor.isOf(Blocks.PODZOL)
                || floor.isOf(Blocks.ROOTED_DIRT)
                || floor.isOf(Blocks.MUD)
                || floor.isOf(Blocks.FARMLAND);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
