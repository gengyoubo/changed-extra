package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasicEnergyPipeBlockEntity extends EnergyPipeBlockEntity {

    public BasicEnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_ENTITIES.get(), pos, state, TransportType.ENERGY);
    }
}
