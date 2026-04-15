package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.I;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemPipeBlockEntity extends BasePipeBlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_BLOCK_ENTITIES.get(), pos, state, TransportType.ITEM);
    }

    @Override
    protected void transfer() {
        // 传 ItemStack
    }
}
