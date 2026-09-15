package com.example.block.entity;

import com.example.ExampleMod;
import com.example.block.JarBlock;
import com.example.screen.JarScreenHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Exercises the actual vanilla click paths as well as persistence and world updates. */
public final class JarRegressionTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void curingBatchPersistenceAndReset(TestContext context) {
        JarCuringClock clock = new JarCuringClock(0);
        clock.advance(120_000_000_000L);
        NbtCompound clockSave = clock.writeNbt(new NbtCompound());
        check(clockSave.getLong("ElapsedMillis") == 120000, "Clock must measure real elapsed seconds");
        JarCuringClock restarted = JarCuringClock.fromNbt(clockSave);
        check(restarted.writeNbt(new NbtCompound()).getLong("ElapsedMillis") == 120000,
                "Restart must restore elapsed time without adding offline time");
        for (JarBlock block : new JarBlock[]{ExampleMod.JAR, ExampleMod.JAR_LARGE}) {
            JarBlockEntity jar = new JarBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
            jar.setStack(0, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, block.getCapacity()));
            NbtCompound saved = new NbtCompound();
            jar.writeNbt(saved);
            long started = saved.getLong("CuringStartedAtRuntime");
            check(started >= 0, "Insertion must start real-time timer");
            JarBlockEntity restored = new JarBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
            restored.readNbt(saved);
            NbtCompound roundTrip = new NbtCompound();
            restored.writeNbt(roundTrip);
            check(roundTrip.getLong("CuringStartedAtRuntime") == started, "Reload must preserve real-time start");
            restored.finishCuring(started + JarBlockEntity.CURING_TIME * 1000L - 1);
            check(restored.getStack(0).isOf(ExampleMod.DRIED_CANNABIS_BUD), "Must not finish early");
            restored.finishCuring(started + JarBlockEntity.CURING_TIME * 1000L);
            check(restored.getStack(0).isOf(ExampleMod.CURED_CANNABIS_BUD)
                    && restored.getStack(0).getCount() == block.getCapacity(), "Must cure whole batch without loss");
            restored.writeNbt(saved);
            jar.readNbt(saved);
            check(jar.getStack(0).isOf(ExampleMod.CURED_CANNABIS_BUD), "Cured output must survive reload");
            jar.setStack(0, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, 3));
            JarBlockEntity.tick(context.getWorld(), BlockPos.ORIGIN, block.getDefaultState(), jar);
            jar.writeNbt(saved);
            saved.putLong("CuringStartedAtRuntime", 0);
            jar.readNbt(saved);

            jar.getStack(0).increment(1);
            jar.markDirty();
            check(jar.getProperties().get(0) == 0, "In-place slot merge must reset batch");
            JarBlockEntity.tick(context.getWorld(), BlockPos.ORIGIN, block.getDefaultState(), jar);
            jar.removeStack(0, 1);
            check(jar.getProperties().get(0) == 0, "Extraction must reset batch");
            jar.setStack(0, new ItemStack(ExampleMod.CANNABIS_BUD, 2));
            JarBlockEntity.tick(context.getWorld(), BlockPos.ORIGIN, block.getDefaultState(), jar);
            check(jar.getProperties().get(0) == 0 && jar.getStack(0).isOf(ExampleMod.CANNABIS_BUD),
                    "Other stored items must not start this recipe");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void capacityAndTransfers(TestContext context) {
        checkTransfers(ExampleMod.JAR, 16);
        checkTransfers(ExampleMod.JAR_LARGE, 64);
        context.complete();
    }

    private static void checkTransfers(JarBlock block, int capacity) {
        JarBlockEntity jar = new JarBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
        PlayerInventory inventory = new PlayerInventory(null);
        JarScreenHandler handler = new JarScreenHandler(0, inventory, jar, capacity);
        check(jar.isEmpty() && !block.getDefaultState().get(JarBlock.FILLED), "New jars must be empty");
        inventory.setStack(9, new ItemStack(ExampleMod.CANNABIS_BUD, 64));
        handler.quickMove(null, 1);
        check(jar.getStack(0).getCount() == capacity, "Shift-click must respect jar capacity");
        check(inventory.getStack(9).getCount() == 64 - capacity, "Excess buds must remain in inventory");
        ItemStack retry = handler.quickMove(null, 1);
        check(retry.isEmpty() && jar.getStack(0).getCount() == capacity, "Full jar must reject Shift-click");
        jar.removeStack(0, 5);
        inventory.setStack(9, new ItemStack(ExampleMod.CANNABIS_BUD, 64));
        handler.quickMove(null, 1);
        check(jar.getStack(0).getCount() == capacity && inventory.getStack(9).getCount() == 59,
                "Merging into an occupied slot must respect its custom limit");
        inventory.setStack(10, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, 8));
        handler.quickMove(null, 2);
        check(inventory.getStack(10).getCount() == 8 && jar.getStack(0).isOf(ExampleMod.CANNABIS_BUD),
                "Different bud types cannot overwrite one another");
        check(!handler.getSlot(0).canInsert(new ItemStack(Items.DIRT)), "Slot must reject other items");
        check(!jar.isValid(0, new ItemStack(ExampleMod.GREEN_CANNABIS_LEAF)), "Automation must reject leaves");
        check(jar.isValid(0, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD)), "Dried buds must be accepted");
        jar.setStack(0, new ItemStack(Items.DIRT));
        check(jar.getStack(0).isOf(ExampleMod.CANNABIS_BUD), "Invalid insertion must not overwrite contents");

        NbtCompound saved = new NbtCompound();
        jar.writeNbt(saved);
        JarBlockEntity restored = new JarBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
        restored.readNbt(saved);
        check(restored.getStack(0).isOf(ExampleMod.CANNABIS_BUD)
                && restored.getStack(0).getCount() == capacity, "Reload must preserve contents");
        inventory.clear();
        handler.quickMove(null, 0);
        check(jar.isEmpty() && count(inventory) == capacity, "Extraction must return every stored bud");
        jar.setStack(0, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, capacity));
        jar.writeNbt(saved);
        restored.readNbt(saved);
        check(restored.getStack(0).isOf(ExampleMod.DRIED_CANNABIS_BUD), "Reload must preserve dried bud type");
        jar.clear();
        inventory.clear();
        inventory.setStack(9, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, capacity));
        handler.quickMove(null, 1);
        check(jar.getStack(0).isOf(ExampleMod.DRIED_CANNABIS_BUD), "Dried buds must transfer into empty jars");
        inventory.clear();
        for (int slot = 0; slot < inventory.size(); slot++) inventory.setStack(slot, new ItemStack(Items.DIRT, 64));
        check(handler.quickMove(null, 0).isEmpty() && jar.getStack(0).getCount() == capacity,
                "Extraction into a full inventory must not lose contents");
        JarScreenHandler client = new JarScreenHandler(1, new PlayerInventory(null), capacity);
        check(client.getCapacity() == capacity && client.getSlot(0).getMaxItemCount() == capacity,
                "Client and server capacity must match");
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void vanillaClicksRespectCapacity(TestContext context) {
        PlayerEntity player = context.createMockSurvivalPlayer();
        for (JarBlock block : new JarBlock[]{ExampleMod.JAR, ExampleMod.JAR_LARGE}) {
            int capacity = block.getCapacity();
            JarBlockEntity jar = new JarBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
            JarScreenHandler handler = new JarScreenHandler(0, player.getInventory(), jar, capacity);
            handler.setCursorStack(new ItemStack(ExampleMod.CANNABIS_BUD, 64));
            handler.onSlotClick(0, 0, SlotActionType.PICKUP, player);
            check(jar.getStack(0).getCount() == capacity && handler.getCursorStack().getCount() == 64 - capacity,
                    "Normal left-click must cap the jar and preserve cursor remainder");
            jar.clear();
            handler.setCursorStack(new ItemStack(ExampleMod.CANNABIS_BUD, 64));
            // Start, add slot, finish left-button drag.
            handler.onSlotClick(-999, 0, SlotActionType.QUICK_CRAFT, player);
            handler.onSlotClick(0, 1, SlotActionType.QUICK_CRAFT, player);
            handler.onSlotClick(-999, 2, SlotActionType.QUICK_CRAFT, player);
            check(jar.getStack(0).getCount() == capacity && handler.getCursorStack().getCount() == 64 - capacity,
                    "Drag placement must cap the jar without deleting excess");
            jar.clear();
            handler.setCursorStack(ItemStack.EMPTY);
            player.getInventory().clear();
            player.getInventory().setStack(0, new ItemStack(ExampleMod.CANNABIS_BUD, 64));
            handler.onSlotClick(0, 0, SlotActionType.SWAP, player);
            check(jar.getStack(0).getCount() == capacity && count(player.getInventory()) == 64 - capacity,
                    "Hotbar number-key insertion must cap the jar and preserve excess");
            jar.clear();
            handler.setCursorStack(new ItemStack(Items.DIRT, 64));
            handler.onSlotClick(0, 0, SlotActionType.PICKUP, player);
            check(jar.isEmpty() && handler.getCursorStack().getCount() == 64,
                    "Normal clicking with a forbidden item must preserve the cursor");
            handler.setCursorStack(new ItemStack(ExampleMod.CANNABIS_BUD, 64));
            handler.onSlotClick(0, 1, SlotActionType.PICKUP, player);
            check(jar.getStack(0).getCount() == 1 && handler.getCursorStack().getCount() == 63,
                    "Right-click insertion must insert exactly one bud");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void filledModelAndBreaking(TestContext context) {
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos pos = context.getAbsolutePos(relative);
        for (JarBlock block : new JarBlock[]{ExampleMod.JAR, ExampleMod.JAR_LARGE}) {
            context.setBlockState(relative, block.getDefaultState());
            JarBlockEntity jar = (JarBlockEntity) context.getWorld().getBlockEntity(pos);
            check(jar != null && jar.isEmpty(), "Placed jar must have an empty inventory");
            jar.setStack(0, new ItemStack(ExampleMod.CANNABIS_BUD, block.getCapacity()));
            check(context.getWorld().getBlockState(pos).get(JarBlock.FILLED), "Adding buds must select filled model");
            check(context.getWorld().getBlockEntity(pos) == jar, "Changing model must keep the block entity");
            check(block.getComparatorOutput(context.getWorld().getBlockState(pos), context.getWorld(), pos) == 15,
                    "Full jar comparator signal must use jar capacity");
            jar.removeStack(0);
            check(!context.getWorld().getBlockState(pos).get(JarBlock.FILLED), "Removing last bud must select empty model");
            jar.setStack(0, new ItemStack(ExampleMod.DRIED_CANNABIS_BUD, 12));
            context.getWorld().breakBlock(pos, true);
            check(context.getWorld().getBlockState(pos).isOf(Blocks.AIR), "Breaking must not restore the jar");
            int droppedBuds = 0;
            int droppedJars = 0;
            for (ItemEntity item : context.getWorld().getEntitiesByClass(ItemEntity.class, new Box(pos).expand(1), e -> true)) {
                if (item.getStack().isOf(ExampleMod.DRIED_CANNABIS_BUD)) droppedBuds += item.getStack().getCount();
                if (item.getStack().isOf(block.asItem())) droppedJars += item.getStack().getCount();
                item.discard();
            }
            check(droppedBuds == 12 && droppedJars == 1, "Breaking must drop the empty jar and contents exactly once");
        }
        context.complete();
    }

    private static int count(PlayerInventory inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (JarBlockEntity.isBud(inventory.getStack(slot))) total += inventory.getStack(slot).getCount();
        }
        return total;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
