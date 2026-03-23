package github.com.gengyoubo.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class MachineBlockEntity extends BaseEnergyBlockEntity {

    private int progress = 0;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, capacity);
    }

    protected abstract int getEnergyCost();

    protected abstract int getMaxProgress();

    protected abstract void onFinish();

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (energy.getEnergyStored() >= getEnergyCost()) {
            energy.extractEnergy(getEnergyCost(), null);
            progress++;

            if (progress >= getMaxProgress()) {
                progress = 0;
                onFinish();
            }
        }

        pushEnergy();
    }
}
