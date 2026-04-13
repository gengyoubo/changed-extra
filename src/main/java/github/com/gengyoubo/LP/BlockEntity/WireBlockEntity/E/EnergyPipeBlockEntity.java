package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.WireType;
import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyPipeBlockEntity extends BasePipeBlockEntity {


    public EnergyPipeBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, TransportType type) {
        super(beType, pos, state, type);
    }

    @Override
    protected void transfer() {
        // 你现在的 pushEnergy 逻辑
    }
}
