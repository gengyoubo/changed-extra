package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergySync;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyStorage;
import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class InfuserPowerBlockEntity extends BlockEntity implements WorkbenchEnergyHolder, ILatexEnergyHandler {
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);
    private final WorkbenchEnergyStorage energy = new WorkbenchEnergyStorage(WorkbenchEnergyRules.NORMAL_CAPACITY, this::markEnergyChanged);
    private final LazyOptional<WorkbenchEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    public InfuserPowerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.INFUSER_POWER.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        energy.setEnergyStored(tag.getInt(WorkbenchEnergyRules.NBT_KEY));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt(WorkbenchEnergyRules.NBT_KEY, energy.getEnergyStored());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public WorkbenchEnergyStorage changede$getWorkbenchEnergy() {
        return energy;
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        return energy.receiveEnergy(Math.max(0, amount), false);
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public ItemStack getStackInSlot(int slot) {
        return items.getStackInSlot(slot);
    }

    private void markEnergyChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            WorkbenchEnergySync.sync(this, energy);
        }
    }
}
