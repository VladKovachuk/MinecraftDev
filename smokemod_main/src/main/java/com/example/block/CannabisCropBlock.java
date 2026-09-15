package com.example.block;

import com.example.ExampleMod;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

public class CannabisCropBlock extends CropBlock {
    public static final int MAX_AGE = 6;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);
    // 0 = base, 1 = middle, 2 = top. Only the base owns loot and growth.
    public static final IntProperty PART = IntProperty.of("part", 0, 2);

    public CannabisCropBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(AGE, 0).with(PART, 0));
    }

    @Override public IntProperty getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return MAX_AGE; }
    @Override protected ItemConvertible getSeedsItem() { return ExampleMod.CANNABIS_SEEDS; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(AGE, PART); }

    public static int height(int age) { return age == MAX_AGE ? 3 : age >= 4 ? 2 : 1; }

    public static boolean isWildSoil(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.ROOTED_DIRT) || state.isOf(Blocks.MUD);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        int part = state.get(PART);
        if (part > 0) {
            BlockState base = world.getBlockState(pos.down(part));
            return base.isOf(this) && base.get(PART) == 0 && height(base.get(AGE)) > part;
        }
        // Seeds require farmland; mature wild plants also survive on natural soil.
        return super.canPlaceAt(state, world, pos)
                || (isMature(state) && isWildSoil(world.getBlockState(pos.down())));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return canPlaceAt(state, world, pos) ? state : Blocks.AIR.getDefaultState();
    }

    @Override public boolean hasRandomTicks(BlockState state) { return state.get(PART) == 0 && !isMature(state); }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!hasRandomTicks(state) || world.getBaseLightLevel(pos, 0) < 9) return;
        float moisture = getAvailableMoisture(this, world, pos);
        if (random.nextInt((int) (25.0F / moisture) + 1) == 0) applyGrowth(world, pos, state);
    }

    private boolean hasGrowthSpace(WorldView world, BlockPos pos, BlockState state) {
        for (int part = 1; part < height(Math.min(MAX_AGE, state.get(AGE) + 1)); part++) {
            BlockState above = world.getBlockState(pos.up(part));
            if (!above.isAir() && !(above.isOf(this) && above.get(PART) == part)) return false;
        }
        return pos.getY() + height(Math.min(MAX_AGE, state.get(AGE) + 1)) <= world.getTopY();
    }

    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        if (state.get(PART) != 0 || isMature(state) || !hasGrowthSpace(world, pos, state)) return;
        int age = state.get(AGE) + 1;
        for (int part = 0; part < height(age); part++) {
            world.setBlockState(pos.up(part), getDefaultState().with(AGE, age).with(PART, part), Block.NOTIFY_ALL);
        }
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return state.get(PART) == 0 && !isMature(state) && hasGrowthSpace(world, pos, state);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return isFertilizable(world, pos, state, world.isClient);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && state.get(PART) > 0) {
            BlockPos basePos = pos.down(state.get(PART));
            BlockState base = world.getBlockState(basePos);
            if (base.isOf(this) && base.get(PART) == 0) {
                if (!player.isCreative()) Block.dropStacks(base, world, basePos, null, player, player.getMainHandStack());
                world.setBlockState(basePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && !state.isOf(newState.getBlock())) {
            BlockPos basePos = pos.down(state.get(PART));
            if (state.get(PART) == 0) {
                for (int part = 1; part < height(state.get(AGE)); part++) {
                    BlockPos above = basePos.up(part);
                    BlockState other = world.getBlockState(above);
                    if (other.isOf(this) && other.get(PART) == part)
                        world.setBlockState(above, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            } else {
                BlockState base = world.getBlockState(basePos);
                if (base.isOf(this) && base.get(PART) == 0) world.breakBlock(basePos, true);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int age = state.get(AGE);
        return Block.createCuboidShape(1, 0, 1, 15, state.get(PART) > 0 || age >= 2 ? 16 : 4 + age * 5, 15);
    }
}
