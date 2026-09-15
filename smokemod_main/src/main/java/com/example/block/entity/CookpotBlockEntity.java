package com.example.block.entity;

import com.example.ExampleMod;
import com.example.block.CookpotBlock;
import com.example.block.CookpotFrameBlock;
import com.example.screen.CookpotScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A fictional game recipe. All quantities and timing are gameplay constants. */
public class CookpotBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int WATER_INPUT = 0, BUCKET_OUTPUT = 1, CALCITE = 2, COAL = 3, RAW = 4, OUTPUT = 5, SIZE = 6;
    public static final int COAL_PER_BATCH = 4;
    public static final int WATER_CAPACITY = 3;
    public static final int COOK_TICKS = 20 * 60;
    public static final int NO_HEAT = 0, NEED_WATER = 1, NEED_INPUT = 2, OUTPUT_FULL = 3, COOKING = 4;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int water, progress;
    private int status = NO_HEAT;

    public final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int size() { return 3; }
        @Override public int get(int index) {
            return switch (index) { case 0 -> water; case 1 -> progress; case 2 -> status; default -> 0; };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> water = MathHelper.clamp(value, 0, WATER_CAPACITY);
                case 1 -> progress = MathHelper.clamp(value, 0, COOK_TICKS - 1);
                case 2 -> status = MathHelper.clamp(value, NO_HEAT, COOKING);
            }
        }
    };

    public CookpotBlockEntity(BlockPos pos, BlockState state) { super(ExampleMod.COOKPOT_BLOCK_ENTITY, pos, state); }

    public static boolean hasHeatBelow(World world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return below.isOf(Blocks.FIRE) || (below.isOf(ExampleMod.COOKPOT_FRAME)
                && below.get(CookpotFrameBlock.LIT) && !below.get(CookpotFrameBlock.WATERLOGGED));
    }
    public static void tick(World world, BlockPos pos, BlockState state, CookpotBlockEntity pot) {
        if (world.isClient) return;
        pot.tickCooking(hasHeatBelow(world, pos));
        pot.updateVisualState();
    }
    public boolean addWaterBucket() {
        if (water >= WATER_CAPACITY) return false;
        water++;
        markDirty();
        updateVisualState();
        return true;
    }
    private void updateVisualState() {
        if (world == null || world.isClient) return;
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(ExampleMod.COOKPOT)) return;
        BlockState next = state.with(CookpotBlock.WATER_LEVEL, water).with(CookpotBlock.LIT, status == COOKING)
                .with(CookpotBlock.FINISHED, items.get(OUTPUT).isOf(ExampleMod.REFINED_OPIUM));
        if (state != next) world.setBlockState(pos, next, 3);
    }
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case WATER_INPUT -> stack.isOf(Items.WATER_BUCKET);
            case CALCITE -> stack.isOf(Items.CALCITE);
            case COAL -> stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL);
            case RAW -> stack.isOf(ExampleMod.RAW_OPIUM);
            default -> false;
        };
    }
    private boolean hasIngredients() {
        return accepts(CALCITE, items.get(CALCITE)) && accepts(COAL, items.get(COAL))
                && items.get(COAL).getCount() >= COAL_PER_BATCH && accepts(RAW, items.get(RAW));
    }
    private boolean canOutput() {
        ItemStack result = items.get(OUTPUT);
        return result.isEmpty() || (ItemStack.canCombine(result, new ItemStack(ExampleMod.REFINED_OPIUM))
                && result.getCount() < result.getMaxCount());
    }
    void tickCooking(boolean heated) {
        int oldWater = water, oldProgress = progress;
        ItemStack buckets = items.get(BUCKET_OUTPUT);
        if (water < WATER_CAPACITY && items.get(WATER_INPUT).isOf(Items.WATER_BUCKET)
                && (buckets.isEmpty() || (ItemStack.canCombine(buckets, new ItemStack(Items.BUCKET))
                && buckets.getCount() < buckets.getMaxCount()))) {
            items.get(WATER_INPUT).decrement(1);
            if (buckets.isEmpty()) items.set(BUCKET_OUTPUT, new ItemStack(Items.BUCKET));
            else buckets.increment(1);
            water++;
        }
        boolean ingredients = hasIngredients();
        if (!ingredients || water < WATER_CAPACITY) progress = 0;
        status = !heated ? NO_HEAT : water < WATER_CAPACITY ? NEED_WATER : !ingredients ? NEED_INPUT
                : !canOutput() ? OUTPUT_FULL : COOKING;
        if (status == COOKING && ++progress >= COOK_TICKS) {
            // Commit the entire batch atomically; a full output never consumes ingredients.
            items.get(CALCITE).decrement(1);
            items.get(COAL).decrement(COAL_PER_BATCH);
            items.get(RAW).decrement(1);
            if (items.get(OUTPUT).isEmpty()) items.set(OUTPUT, new ItemStack(ExampleMod.REFINED_OPIUM));
            else items.get(OUTPUT).increment(1);
            water -= WATER_CAPACITY;
            progress = 0;
            status = NEED_WATER;
        }
        if (oldWater != water || oldProgress != progress) markDirty();
    }

    @Override public int size() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public boolean isValid(int slot, ItemStack stack) { return accepts(slot, stack); }
    private void inputChanged(int slot) {
        if (slot >= CALCITE && slot <= RAW) progress = 0;
        markDirty();
    }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) inputChanged(slot);
        return result;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty()) inputChanged(slot);
        return result;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.setCount(Math.min(stack.getCount(), Math.min(getMaxCountPerStack(), stack.getMaxCount())));
        inputChanged(slot);
    }
    @Override public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    @Override public void clear() { items.clear(); progress = 0; markDirty(); }
    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putInt("WaterBuckets", water);
        nbt.putInt("CookProgress", progress);
    }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items.clear();
        Inventories.readNbt(nbt, items);
        water = MathHelper.clamp(nbt.getInt("WaterBuckets"), 0, WATER_CAPACITY);
        progress = water == WATER_CAPACITY && hasIngredients()
                ? MathHelper.clamp(nbt.getInt("CookProgress"), 0, COOK_TICKS - 1) : 0;
        status = NO_HEAT;
    }
    @Override public Text getDisplayName() { return Text.translatable("block.smokemod.cookpot"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CookpotScreenHandler(syncId, playerInventory, this, properties);
    }
}
