package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;

public class BasicItemPipeBlockEntity extends ItemPipeBlockEntity {

    public BasicItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.BASIC_ITEM_PIPE_BLOCK_ENTITY.get(), pos, state);
    }
}
