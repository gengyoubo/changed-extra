package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.F;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

public abstract class FluidPipeBlockEntity extends BasePipeBlockEntity {

    private final FluidStack fluid = FluidStack.EMPTY;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_BLOCK_ENTITIES.get(), pos, state, TransportType.FLUID);
    }

    @Override
    protected boolean canConnectToPipe(BasePipeBlockEntity other, Direction direction) {
        return other.getTransportType() == TransportType.FLUID;
    }

    @Override
    protected boolean canConnectToMachine(BlockEntity target, Direction direction) {
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).isPresent();
    }

    @Override
    protected void transfer() {
        // TODO: fluid transfer logic
    }
}
