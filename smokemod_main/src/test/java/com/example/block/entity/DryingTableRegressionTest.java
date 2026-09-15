package com.example.block.entity;

import com.example.ExampleMod;
import com.example.screen.DryingTableScreenHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Run with ./gradlew runGametest; test sources are excluded from the mod JAR. */
public final class DryingTableRegressionTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void independentDrying(TestContext context) {
        DryingTableBlockEntity table = table();
        table.setStack(1, new ItemStack(ExampleMod.GREEN_TOBACCO_LEAF));
        advance(table, 1199);
        table.setStack(2, new ItemStack(ExampleMod.CANNABIS_BUD));
        table.setStack(9, new ItemStack(ExampleMod.GREEN_CANNABIS_LEAF));
        advance(table, 1);
        check(table.getStack(1).isOf(ExampleMod.DRIED_TOBACCO_LEAF), "Old leaf should finish");
        check(table.getStack(2).isOf(ExampleMod.CANNABIS_BUD), "Late bud must stay wet");
        check(table.getStack(9).isOf(ExampleMod.GREEN_CANNABIS_LEAF), "Late leaf must stay wet");
        check(table.propertyDelegate.get(1) == 1 && table.propertyDelegate.get(8) == 1,
                "New slots should have only one elapsed tick");

        NbtCompound saved = new NbtCompound();
        table.writeNbt(saved);
        DryingTableBlockEntity restored = table();
        restored.readNbt(saved);
        advance(restored, 1198);
        check(restored.getStack(2).isOf(ExampleMod.CANNABIS_BUD), "Reload must preserve remaining time");
        advance(restored, 1);
        check(restored.getStack(2).isOf(ExampleMod.DRIED_CANNABIS_BUD), "Bud should finish on its 1200th tick");
        check(restored.getStack(9).isOf(ExampleMod.DRIED_CANNABIS_LEAF), "Leaf should finish on its 1200th tick");

        table.removeStack(2, 1);
        check(table.propertyDelegate.get(1) == 0, "Partial removal API must reset its slot");
        table.setStack(2, new ItemStack(ExampleMod.CANNABIS_BUD));
        advance(table, 600);
        table.setStack(2, new ItemStack(ExampleMod.CANNABIS_BUD));
        check(table.propertyDelegate.get(1) == 0, "Replacing with the same item type must reset time");
        check(table.propertyDelegate.get(8) == 601, "Replacement must not reset another slot");
        table.removeStack(9);
        check(table.propertyDelegate.get(8) == 0, "Full removal API must reset its slot");

        DryingTableBlockEntity legacy = table();
        saved.remove("dryingTicks");
        saved.putFloat("dryingProgress", 0.999f);
        legacy.readNbt(saved);
        advance(legacy, 1);
        check(legacy.getStack(2).isOf(ExampleMod.CANNABIS_BUD), "Legacy common timer must not dry late items");
        check(legacy.propertyDelegate.get(1) == 1, "Legacy cycle should restart safely");

        PlayerInventory playerInventory = new PlayerInventory(null);
        DryingTableBlockEntity shifted = table();
        DryingTableScreenHandler handler = new DryingTableScreenHandler(0, playerInventory, shifted, shifted.propertyDelegate);
        shifted.setStack(1, new ItemStack(ExampleMod.GREEN_TOBACCO_LEAF));
        advance(shifted, 1199);
        playerInventory.setStack(9, new ItemStack(ExampleMod.CANNABIS_BUD, 12));
        handler.quickMove(null, 9);
        check(playerInventory.getStack(9).getCount() == 4, "Shift-click must preserve excess items");
        advance(shifted, 1);
        for (int slot = 2; slot <= 9; slot++) {
            check(shifted.getStack(slot).isOf(ExampleMod.CANNABIS_BUD), "Shift-clicked items must stay wet");
            check(shifted.propertyDelegate.get(slot - 1) == 1, "Shift-clicked slots must start independently");
        }
        handler.quickMove(null, 1);
        check(shifted.getStack(2).isEmpty() && shifted.propertyDelegate.get(1) == 0,
                "Shift-click extraction must reset progress");
        shifted.clear();
        for (int slot = 0; slot < 9; slot++) check(shifted.propertyDelegate.get(slot) == 0, "Clear must reset all slots");
        System.out.println("Drying table regression checks passed: staggered insertion, all recipes, reload, replacement, removal, legacy saves, Shift-click and clear.");
        context.complete();
    }

    private static DryingTableBlockEntity table() {
        return new DryingTableBlockEntity(BlockPos.ORIGIN, ExampleMod.DRYING_TABLE.getDefaultState());
    }

    private static void advance(DryingTableBlockEntity table, int ticks) {
        for (int tick = 0; tick < ticks; tick++) table.tickDrying();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
