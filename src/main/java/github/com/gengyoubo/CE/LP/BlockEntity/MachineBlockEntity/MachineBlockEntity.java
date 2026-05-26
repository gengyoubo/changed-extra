package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity;
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

    protected int getEnergyCostForNextTick() {
        return getEnergyCost();
    }

    protected boolean hasEnoughEnergyForNextTick() {
        return getEnergyStored() >= getEnergyCostForNextTick();
    }

    protected void consumeEnergyForNextTick() {
        int cost = getEnergyCostForNextTick();
        if (cost > 0) {
            extractEnergy(cost, null);
        }
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgressValue() {
        return getMaxProgress();
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        boolean dirty = false;
        if (canProcess() && hasEnoughEnergyForNextTick()) {
            consumeEnergyForNextTick();
            progress++;
            dirty = true;

            if (progress >= getMaxProgress()) {
                progress = 0;
                processItem();
            }

        } else if (progress != 0) {
            progress = 0;
            dirty = true;
        }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}

