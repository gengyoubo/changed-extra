package github.com.gengyoubo.LP.BlockEntity.GeneratorBlockEntity;

import github.com.gengyoubo.LP.BlockEntity.BaseEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GeneratorBlockEntity extends BaseEnergyBlockEntity {

    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, capacity);
    }

    protected abstract int generate();

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        // 发电
        energy.receiveEnergy(generate(), null);

        pushEnergy();
    }
}
