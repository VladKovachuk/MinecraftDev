package com.example.block.entity;

import com.example.ExampleMod;
import com.example.block.CookpotBlock;
import com.example.block.CookpotFrameBlock;
import com.example.screen.CookpotScreenHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public final class CookpotRegressionTest implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void batchPauseReloadAndBothCoals(TestContext context) {
        for (Item coal : new Item[]{Items.COAL, Items.CHARCOAL}) {
            CookpotBlockEntity pot = ready(coal);
            advance(pot, 1500, false);
            check(pot.properties.get(1) == 0 && pot.getStack(5).isEmpty(), "Cannot cook without fire");
            advance(pot, 600, true);
            advance(pot, 100, false);
            check(pot.properties.get(1) == 600, "Fire loss pauses progress");
            NbtCompound save = new NbtCompound();
            pot.writeNbt(save);
            CookpotBlockEntity restored = pot();
            restored.readNbt(save);
            advance(restored, 599, true);
            check(restored.getStack(5).isEmpty(), "Reload must not finish early");
            advance(restored, 1, true);
            check(restored.getStack(5).isOf(ExampleMod.REFINED_OPIUM) && restored.getStack(5).getCount() == 1,
                    "Exactly one output per batch with either coal");
            check(restored.properties.get(0) == 0 && restored.properties.get(1) == 0, "Batch consumes all three water units");
            check(restored.getStack(2).getCount() == 1 && restored.getStack(4).getCount() == 1, "Batch consumes one calcite and raw opium");
            check(restored.getStack(3).getCount() == 4, "Batch consumes exactly four coal or charcoal");
            advance(restored, 1500, true);
            check(restored.getStack(5).getCount() == 1, "Second batch needs more water");
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bucketsCapacityAndBlockedOutput(TestContext context) {
        CookpotBlockEntity pot = pot();
        for (int i = 0; i < 3; i++) {
            pot.setStack(0, new ItemStack(Items.WATER_BUCKET));
            pot.tickCooking(false);
        }
        check(pot.properties.get(0) == 3 && pot.getStack(1).getCount() == 3, "Each water bucket returns an empty bucket");
        pot.setStack(0, new ItemStack(Items.WATER_BUCKET));
        pot.tickCooking(false);
        check(pot.getStack(0).isOf(Items.WATER_BUCKET) && !pot.addWaterBucket(), "Full tank preserves the fourth bucket");
        CookpotBlockEntity blockedBuckets = pot();
        blockedBuckets.setStack(0, new ItemStack(Items.WATER_BUCKET));
        blockedBuckets.setStack(1, new ItemStack(Items.BUCKET, 16));
        blockedBuckets.tickCooking(false);
        check(blockedBuckets.properties.get(0) == 0 && blockedBuckets.getStack(0).isOf(Items.WATER_BUCKET),
                "A full empty-bucket slot blocks filling without losing the bucket");
        CookpotBlockEntity cooking = ready(Items.COAL);
        advance(cooking, 1199, true);
        cooking.setStack(5, new ItemStack(ExampleMod.REFINED_OPIUM, 64));
        advance(cooking, 1200, true);
        check(cooking.properties.get(1) == 1199 && cooking.properties.get(0) == 3
                && cooking.getStack(2).getCount() == 2, "Full output pauses and preserves the entire batch");
        cooking.removeStack(5);
        cooking.tickCooking(true);
        check(cooking.getStack(5).getCount() == 1 && cooking.properties.get(0) == 0, "Clearing output resumes batch");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void screenFiltersAndShiftClick(TestContext context) {
        CookpotBlockEntity pot = pot();
        PlayerInventory player = new PlayerInventory(context.createMockSurvivalPlayer());
        CookpotScreenHandler handler = new CookpotScreenHandler(1, player, pot, pot.properties);
        Item[] inputs = {Items.WATER_BUCKET, Items.CALCITE, Items.CHARCOAL, ExampleMod.RAW_OPIUM};
        int[] targetSlots = {0, 2, 3, 4};
        for (int i = 0; i < inputs.length; i++) {
            player.setStack(9, new ItemStack(inputs[i]));
            handler.quickMove(player.player, 6);
            check(pot.getStack(targetSlots[i]).isOf(inputs[i]) && player.getStack(9).isEmpty(), "Shift-click input routing");
        }
        for (int i = 0; i < 6; i++) {
            check(!handler.slots.get(i).canInsert(new ItemStack(Items.DIRT)), "Reject invalid ingredients");
        }
        check(!handler.slots.get(1).canInsert(new ItemStack(Items.BUCKET))
                && !handler.slots.get(5).canInsert(new ItemStack(ExampleMod.REFINED_OPIUM)), "Output slots are extraction only");
        pot.tickCooking(false);
        handler.quickMove(player.player, 1);
        check(pot.getStack(1).isEmpty() && player.count(Items.BUCKET) == 1, "Shift-click returns the empty bucket");
        pot.setStack(5, new ItemStack(ExampleMod.REFINED_OPIUM, 4));
        handler.quickMove(player.player, 5);
        check(pot.getStack(5).isEmpty() && player.count(ExampleMod.REFINED_OPIUM) == 4, "Shift-click extracts result exactly once");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void missingInputsAndNbtClamping(TestContext context) {
        for (int missing = 2; missing <= 4; missing++) {
            CookpotBlockEntity pot = ready(Items.COAL);
            advance(pot, 100, true);
            pot.removeStack(missing);
            advance(pot, 1300, true);
            check(pot.getStack(5).isEmpty() && pot.properties.get(1) == 0 && pot.properties.get(0) == 3,
                    "Every ingredient is required and removing it resets the batch");
        }
        CookpotBlockEntity pot = ready(Items.COAL);
        NbtCompound save = new NbtCompound();
        pot.writeNbt(save);
        save.putInt("WaterBuckets", -100);
        save.putInt("CookProgress", Integer.MAX_VALUE);
        pot.readNbt(save);
        check(pot.properties.get(0) == 0 && pot.properties.get(1) == 0, "Invalid water cannot grant progress");
        save.putInt("WaterBuckets", 500);
        pot.readNbt(save);
        check(pot.properties.get(0) == 3 && pot.properties.get(1) == 1199, "NBT values are bounded");
        pot.clear();
        pot.tickCooking(true);
        check(pot.isEmpty() && pot.properties.get(1) == 0, "Clear cancels the pending batch");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void actualWorldFireStateAndBreaking(TestContext context) {
        var world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 3, 1));
        world.setBlockState(pos.down(), ExampleMod.COOKPOT_FRAME.getDefaultState().with(CookpotFrameBlock.LIT, true));
        world.setBlockState(pos, ExampleMod.COOKPOT.getDefaultState());
        CookpotBlockEntity pot = (CookpotBlockEntity) world.getBlockEntity(pos);
        check(pot != null, "Block entity is registered");
        for (int i = 0; i < 3; i++) pot.addWaterBucket();
        ingredients(pot, Items.COAL);
        CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(world.getBlockState(pos).get(CookpotBlock.LIT)
                && world.getBlockState(pos).get(CookpotBlock.WATER_LEVEL) == 3, "World state shows water and cooking");
        world.setBlockState(pos.down(), ExampleMod.COOKPOT_FRAME.getDefaultState());
        CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(!world.getBlockState(pos).get(CookpotBlock.LIT) && pot.properties.get(1) == 1, "Extinguishing the frame pauses");
        world.setBlockState(pos.down(), ExampleMod.COOKPOT_FRAME.getDefaultState().with(CookpotFrameBlock.LIT, true));
        CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(pot.properties.get(1) == 2, "Relighting resumes progress");
        world.breakBlock(pos, true);
        var drops = world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(2), item -> true);
        check(drops.stream().filter(e -> e.getStack().isOf(ExampleMod.RAW_OPIUM)).mapToInt(e -> e.getStack().getCount()).sum() == 2,
                "Breaking the cookpot returns its stored ingredients exactly once");
        check(drops.stream().anyMatch(e -> e.getStack().isOf(ExampleMod.COOKPOT_ITEM)), "Cookpot itself drops");
        check(world.getRecipeManager().get(new Identifier("smokemod", "cookpot")).isPresent()
                && world.getRecipeManager().get(new Identifier("smokemod", "cookpot_frame")).isPresent(), "Crafting recipes load");
        check(world.getRecipeManager().get(new Identifier("smokemod", "calcium_block")).isEmpty(), "Old calcium recipe is removed");
        check(!net.minecraft.registry.Registries.BLOCK.containsId(new Identifier("smokemod", "calcium_block"))
                && !net.minecraft.registry.Registries.ITEM.containsId(new Identifier("smokemod", "calcium_block")), "Old calcium block and item are removed");
        // Unrequested heat sources still do not heat the pot.
        for (var other : new net.minecraft.block.Block[]{Blocks.SOUL_FIRE, Blocks.LAVA, Blocks.CAMPFIRE}) {
            world.setBlockState(pos.down(), other.getDefaultState(), 2);
            check(!CookpotBlockEntity.hasHeatBelow(world, pos), "Standalone fire/lava/campfire must not heat the pot");
        }
        context.complete();
    }

    private static CookpotBlockEntity pot() {
        return new CookpotBlockEntity(BlockPos.ORIGIN, ExampleMod.COOKPOT.getDefaultState());
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void ordinaryFireHeatsFreestandingPot(TestContext context) {
        var world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 3, 1));
        world.setBlockState(pos.down(2), Blocks.NETHERRACK.getDefaultState());
        world.setBlockState(pos, ExampleMod.COOKPOT.getDefaultState());
        var pot = (CookpotBlockEntity) world.getBlockEntity(pos);
        ingredients(pot, Items.COAL);
        for (int i = 0; i < 3; i++) pot.addWaterBucket();
        world.setBlockState(pos.down(), Blocks.FIRE.getDefaultState());
        for (int i = 0; i < 600; i++) CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(pot.properties.get(1) == 600 && world.getBlockState(pos).get(CookpotBlock.LIT), "Ordinary fire heats the pot without a frame");
        world.setBlockState(pos.down(), Blocks.AIR.getDefaultState());
        CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(world.getBlockEntity(pos) == pot && pot.properties.get(1) == 600, "Extinguishing fire preserves the pot and pauses progress");
        world.setBlockState(pos.down(), Blocks.FIRE.getDefaultState());
        for (int i = 0; i < 600; i++) CookpotBlockEntity.tick(world, pos, world.getBlockState(pos), pot);
        check(pot.getStack(5).isOf(ExampleMod.REFINED_OPIUM) && pot.getStack(3).getCount() == 4, "Relighting completes the normal recipe");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void placementOnFrameAndDirectBucket(TestContext context) {
        var world = context.getWorld();
        var player = context.createMockSurvivalPlayer();
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        world.setBlockState(base, Blocks.STONE.getDefaultState());
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(ExampleMod.COOKPOT_ITEM));
        var hit = new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(base).add(0, .5, 0),
                net.minecraft.util.math.Direction.UP, base, false);
        var usage = new net.minecraft.item.ItemUsageContext(player, net.minecraft.util.Hand.MAIN_HAND, hit);
        check(ExampleMod.COOKPOT_ITEM.place(new net.minecraft.item.ItemPlacementContext(usage)).isAccepted(), "Cookpot can be placed on ordinary stone");
        world.setBlockState(base.up(), Blocks.AIR.getDefaultState());
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(ExampleMod.COOKPOT_ITEM));
        usage = new net.minecraft.item.ItemUsageContext(player, net.minecraft.util.Hand.MAIN_HAND, hit);
        world.setBlockState(base, ExampleMod.COOKPOT_FRAME.getDefaultState());
        var result = ExampleMod.COOKPOT_ITEM.place(new net.minecraft.item.ItemPlacementContext(usage));
        check(result.isAccepted() && world.getBlockState(base).isOf(ExampleMod.COOKPOT_FRAME)
                && world.getBlockState(base.up()).isOf(ExampleMod.COOKPOT), "Cookpot is placed directly on its frame");
        check(player.getMainHandStack().isEmpty(), "Survival placement consumes exactly one cookpot");
        var pot = (CookpotBlockEntity) world.getBlockEntity(base.up());
        for (int i = 0; i < 3; i++) {
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            ExampleMod.COOKPOT.onUse(world.getBlockState(base.up()), world, base.up(), player, net.minecraft.util.Hand.MAIN_HAND, hit);
            check(player.getMainHandStack().isOf(Items.BUCKET), "Direct pouring returns the bucket in survival");
        }
        check(pot.properties.get(0) == 3, "Direct bucket interaction fills the tank");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        ExampleMod.COOKPOT.onUse(world.getBlockState(base.up()), world, base.up(), player, net.minecraft.util.Hand.MAIN_HAND, hit);
        check(player.getMainHandStack().isOf(Items.WATER_BUCKET), "Fourth direct bucket is preserved");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(ExampleMod.COOKPOT_ITEM));
        usage = new net.minecraft.item.ItemUsageContext(player, net.minecraft.util.Hand.MAIN_HAND, hit);
        check(!ExampleMod.COOKPOT_ITEM.place(new net.minecraft.item.ItemPlacementContext(usage)).isAccepted(),
                "An occupied block above the frame cannot be overwritten");
        check(player.getMainHandStack().isOf(ExampleMod.COOKPOT_ITEM) && world.getBlockEntity(base.up()) == pot
                && pot.properties.get(0) == 3, "Failed placement preserves the existing pot and held item");
        context.complete();
    }
    private static void ingredients(CookpotBlockEntity pot, Item coal) {
        pot.setStack(2, new ItemStack(Items.CALCITE, 2));
        pot.setStack(3, new ItemStack(coal, 8));
        pot.setStack(4, new ItemStack(ExampleMod.RAW_OPIUM, 2));
    }
    private static CookpotBlockEntity ready(Item coal) {
        CookpotBlockEntity pot = pot();
        ingredients(pot, coal);
        for (int i = 0; i < 3; i++) pot.addWaterBucket();
        return pot;
    }
    private static void advance(CookpotBlockEntity pot, int ticks, boolean heated) {
        for (int i = 0; i < ticks; i++) pot.tickCooking(heated);
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fourCoalRequiredForEveryBatch(TestContext context) {
        for (Item coal : new Item[]{Items.COAL, Items.CHARCOAL}) {
            for (int count = 0; count < 4; count++) {
                var pot = ready(coal);
                pot.setStack(3, new ItemStack(coal, count));
                advance(pot, 1300, true);
                check(pot.getStack(5).isEmpty() && pot.properties.get(0) == 3
                        && pot.getStack(2).getCount() == 2 && pot.getStack(3).getCount() == count,
                        "Fewer than four coal cannot cook or consume anything");
            }
            var pot = ready(coal);
            advance(pot, 1200, true);
            for (int i = 0; i < 3; i++) pot.addWaterBucket();
            advance(pot, 1200, true);
            check(pot.getStack(5).getCount() == 2 && pot.getStack(3).isEmpty(), "Two batches consume exactly eight coal");
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void igniteExtinguishWaterAndSupportLoss(TestContext context) {
        var world = context.getWorld();
        var player = context.createMockSurvivalPlayer();
        var hand = net.minecraft.util.Hand.MAIN_HAND;
        BlockPos frame = context.getAbsolutePos(new BlockPos(1, 2, 1));
        world.setBlockState(frame, ExampleMod.COOKPOT_FRAME.getDefaultState());
        world.setBlockState(frame.up(), ExampleMod.COOKPOT.getDefaultState());
        var hit = new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(frame),
                net.minecraft.util.math.Direction.NORTH, frame, false);
        player.setStackInHand(hand, new ItemStack(Items.FLINT_AND_STEEL));
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(world.getBlockState(frame).get(CookpotFrameBlock.LIT) && player.getMainHandStack().getDamage() == 1,
                "Flint and steel lights the contained fire and costs one durability");
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(player.getMainHandStack().getDamage() == 1, "Already-lit frame does not waste durability");
        player.setStackInHand(hand, new ItemStack(Items.IRON_SHOVEL));
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(!world.getBlockState(frame).get(CookpotFrameBlock.LIT) && player.getMainHandStack().getDamage() == 1,
                "Shovel extinguishes the frame without removing it");
        player.setStackInHand(hand, new ItemStack(Items.FIRE_CHARGE, 2));
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(world.getBlockState(frame).get(CookpotFrameBlock.LIT) && player.getMainHandStack().getCount() == 1,
                "Fire charge relights and consumes one charge");
        player.setStackInHand(hand, new ItemStack(Items.WATER_BUCKET));
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(world.getBlockState(frame).get(CookpotFrameBlock.WATERLOGGED) && !world.getBlockState(frame).get(CookpotFrameBlock.LIT)
                && player.getMainHandStack().isOf(Items.BUCKET), "Water extinguishes and waterlogs, returning the bucket");
        player.setStackInHand(hand, new ItemStack(Items.FIRE_CHARGE));
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(!CookpotBlockEntity.hasHeatBelow(world, frame.up()) && player.getMainHandStack().getCount() == 1,
                "Wet frame cannot ignite or consume a fire charge");
        ItemStack water = ExampleMod.COOKPOT_FRAME.tryDrainFluid(world, frame, world.getBlockState(frame));
        check(water.isOf(Items.WATER_BUCKET) && !world.getBlockState(frame).get(CookpotFrameBlock.WATERLOGGED), "Water can be picked up again");
        ExampleMod.COOKPOT_FRAME.onUse(world.getBlockState(frame), world, frame, player, hand, hit);
        check(CookpotBlockEntity.hasHeatBelow(world, frame.up()), "Drained frame can be relit");
        var pot = (CookpotBlockEntity) world.getBlockEntity(frame.up());
        ingredients(pot, Items.COAL);
        world.breakBlock(frame, true);
        check(world.getBlockEntity(frame.up()) == pot, "Removing the frame preserves the pot");
        CookpotBlockEntity.tick(world, frame.up(), world.getBlockState(frame.up()), pot);
        check(pot.getStack(3).getCount() == 8 && pot.properties.get(2) == CookpotBlockEntity.NO_HEAT, "Unsupported pot retains inventory and stops cooking");
        world.breakBlock(frame.up(), true);
        var drops = world.getEntitiesByClass(ItemEntity.class, new Box(frame).expand(2), e -> true);
        check(drops.stream().filter(e -> e.getStack().isOf(ExampleMod.COOKPOT_ITEM)).mapToInt(e -> e.getStack().getCount()).sum() == 1,
                "Unsupported pot drops exactly once");
        check(drops.stream().filter(e -> e.getStack().isOf(Items.COAL)).mapToInt(e -> e.getStack().getCount()).sum() == 8,
                "Support loss preserves the inventory");
        check(drops.stream().anyMatch(e -> e.getStack().isOf(ExampleMod.COOKPOT_FRAME_ITEM)), "Frame itself drops");
        context.complete();
    }
    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
