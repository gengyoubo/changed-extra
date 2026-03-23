package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPipeBlockEntity extends BasePipeBlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state, TransportType.ITEM);
    }

    @Override
    protected void transfer() {
        // 传 ItemStack
    }
}
