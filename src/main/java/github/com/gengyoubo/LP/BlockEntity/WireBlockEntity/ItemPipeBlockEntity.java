package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import github.com.gengyoubo.LP.CELPRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPipeBlockEntity extends BasePipeBlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPRegister.BASIC_WIRE_ENTITIES.get(), pos, state, TransportType.ITEM);
    }

    @Override
    protected void transfer() {
        // 传 ItemStack
    }
}
