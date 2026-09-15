package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.JarBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.slot.Slot;

public class JarScreenHandler extends ScreenHandler {
    public static final int SMALL_CAPACITY = 16;
    public static final int LARGE_CAPACITY = 64;
    public static final int JAR_SLOT_X = 80;
    public static final int JAR_SLOT_Y = 57;
    private static final int PLAYER_START = 1;
    private static final int HOTBAR_START = 28;
    private static final int PLAYER_END = 37;

    private final Inventory inventory;
    private final int capacity;
    private final PropertyDelegate properties;

    public JarScreenHandler(int syncId, PlayerInventory playerInventory, int capacity) {
        this(syncId, playerInventory, new SimpleInventory(1), capacity);
    }

    public JarScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int capacity) {
        super(capacity == SMALL_CAPACITY ? ExampleMod.JAR_SCREEN_HANDLER : ExampleMod.JAR_LARGE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 1);
        this.inventory = inventory;
        this.capacity = capacity;
        properties = inventory instanceof JarBlockEntity jar ? jar.getProperties() : new ArrayPropertyDelegate(1);
        addProperties(properties);
        inventory.onOpen(playerInventory.player);
        addSlot(new Slot(inventory, 0, JAR_SLOT_X, JAR_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) { return JarBlockEntity.isBud(stack); }

            @Override
            public int getMaxItemCount() { return capacity; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, 146 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 204));
        }
    }

    public int getCapacity() { return capacity; }

    public int getStoredCount() { return inventory.getStack(0).getCount(); }

    public boolean isCuring() { return inventory.getStack(0).isOf(ExampleMod.DRIED_CANNABIS_BUD); }
    public boolean isCured() { return inventory.getStack(0).isOf(ExampleMod.CURED_CANNABIS_BUD); }
    public int getCuringSeconds() { return properties.get(0); }
    public float getCuringProgress() {
        return isCured() ? 1F : Math.min(1F, Math.max(0F, (float) getCuringSeconds() / JarBlockEntity.CURING_TIME));
    }

    @Override
    public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = source.getStack();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!insertItem(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (JarBlockEntity.isBud(stack)) {
            Slot target = slots.get(0);
            ItemStack stored = target.getStack();
            if (!stored.isEmpty() && !ItemStack.canCombine(stored, stack)) return ItemStack.EMPTY;
            // Vanilla insertItem's merge pass can exceed a custom slot's stack limit.
            int moved = Math.min(stack.getCount(), target.getMaxItemCount(stack) - stored.getCount());
            if (moved <= 0) return ItemStack.EMPTY;
            if (stored.isEmpty()) target.setStack(stack.split(moved));
            else {
                stored.increment(moved);
                stack.decrement(moved);
                target.markDirty();
            }
        } else if (index < HOTBAR_START) {
            if (!insertItem(stack, HOTBAR_START, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, PLAYER_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setStack(ItemStack.EMPTY);
        else source.markDirty();
        source.onTakeItem(player, stack);
        return original;
    }
}
