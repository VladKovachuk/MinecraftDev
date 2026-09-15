package com.example.block.entity;

import com.example.ExampleMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import com.example.screen.DryingTableScreenHandler;

import java.util.Arrays;

public class DryingTableBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {

    public static final int SLOT_RESULT = 0;
    public static final int INPUT_SLOTS = 9;
    public static final int SIZE = 1 + INPUT_SLOTS;

    /** One minute of loaded server ticks for each item. */
    public static final int DRYING_TICKS = 20 * 60;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    private final int[] dryingTicks = new int[SIZE];

    public final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return index >= 0 && index < INPUT_SLOTS ? dryingTicks[index + 1] : 0;
        }
        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < INPUT_SLOTS)
                dryingTicks[index + 1] = Math.max(0, Math.min(DRYING_TICKS, value));
        }
        @Override
        public int size() { return INPUT_SLOTS; }
    };

    public DryingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.DRYING_TABLE_BLOCK_ENTITY, pos, state);
    }

    // ---- Tick ----

    public static void tick(World world, BlockPos pos, BlockState state, DryingTableBlockEntity be) {
        if (world.isClient) return;

        be.tickDrying();
    }

    @Nullable
    private static Item getDryingResult(ItemStack stack) {
        if (stack.isOf(ExampleMod.GREEN_TOBACCO_LEAF)) return ExampleMod.DRIED_TOBACCO_LEAF;
        if (stack.isOf(ExampleMod.CANNABIS_BUD)) return ExampleMod.DRIED_CANNABIS_BUD;
        if (stack.isOf(ExampleMod.GREEN_CANNABIS_LEAF)) return ExampleMod.DRIED_CANNABIS_LEAF;
        return null;
    }

    public static boolean canDry(ItemStack stack) {
        return getDryingResult(stack) != null;
    }

    void tickDrying() {
        boolean changed = false;
        boolean finished = false;
        for (int slot = 1; slot < SIZE; slot++) {
            Item result = getDryingResult(items.get(slot));
            if (result != null) {
                changed = true;
                if (++dryingTicks[slot] >= DRYING_TICKS) {
                    // Each finished item stays at its own position on the table.
                    items.set(slot, new ItemStack(result, items.get(slot).getCount()));
                    dryingTicks[slot] = 0;
                    finished = true;
                }
            } else if (dryingTicks[slot] != 0) {
                dryingTicks[slot] = 0;
                changed = true;
            }
        }
        if (finished) markDirtyAndSync();
        else if (changed) markDirty();
    }

    // ---- Inventory ----

    @Override
    public int size() { return SIZE; }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            dryingTicks[slot] = 0;
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        dryingTicks[slot] = 0;
        markDirtyAndSync();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        dryingTicks[slot] = 0;
        if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
        markDirtyAndSync();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        items.clear();
        Arrays.fill(dryingTicks, 0);
        markDirtyAndSync();
    }

    // ---- NBT ----

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putIntArray("dryingTicks", dryingTicks);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items.clear();
        Inventories.readNbt(nbt, items);
        Arrays.fill(dryingTicks, 0);
        int[] savedTicks = nbt.getIntArray("dryingTicks");
        for (int slot = 1; slot < Math.min(SIZE, savedTicks.length); slot++) {
            if (canDry(items.get(slot)))
                dryingTicks[slot] = Math.max(0, Math.min(DRYING_TICKS - 1, savedTicks[slot]));
        }
        // Legacy dryingProgress cannot tell when individual items were added.
        // Old saves keep their items, but start this first per-slot cycle at zero.
    }

    // ---- Client sync ----

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt);
        return nbt;
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    // ---- NamedScreenHandlerFactory ----

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.smokemod.drying_table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DryingTableScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
