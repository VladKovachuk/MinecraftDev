package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.DryingTableBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Слоты:
 *   0–8   — 3×3 входные (inventory slots 1–9, grid x=55+col*24, y=22+row*24)
 *   9–35  — инвентарь игрока (3 ряда, шаг X=18.25, y=118/135/154)
 *   36–44 — хотбар игрока (y=176)
 *
 * Slot результата (inventory slot 0) в GUI не показывается — сушёные листья
 * заменяют зелёные прямо на своих позициях.
 *
 * X-шаг для стандартных слотов не ровно 18, а 18.25 — так как текстура
 * 891×1001 растянута в панель 176×200 (91.25 tex px на слот при GUI 18.25).
 */
public class DryingTableScreenHandler extends ScreenHandler {

    private final Inventory blockInventory;
    private final PropertyDelegate propertyDelegate;

    // Client-side constructor called by ScreenHandlerRegistry factory
    public DryingTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(10),
                new ArrayPropertyDelegate(DryingTableBlockEntity.INPUT_SLOTS));
    }

    // Server-side constructor called by BlockEntity.createMenu()
    public DryingTableScreenHandler(int syncId, PlayerInventory playerInventory,
                                    Inventory blockInventory, PropertyDelegate propertyDelegate) {
        super(ExampleMod.DRYING_TABLE_SCREEN_HANDLER, syncId);
        checkSize(blockInventory, 10);
        checkDataCount(propertyDelegate, DryingTableBlockEntity.INPUT_SLOTS);
        this.blockInventory = blockInventory;
        this.propertyDelegate = propertyDelegate;
        blockInventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 3×3 входной grid (inventory slots 1–9, ScreenHandler slots 0–8)
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                this.addSlot(new Slot(blockInventory, 1 + col * 3 + row, 55 + col * 24, 22 + row * 24) {
                    @Override
                    public int getMaxItemCount() { return 1; }
                });
            }
        }

        // Инвентарь игрока (3 ряда)
        // Y-позиции точно совпадают с тёмными квадратами текстуры 891×1001→176×200
        int[] invRowY = {118, 135, 154};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 7 + Math.round(col * 18.25f);
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, invRowY[row]));
            }
        }

        // Хотбар
        for (int col = 0; col < 9; col++) {
            int x = 7 + Math.round(col * 18.25f);
            this.addSlot(new Slot(playerInventory, col, x, 176));
        }
    }

    /** Progress for a table slot in screen order (0..8). */
    public float getDryingProgress(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= DryingTableBlockEntity.INPUT_SLOTS) return 0f;
        return propertyDelegate.get(slotIndex) / (float) DryingTableBlockEntity.DRYING_TICKS;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockInventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (index < 9) {
                // Со стола → в инвентарь игрока
                if (!this.insertItem(slotStack, 9, 45, true)) return ItemStack.EMPTY;
            } else {
                // Из инвентаря → в слоты стола (0–8), по одному в каждый ПУСТОЙ слот
                boolean moved = false;
                for (int i = 0; i < 9 && !slotStack.isEmpty(); i++) {
                    Slot targetSlot = this.slots.get(i);
                    if (!targetSlot.hasStack() && targetSlot.canInsert(slotStack)) {
                        targetSlot.setStack(new ItemStack(slotStack.getItem(), 1));
                        slotStack.decrement(1);
                        targetSlot.markDirty();
                        moved = true;
                    }
                }
                // Нет пустых слотов — сообщаем фреймворку что ничего не перемещено,
                // чтобы while-цикл в ScreenHandler прекратился и остаток не пропал
                if (!moved) return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }
}
