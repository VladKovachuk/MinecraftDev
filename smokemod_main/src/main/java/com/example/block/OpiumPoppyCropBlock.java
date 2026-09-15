package com.example.block;

import com.example.ExampleMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Eight-stage opium poppy. Ages 0-4 are one block high and ages 5-7 are two
 * blocks high. Breaking either half of a tall plant removes the whole plant.
 */
public class OpiumPoppyCropBlock extends CropBlock {
    public static final int MAX_AGE = 7;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public OpiumPoppyCropBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState()
                .with(AGE, 0)
                .with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ExampleMod.POPPY_SEEDS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    private static boolean isTall(int age) {
        return age >= 5;
    }

    public static boolean isWildSoil(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.ROOTED_DIRT)
                || state.isOf(Blocks.MUD);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER && !isMature(state);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            super.randomTick(state, world, pos, random);
        }
    }

    @Override
    protected int getGrowthAmount(World world) {
        return 1;
    }

    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER || isMature(state)) {
            return;
        }

        int currentAge = state.get(AGE);
        int newAge = currentAge + 1;
        if (!isTall(currentAge) && isTall(newAge) && !world.isAir(pos.up())) {
            return;
        }

        world.setBlockState(pos, state.with(AGE, newAge), Block.NOTIFY_ALL);
        if (isTall(newAge)) {
            BlockPos upperPos = pos.up();
            BlockState upperState = world.getBlockState(upperPos);
            if (upperState.isAir() || upperState.isOf(this)) {
                world.setBlockState(upperPos, getDefaultState()
                        .with(AGE, newAge)
                        .with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState lower = world.getBlockState(pos.down());
            return lower.isOf(this)
                    && lower.get(HALF) == DoubleBlockHalf.LOWER
                    && isTall(lower.get(AGE));
        }
        // Seeds still require farmland, while fully grown worldgen plants may
        // remain on natural soil just like wild cannabis.
        return super.canPlaceAt(state, world, pos)
                || (isMature(state) && isWildSoil(world.getBlockState(pos.down())));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return canPlaceAt(state, world, pos) ? state : Blocks.AIR.getDefaultState();
        }
        if (isTall(state.get(AGE)) && direction == Direction.UP
                && (!neighborState.isOf(this) || neighborState.get(HALF) != DoubleBlockHalf.UPPER)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            if (state.get(HALF) == DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = pos.down();
                BlockState lowerState = world.getBlockState(lowerPos);
                if (lowerState.isOf(this) && lowerState.get(HALF) == DoubleBlockHalf.LOWER) {
                    world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS);
                    if (!player.isCreative()) {
                        Block.dropStacks(lowerState, world, lowerPos, null, player, player.getMainHandStack());
                    }
                }
            } else if (isTall(state.get(AGE))) {
                BlockPos upperPos = pos.up();
                BlockState upperState = world.getBlockState(upperPos);
                if (upperState.isOf(this) && upperState.get(HALF) == DoubleBlockHalf.UPPER) {
                    world.setBlockState(upperPos, Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER || isMature(state)) {
            return false;
        }
        return isTall(state.get(AGE)) || state.get(AGE) != 4 || world.isAir(pos.up());
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return isFertilizable(world, pos, state, world.isClient);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return Block.createCuboidShape(1, 0, 1, 15, 16, 15);
        }
        int age = state.get(AGE);
        return Block.createCuboidShape(1, 0, 1, 15, age == 0 ? 5 : Math.min(16, 7 + age * 2), 15);
    }
}
