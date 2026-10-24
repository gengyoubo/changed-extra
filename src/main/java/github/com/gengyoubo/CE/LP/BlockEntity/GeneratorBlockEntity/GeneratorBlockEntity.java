package github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GeneratorBlockEntity extends BaseEnergyBlockEntity {
    /** Fuel is consumed in one go, so the fire stays visible for a while after each burst. */
    private static final int LIT_LINGER_TICKS = 40;
    protected final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS_ON;
    private LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, capacity);
    }
    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void cycleRedstoneMode() {
        setRedstoneMode(switch (redstoneMode) {
            case ALWAYS_ON -> RedstoneMode.ALWAYS_OFF;
            case ALWAYS_OFF -> RedstoneMode.ON_WITH_REDSTONE;
            case ON_WITH_REDSTONE -> RedstoneMode.OFF_WITH_REDSTONE;
            case OFF_WITH_REDSTONE -> RedstoneMode.ALWAYS_ON;
        });
    }
    protected boolean shouldWork() {
        if (level == null) {
            return false;
        }

        boolean hasSignal = level.hasNeighborSignal(worldPosition);

        return switch (redstoneMode) {
            case ALWAYS_ON -> true;
            case ALWAYS_OFF -> false;
            case ON_WITH_REDSTONE -> hasSignal;
            case OFF_WITH_REDSTONE -> !hasSignal;
        };
    }
    protected abstract int generate(ItemStack fuel);

    public boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && generate(stack) > 0;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        itemHandlerCap = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;
        boolean generated = false;
        if (shouldWork()) {
            generated = tryGenerate();
            pushEnergy();
        }
        updateLit(generated, LIT_LINGER_TICKS);
    }

    /** Burns one unit of fuel if the buffer has room; returns whether energy was produced. */
    protected boolean tryGenerate() {
        ItemStack fuelStack = itemHandler.getStackInSlot(0);
        if (fuelStack.isEmpty()) {
            return false;
        }

        int generated = generate(fuelStack);
        if (generated <= 0) {
            return false;
        }

        if (getMaxEnergyStored() - getEnergyStored() < generated) {
            return false;
        }

        itemHandler.extractItem(0, 1, false);
        receiveEnergy(generated, null);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putString("RedstoneMode", redstoneMode.name());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("RedstoneMode")) {
            redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode"));
        }
    }
}
