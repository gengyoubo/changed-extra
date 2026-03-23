package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public class FluidPipeBlockEntity extends BasePipeBlockEntity {

    private FluidStack fluid = FluidStack.EMPTY;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state, TransportType.FLUID);
    }

    @Override
    protected void transfer() {
        // 向邻居传流体
    }
}
