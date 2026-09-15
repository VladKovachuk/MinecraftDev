package com.example.block;

import com.example.ExampleMod;
import com.example.block.entity.CookpotBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CookpotBlock extends BlockWithEntity {
    public static final BooleanProperty LIT = Properties.LIT;
    public static final BooleanProperty FINISHED = BooleanProperty.of("finished");
    public static final IntProperty WATER_LEVEL = IntProperty.of("water_level", 0, 3);
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2, 0, 2, 14, 9.1, 14),
            Block.createCuboidShape(0, 7, 5.7, 2, 10.5, 10.3),
            Block.createCuboidShape(14, 7, 5.7, 16, 10.5, 10.3));

    public CookpotBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(LIT, false).with(WATER_LEVEL, 0).with(FINISHED, false));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATER_LEVEL, FINISHED);
    }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CookpotBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, ExampleMod.COOKPOT_BLOCK_ENTITY, CookpotBlockEntity::tick);
    }
    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                        Hand hand, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CookpotBlockEntity pot) {
            if (player.getStackInHand(hand).isOf(Items.WATER_BUCKET)) {
                if (pot.addWaterBucket()) {
                    player.setStackInHand(hand, ItemUsage.exchangeStack(player.getStackInHand(hand), player,
                            new ItemStack(Items.BUCKET)));
                    world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1, 1);
                }
            } else {
                player.openHandledScreen(pot);
            }
        }
        return ActionResult.success(world.isClient);
    }
    @Override public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof CookpotBlockEntity pot) {
            ItemScatterer.spawn(world, pos, pot);
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            world.addParticle(ParticleTypes.CLOUD, pos.getX() + .3 + random.nextDouble() * .4,
                    pos.getY() + .7, pos.getZ() + .3 + random.nextDouble() * .4, 0, .025, 0);
        }
    }
}
