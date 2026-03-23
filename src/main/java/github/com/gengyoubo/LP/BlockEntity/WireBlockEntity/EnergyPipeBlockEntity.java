package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyPipeBlockEntity extends BasePipeBlockEntity{
    private final WireType wireType;
    private final LatexEnergyStorage energy;

    public EnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_ENTITIES.get(), pos, state, TransportType.ENERGY);

        this.wireType = ((WireBlock)state.getBlock()).getWireType();
        this.energy = new LatexEnergyStorage(wireType.capacity);
    }

    @Override
    protected void transfer() {
        // 你现在的 pushEnergy 逻辑
    }
}
