package github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.RedstoneMode;
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
    protected abstract int generate();

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
        if (!shouldWork()) return;

        tryGenerate();
        pushEnergy();
    }

    protected void tryGenerate() {
        if (getEnergyStored() >= getMaxEnergyStored()) {
            return;
        }

        ItemStack fuelStack = itemHandler.getStackInSlot(0);
        if (fuelStack.isEmpty()) {
            return;
        }

        itemHandler.extractItem(0, 1, false);
        receiveEnergy(generate(), null);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putString("RedstoneMode", redstoneMode.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("RedstoneMode")) {
            redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode"));
        }
    }
}
