package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.ltxprogrammer.changed.block.entity.PurifierBlockEntity;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicLatexPurifierBlockEntity extends MachineBlockEntity {
    private static final int ENERGY_CAPACITY = 30_000;
    private static final int ENERGY_COST = 30;
    private static final int ITEM_PROCESS_TICKS = 100;
    private static final int FLUID_PROCESS_TICKS = 100;
    private static final int TANK_CAPACITY = 10_000;
    private static final int FLUID_PER_SAMPLE = 250;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isValidInput(level, stack);
        }
    };
    private final IItemHandler automationItems = new IItemHandler() {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) { return isLatex(stack); }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final IFluidHandler inputTank = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tankIndex) { return tank.getFluidInTank(tankIndex); }
        @Override public int getTankCapacity(int tankIndex) { return tank.getTankCapacity(tankIndex); }
        @Override public boolean isFluidValid(int tankIndex, FluidStack stack) { return tank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return tank.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private LazyOptional<IItemHandler> automationItemCap = LazyOptional.of(() -> automationItems);
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> inputTank);
    private int itemProgress;
    private int fluidProgress;

    public BasicLatexPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_LATEX_PURIFIER.get(), pos, state, ENERGY_CAPACITY);
    }

    @Override protected int getEnergyCost() { return ENERGY_COST; }
    @Override protected int getMaxProgress() { return ITEM_PROCESS_TICKS; }
    @Override protected boolean canProcess() { return canProcessItem(); }
    @Override protected void processItem() { completeItemProcess(); }

    public ItemStackHandler getItemHandler() { return items; }
    public FluidStack getStoredFluid() { return tank.getFluid().copy(); }
    public int getItemProgress() { return itemProgress; }
    public int getFluidProgress() { return fluidProgress; }
    public int getTankCapacity() { return TANK_CAPACITY; }

    public static boolean isValidInput(@Nullable Level level, ItemStack stack) {
        return level != null && !stack.isEmpty()
                && PurifierBlockEntity.isConversionRecipe(level.getRecipeManager(), stack);
    }

    private ItemStack getItemResult() {
        if (!isValidInput(level, items.getStackInSlot(0))) return ItemStack.EMPTY;
        return level.getRecipeManager().getAllRecipesFor(ChangedRecipeTypes.PURIFIER_RECIPE.get()).stream()
                .filter(recipe -> recipe.getIngredient().test(items.getStackInSlot(0)))
                .findFirst().map(recipe -> recipe.getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    private boolean canProcessItem() {
        return canFitOutput(getItemResult());
    }

    private void completeItemProcess() {
        if (level == null || !canProcessItem()) return;
        ItemStack result = getItemResult();
        items.extractItem(0, 1, false);
        insertOutput(result);
    }

    private boolean canProcessFluid() {
        return tank.getFluidAmount() >= FLUID_PER_SAMPLE && canFitOutput(new ItemStack(ChangedItems.LATEX_BASE.get()));
    }

    private void completeFluidProcess() {
        if (!canProcessFluid()) return;
        tank.drain(FLUID_PER_SAMPLE, IFluidHandler.FluidAction.EXECUTE);
        insertOutput(new ItemStack(ChangedItems.LATEX_BASE.get()));
    }

    private boolean canFitOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack output = items.getStackInSlot(1);
        return output.isEmpty() || ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(1);
        if (output.isEmpty()) items.setStackInSlot(1, result.copy());
        else output.grow(result.getCount());
        setChanged();
    }

    @Override public void tick() {
        if (level == null || level.isClientSide) return;
        boolean dirty = false;

        // Fluid production gets first access to the shared output slot. If it cannot fit,
        // it pauses without draining fluid; item processing can still use any room left.
        if (canProcessFluid()) {
            if (getEnergyStored() >= ENERGY_COST) {
                extractEnergy(ENERGY_COST, null);
                if (++fluidProgress >= FLUID_PROCESS_TICKS) {
                    fluidProgress = 0;
                    completeFluidProcess();
                }
                dirty = true;
            } else if (fluidProgress != 0) { fluidProgress = 0; dirty = true; }
        } else if (fluidProgress != 0) { fluidProgress = 0; dirty = true; }

        if (canProcessItem()) {
            if (getEnergyStored() >= ENERGY_COST) {
                extractEnergy(ENERGY_COST, null);
                if (++itemProgress >= ITEM_PROCESS_TICKS) {
                    itemProgress = 0;
                    completeItemProcess();
                }
                dirty = true;
            } else if (itemProgress != 0) { itemProgress = 0; dirty = true; }
        } else if (itemProgress != 0) { itemProgress = 0; dirty = true; }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static boolean isLatex(FluidStack stack) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && id.getNamespace().equals("changed")
                && (id.getPath().equals("dark_latex") || id.getPath().equals("white_latex"));
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("PurifierTank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("ItemProgress", itemProgress);
        tag.putInt("FluidProgress", fluidProgress);
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        tank.readFromNBT(tag.getCompound("PurifierTank"));
        itemProgress = tag.getInt("ItemProgress");
        fluidProgress = tag.getInt("FluidProgress");
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate(); automationItemCap.invalidate(); fluidCap.invalidate();
        itemCap = LazyOptional.of(() -> items);
        automationItemCap = LazyOptional.of(() -> automationItems);
        fluidCap = LazyOptional.of(() -> inputTank);
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return (side == null ? itemCap : automationItemCap).cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }
}
