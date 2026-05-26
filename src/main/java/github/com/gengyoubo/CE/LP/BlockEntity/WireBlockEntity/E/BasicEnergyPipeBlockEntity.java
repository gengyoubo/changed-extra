package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.WireType;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasicEnergyPipeBlockEntity extends EnergyPipeBlockEntity {

    public BasicEnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(
                CELPBlockEntity.BASIC_WIRE_BLOCK_ENTITIES.get(),
                pos,
                state,
                WireType.BASIC.capacity,
                WireType.BASIC.maxTransfer
        );
    }
}
