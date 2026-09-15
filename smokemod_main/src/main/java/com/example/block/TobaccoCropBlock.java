package com.example.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class TobaccoCropBlock extends CropBlock {

    public static final int MAX_AGE = 5;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public TobaccoCropBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
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
    protected net.minecraft.item.ItemConvertible getSeedsItem() {
        return com.example.ExampleMod.TOBACCO_SEEDS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    // Только блоки LOWER (нижняя половина) нуждаются в случайных тиках; созревшие растения не растут
    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER && !this.isMature(state);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return;
        super.randomTick(state, world, pos, random);
    }

    // Рост на одну стадию за раз для сбалансированного темпа
    @Override
    protected int getGrowthAmount(World world) {
        return 1;
    }

    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return;

        int currentAge = this.getAge(state);
        int newAge = Math.min(currentAge + 1, MAX_AGE);

        boolean wasTall = currentAge >= 4;
        boolean willBeTall = newAge >= 4;

        if (!wasTall && willBeTall) {
            // Рост из одного блока в двухвысотный: требуется свободное пространство сверху
            BlockPos upperPos = pos.up();
            if (!world.isAir(upperPos)) return;
            world.setBlockState(pos, state.with(AGE, newAge));
            world.setBlockState(upperPos, this.getDefaultState().with(AGE, newAge).with(HALF, DoubleBlockHalf.UPPER));
        } else {
            world.setBlockState(pos, state.with(AGE, newAge));
            if (wasTall) {
                // Синхронизируем возраст с верхним блоком
                BlockPos upperPos = pos.up();
                BlockState upperState = world.getBlockState(upperPos);
                if (upperState.getBlock() == this && upperState.get(HALF) == DoubleBlockHalf.UPPER) {
                    world.setBlockState(upperPos, upperState.with(AGE, newAge));
                }
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.down());
            return below.getBlock() == this && below.get(HALF) == DoubleBlockHalf.LOWER;
        }
        // LOWER: должен быть на грядке (обрабатывается родителем = PlantBlock + CropBlock)
        return super.canPlaceAt(state, world, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            // Верхняя половина всегда должна иметь под собой нашу нижнюю половину
            if (direction == Direction.DOWN) {
                if (neighborState.getBlock() != this || neighborState.get(HALF) != DoubleBlockHalf.LOWER) {
                    return Blocks.AIR.getDefaultState();
                }
            }
            return state;
        }
        // Нижняя половина: используем родительскую логику (проверка грядки и т.д.)
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    // Выпадение дропа идет из таблицы лута, привязанной к half=lower (из upper ничего не выпадает).
    // Когда ломается UPPER, мы вручную вызываем логику выпадения дропа для LOWER перед его удалением.
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            DoubleBlockHalf half = state.get(HALF);
            if (half == DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = pos.down();
                BlockState lowerState = world.getBlockState(lowerPos);
                if (lowerState.getBlock() == this && lowerState.get(HALF) == DoubleBlockHalf.LOWER) {
                    world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                    // afterBreak для UPPER вызовет dropStacks(upperState) -> таблица лута вернет
                    // пустоту для half=upper, поэтому мы вручную выбиваем дроп из нижнего блока (lowerState) здесь.
                    if (!player.isCreative()) {
                        Block.dropStacks(lowerState, world, lowerPos, null, player, player.getMainHandStack());
                    }
                }
            } else {
                // LOWER: ванильный afterBreak обработает дроп через таблицу лута (half=lower -> дроп).
                // Удаляем верхнюю половину без выпадения из неё собственного дропа.
                BlockPos upperPos = pos.up();
                BlockState upperState = world.getBlockState(upperPos);
                if (upperState.getBlock() == this && upperState.get(HALF) == DoubleBlockHalf.UPPER) {
                    world.setBlockState(upperPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return state.get(HALF) == DoubleBlockHalf.LOWER && !this.isMature(state);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER || this.isMature(state)) return false;
        // Переход в высокий рост требует свободного места сверху
        if (state.get(AGE) == 3) return world.isAir(pos.up());
        return true;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return VoxelShapes.fullCube();
        }
        return super.getOutlineShape(state, world, pos, context);
    }
}
