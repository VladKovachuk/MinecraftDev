package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.CookpotBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

public class CookpotScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;

    public CookpotScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CookpotBlockEntity.SIZE), new ArrayPropertyDelegate(3));
    }
    public CookpotScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties) {
        super(ExampleMod.COOKPOT_SCREEN_HANDLER, syncId);
        checkSize(inventory, CookpotBlockEntity.SIZE);
        checkDataCount(properties, 3);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);
        addProperties(properties);
        int[][] positions = {{20, 12}, {20, 36}, {141, 12}, {141, 36}, {141, 60}, {80, 36}};
        for (int i = 0; i < CookpotBlockEntity.SIZE; i++) {
            final int slot = i;
            addSlot(new Slot(inventory, i, positions[i][0], positions[i][1]) {
                @Override public boolean canInsert(ItemStack stack) { return CookpotBlockEntity.accepts(slot, stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 118 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
    }
    public int getWater() { return properties.get(0); }
    public int getProgress() { return properties.get(1); }
    public int getStatus() { return properties.get(2); }
    public boolean hasHeat() { return getStatus() != CookpotBlockEntity.NO_HEAT; }
    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }
    @Override public void onClosed(PlayerEntity player) { super.onClosed(player); inventory.onClose(player); }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        int firstPlayerSlot = CookpotBlockEntity.SIZE;
        if (index < firstPlayerSlot) {
            if (!insertItem(stack, firstPlayerSlot, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int target = -1;
            for (int i = 0; i < firstPlayerSlot; i++) if (CookpotBlockEntity.accepts(i, stack)) { target = i; break; }
            if (target >= 0) {
                if (!insertItem(stack, target, target + 1, false)) return ItemStack.EMPTY;
            } else if (index < firstPlayerSlot + 27) {
                if (!insertItem(stack, firstPlayerSlot + 27, slots.size(), false)) return ItemStack.EMPTY;
            } else if (!insertItem(stack, firstPlayerSlot, firstPlayerSlot + 27, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, stack);
        return original;
    }
}
