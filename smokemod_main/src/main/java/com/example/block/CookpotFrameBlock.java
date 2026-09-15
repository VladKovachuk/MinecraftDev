package com.example.block;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

/** A reusable metal hearth. Its contained flame never places or spreads a fire block. */
public class CookpotFrameBlock extends Block implements Waterloggable {
    public static final BooleanProperty LIT = Properties.LIT;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape SHAPE = VoxelShapes.union(
            createCuboidShape(1, 0, 1, 3, 14, 3), createCuboidShape(13, 0, 1, 15, 14, 3),
            createCuboidShape(1, 0, 13, 3, 14, 15), createCuboidShape(13, 0, 13, 15, 14, 15),
            createCuboidShape(1, 14, 1, 15, 16, 3), createCuboidShape(1, 14, 13, 15, 16, 15),
            createCuboidShape(1, 14, 3, 3, 16, 13), createCuboidShape(13, 14, 3, 15, 16, 13),
            createCuboidShape(3, 15, 5, 13, 16, 6), createCuboidShape(3, 15, 10, 13, 16, 11),
            createCuboidShape(3, 1, 3, 13, 3, 13));

    public CookpotFrameBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(LIT, false).with(WATERLOGGED, false));
    }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(LIT, WATERLOGGED); }
    @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return SHAPE; }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getFluid() == Fluids.WATER);
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor,
                                                         WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
    }
    @Override public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluid) {
        if (state.get(WATERLOGGED) || fluid.getFluid() != Fluids.WATER) return false;
        if (!world.isClient()) {
            if (state.get(LIT)) world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
            world.setBlockState(pos, state.with(WATERLOGGED, true).with(LIT, false), 3);
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return true;
    }
    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                        Hand hand, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);
        boolean flint = held.isOf(Items.FLINT_AND_STEEL), charge = held.isOf(Items.FIRE_CHARGE);
        if (flint || charge) {
            if (!world.isClient && !state.get(LIT) && !state.get(WATERLOGGED)) {
                world.setBlockState(pos, state.with(LIT, true), 3);
                world.playSound(null, pos, flint ? SoundEvents.ITEM_FLINTANDSTEEL_USE : SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.BLOCKS, 1, 1);
                world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                if (flint) held.damage(1, player, p -> p.sendToolBreakStatus(hand));
                else if (!player.getAbilities().creativeMode) held.decrement(1);
            }
            // Consume an ignition click even when wet/already lit so it cannot ignite an adjacent block.
            return ActionResult.success(world.isClient);
        }
        if (held.getItem() instanceof ShovelItem && state.get(LIT)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(LIT, false), 3);
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
                world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                held.damage(1, player, p -> p.sendToolBreakStatus(hand));
            }
            return ActionResult.success(world.isClient);
        }
        if (held.isOf(Items.WATER_BUCKET)) {
            if (!world.isClient && tryFillWithFluid(world, pos, state, Fluids.WATER.getStill(false))) {
                player.setStackInHand(hand, ItemUsage.exchangeStack(held, player, new ItemStack(Items.BUCKET)));
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1, 1);
            }
            return ActionResult.success(world.isClient);
        }
        // Let vanilla bucket pickup and placing the pot on the frame use the normal item path.
        return ActionResult.PASS;
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT) && !state.get(WATERLOGGED)) {
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + .3 + random.nextDouble() * .4,
                    pos.getY() + .8, pos.getZ() + .3 + random.nextDouble() * .4, 0, .025, 0);
            if (random.nextInt(16) == 0) world.playSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, .35f, .9f + random.nextFloat() * .2f, false);
        }
    }
}
