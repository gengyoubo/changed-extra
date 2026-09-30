package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ElectricFurnaceBlockEntity extends MachineBlockEntity {
    private static final int CAPACITY = 10_000;
    private static final int ENERGY_COST = 5;
    private static final int MAX_PROGRESS = 100;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler automationItemHandler = new IItemHandler() {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && itemHandler.isItemValid(slot, stack);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);
    private LazyOptional<IItemHandler> automationItemHandlerCap = LazyOptional.of(() -> automationItemHandler);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.ELECTRIC_FURNACE_BLOCK_ENTITY.get(), pos, state, CAPACITY);
    }

    @Override
    protected int getEnergyCost() {
        return ENERGY_COST;
    }

    @Override
    protected int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        automationItemHandlerCap.invalidate();
        itemHandlerCap = LazyOptional.of(() -> itemHandler);
        automationItemHandlerCap = LazyOptional.of(() -> automationItemHandler);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return (side == null ? itemHandlerCap : automationItemHandlerCap).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected boolean canProcess() {
        if (level == null) return false;

        Optional<SmeltingRecipe> recipe = findSmeltingRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameTags(output, result)) {
            return false;
        }

        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void processItem() {
        if (level == null) return;

        Optional<SmeltingRecipe> recipe = findSmeltingRecipe();
        if (recipe.isEmpty()) {
            return;
        }

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();

        itemHandler.extractItem(0, 1, false);

        ItemStack output = itemHandler.getStackInSlot(1);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(1, result);
        } else {
            output.grow(result.getCount());
        }

        setChanged();
    }

    private Optional<SmeltingRecipe> findSmeltingRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, createRecipeInventory(), level);
    }

    private SimpleContainer createRecipeInventory() {
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, itemHandler.getStackInSlot(0));
        return inventory;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
    }
}
