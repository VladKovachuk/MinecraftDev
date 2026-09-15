package com.example.block.entity;

import com.example.ExampleMod;
import com.example.block.JarBlock;
import com.example.screen.JarScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class JarBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int CURING_TIME = 1 * 60; // Real seconds, fits a screen property packet. 140 * 60
    private long curingStartedAt = -1;
    private ItemStack trackedStack = ItemStack.EMPTY;
    private final PropertyDelegate properties = new PropertyDelegate() {
        public int size() { return 1; }
        public int get(int index) { return (int) (elapsedMillis(clockNow()) / 1000); }
        public void set(int index, int value) { }
    };
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private final int capacity;

    public JarBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.JAR_BLOCK_ENTITY, pos, state);
        capacity = ((JarBlock) state.getBlock()).getCapacity();
    }

    public static boolean isBud(ItemStack stack) {
        return stack.isOf(ExampleMod.CANNABIS_BUD) || stack.isOf(ExampleMod.DRIED_CANNABIS_BUD)
                || stack.isOf(ExampleMod.CURED_CANNABIS_BUD);
    }

    public PropertyDelegate getProperties() { return properties; }

    public static void tick(World world, BlockPos pos, BlockState state, JarBlockEntity jar) {
        if (world.isClient || !jar.getStack(0).isOf(ExampleMod.DRIED_CANNABIS_BUD)) return;
        jar.detectBatchChange();
        if (jar.curingStartedAt < 0) {
            jar.curingStartedAt = jar.clockNow();
            jar.markDirty();
        }
        jar.finishCuring(jar.clockNow());
    }

    private long clockNow() {
        return world != null && !world.isClient ? JarCuringClock.now(world.getServer()) : 0;
    }

    private long elapsedMillis(long now) {
        return curingStartedAt < 0 ? 0 : Math.max(0, Math.min(CURING_TIME * 1000L, now - curingStartedAt));
    }

    void finishCuring(long now) {
        if (items.get(0).isOf(ExampleMod.DRIED_CANNABIS_BUD) && elapsedMillis(now) >= CURING_TIME * 1000L) {
            setStack(0, new ItemStack(ExampleMod.CURED_CANNABIS_BUD, items.get(0).getCount()));
        }
    }

    private void detectBatchChange() {
        ItemStack stack = items.get(0);
        if (stack.getCount() != trackedStack.getCount() || !ItemStack.canCombine(stack, trackedStack)) {
            curingStartedAt = stack.isOf(ExampleMod.DRIED_CANNABIS_BUD) ? clockNow() : -1;
            trackedStack = stack.copy();
            super.markDirty();
        }
    }

    @Override
    public int size() { return 1; }

    @Override
    public int getMaxCountPerStack() { return capacity; }

    @Override
    public boolean isEmpty() { return items.get(0).isEmpty(); }

    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public boolean isValid(int slot, ItemStack stack) { return slot == 0 && isBud(stack); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(items, slot, amount);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(items, slot);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !isValid(slot, stack)) return;
        if (stack != items.get(slot)) {
            curingStartedAt = stack.isOf(ExampleMod.DRIED_CANNABIS_BUD) ? clockNow() : -1;
        }
        items.set(slot, stack);
        if (stack.getCount() > capacity) stack.setCount(capacity);
        markDirty();
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public void markDirty() {
        detectBatchChange();
        super.markDirty();
        if (world != null && !world.isClient) {
            BlockState state = world.getBlockState(pos);
            // Do not restore the jar while its contents are scattered during breaking.
            if (state.isOf(getCachedState().getBlock())) {
                boolean filled = !isEmpty();
                if (state.get(JarBlock.FILLED) != filled) {
                    world.setBlockState(pos, state.with(JarBlock.FILLED, filled), Block.NOTIFY_ALL);
                }
                world.updateComparators(pos, state.getBlock());
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("CuringStartedAtRuntime", curingStartedAt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items.clear();
        Inventories.readNbt(nbt, items);
        ItemStack stack = items.get(0);
        if (!stack.isEmpty() && !isBud(stack)) items.set(0, ItemStack.EMPTY);
        else if (stack.getCount() > capacity) stack.setCount(capacity);
        trackedStack = items.get(0).copy();
        long savedStart = nbt.contains("CuringStartedAtRuntime") ? nbt.getLong("CuringStartedAtRuntime") : -1;
        curingStartedAt = trackedStack.isOf(ExampleMod.DRIED_CANNABIS_BUD)
                ? Math.max(-1, savedStart) : -1;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new JarScreenHandler(syncId, inventory, this, capacity);
    }
}
