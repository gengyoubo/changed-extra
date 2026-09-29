package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.F;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;

public class BasicFluidPipeBlockEntity extends FluidPipeBlockEntity {

    public BasicFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_FLUID_PIPE_BLOCK_ENTITY.get(), pos, state);
    }
}
