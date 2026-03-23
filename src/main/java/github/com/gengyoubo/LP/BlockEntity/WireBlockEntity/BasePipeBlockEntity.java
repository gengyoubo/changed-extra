package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BasePipeBlockEntity extends BlockEntity {

    protected final TransportType type;

    public BasePipeBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, TransportType type) {
        super(beType, pos, state);
        this.type = type;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        transfer();
    }

    protected abstract void transfer();
}
