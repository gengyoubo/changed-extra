package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public class FluidPipeBlockEntity extends BasePipeBlockEntity {

    private final FluidStack fluid = FluidStack.EMPTY;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_ENTITIES.get(), pos, state, TransportType.FLUID);
    }

    @Override
    protected void transfer() {
        // 向邻居传流体
    }
}
