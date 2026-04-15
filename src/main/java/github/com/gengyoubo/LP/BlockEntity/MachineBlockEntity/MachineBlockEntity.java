package github.com.gengyoubo.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class MachineBlockEntity extends BaseEnergyBlockEntity {

    protected int progress = 0;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, capacity);
    }

    protected abstract int getEnergyCost();

    protected abstract int getMaxProgress();

    protected abstract boolean canProcess();

    protected abstract void processItem();

    public int getProgress() {
        return progress;
    }

    public int getMaxProgressValue() {
        return getMaxProgress();
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (canProcess() && getEnergyStored() >= getEnergyCost()) {
            extractEnergy(getEnergyCost(), null);
            progress++;

            if (progress >= getMaxProgress()) {
                progress = 0;
                processItem();
            }

            setChanged();
        } else {
            progress = 0;
        }
    }
}

