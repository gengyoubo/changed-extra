package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyPipeBlockEntity extends BasePipeBlockEntity{

    public EnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_ENTITIES.get(), pos, state, TransportType.ENERGY);

        WireType wireType = ((WireBlock) state.getBlock()).getWireType();
        LatexEnergyStorage energy = new LatexEnergyStorage(wireType.capacity);
    }

    @Override
    protected void transfer() {
        // 你现在的 pushEnergy 逻辑
    }
}
