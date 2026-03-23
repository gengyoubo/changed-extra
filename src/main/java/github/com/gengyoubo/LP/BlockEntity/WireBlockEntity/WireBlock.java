package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WireBlock extends Block implements EntityBlock {
    private final WireType type;

    public WireBlock(WireType type, Properties props) {
        super(props);
        this.type = type;
    }

    public WireType getWireType() {
        return type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }
}
