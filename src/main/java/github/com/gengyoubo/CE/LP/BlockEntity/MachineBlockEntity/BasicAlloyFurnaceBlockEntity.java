package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.recipe.AlloyFurnaceRecipe;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicAlloyFurnaceBlockEntity extends BaseEnergyBlockEntity {
    public static final int CAPACITY = 50_000;
    private int progress;
    private int costRemainder;
    private int processTicks;
    private int lpPerSecond;
    private ResourceLocation runningRecipe;
    private ItemStack firstInput = ItemStack.EMPTY;
    private ItemStack secondInput = ItemStack.EMPTY;
    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < 2 && isValidInput(level, stack); }
    };
    private final IItemHandler automationItems = new IItemHandler() {
        @Override public int getSlots() { return 3; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return items.isItemValid(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 2 ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private LazyOptional<IItemHandler> automationCapability = LazyOptional.of(() -> automationItems);

    public BasicAlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_ALLOY_FURNACE.get(), pos, state, CAPACITY);
    }

    public static boolean isValidInput(@Nullable Level level, ItemStack stack) {
        return level != null && !stack.isEmpty() && level.getRecipeManager()
                .getAllRecipesFor(CELPRecipes.ALLOY_FURNACE_TYPE).stream().anyMatch(recipe -> recipe.acceptsInput(stack));
    }

    public ItemStackHandler getItemHandler() { return items; }
    public int getProgress() { return progress; }
    public int getProcessTicks() { return processTicks; }
    public int getLpPerSecond() { return lpPerSecond; }

    private void resetProcess() {
        if (runningRecipe != null || progress != 0 || costRemainder != 0) setChanged();
        progress = 0;
        costRemainder = 0;
        processTicks = 0;
        lpPerSecond = 0;
        runningRecipe = null;
        firstInput = ItemStack.EMPTY;
        secondInput = ItemStack.EMPTY;
    }

    private boolean canFitOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(2);
        int limit = Math.min(items.getSlotLimit(2), result.getMaxStackSize());
        return !result.isEmpty() && (output.isEmpty() ? result.getCount() <= limit
                : ItemStack.isSameItemSameTags(output, result) && output.getCount() + result.getCount() <= limit);
    }

    @Override public void tick() {
        if (level == null || level.isClientSide) return;
        SimpleContainer input = new SimpleContainer(items.getStackInSlot(0), items.getStackInSlot(1));
        AlloyFurnaceRecipe recipe = level.getRecipeManager().getRecipeFor(CELPRecipes.ALLOY_FURNACE_TYPE, input, level).orElse(null);
        if (recipe == null) { resetProcess(); return; }
        if (!recipe.getId().equals(runningRecipe)
                || !ItemStack.isSameItemSameTags(firstInput, input.getItem(0))
                || !ItemStack.isSameItemSameTags(secondInput, input.getItem(1))) {
            resetProcess();
            runningRecipe = recipe.getId();
            firstInput = input.getItem(0).copyWithCount(1);
            secondInput = input.getItem(1).copyWithCount(1);
            setChanged();
        }
        processTicks = recipe.getProcessTicks();
        lpPerSecond = recipe.getLpPerSecond();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!canFitOutput(result)) return;

        // Carry fractional LP between ticks so nonmultiples of 20 retain their configured rate.
        long accumulated = (long) costRemainder + lpPerSecond;
        int cost = (int) (accumulated / 20);
        int remainder = (int) (accumulated % 20);
        if (progress + 1 >= processTicks && remainder > 0) { cost++; remainder = 0; }
        if (getEnergyStored() < cost) return;
        extractEnergy(cost, null);
        costRemainder = remainder;
        progress++;
        if (progress >= processTicks) {
            items.extractItem(0, 1, false);
            items.extractItem(1, 1, false);
            ItemStack output = items.getStackInSlot(2);
            items.setStackInSlot(2, output.isEmpty() ? result.copy() : output.copyWithCount(output.getCount() + result.getCount()));
            progress = 0;
            costRemainder = 0;
        }
        // Menu data synchronizes progress; block update packets are unnecessary each tick.
        setChanged();
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("CostRemainder", costRemainder);
        if (runningRecipe != null) tag.putString("RunningRecipe", runningRecipe.toString());
        tag.put("FirstInput", firstInput.save(new CompoundTag()));
        tag.put("SecondInput", secondInput.save(new CompoundTag()));
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        progress = Math.max(0, tag.getInt("Progress"));
        costRemainder = Math.floorMod(tag.getInt("CostRemainder"), 20);
        runningRecipe = ResourceLocation.tryParse(tag.getString("RunningRecipe"));
        firstInput = ItemStack.of(tag.getCompound("FirstInput"));
        secondInput = ItemStack.of(tag.getCompound("SecondInput"));
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); automationCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items);
        automationCapability = LazyOptional.of(() -> automationItems);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return (side == null ? itemCapability : automationCapability).cast();
        return super.getCapability(capability, side);
    }
}
