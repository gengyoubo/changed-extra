package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    public TransportType getTransportType() {
        return type;
    }

    public boolean canConnect(Direction direction) {
        if (level == null) {
            return false;
        }

        BlockPos targetPos = worldPosition.relative(direction);
        BlockEntity targetEntity = level.getBlockEntity(targetPos);

        if (targetEntity == null) {
            return false;
        }

        if (targetEntity instanceof BasePipeBlockEntity pipeBlockEntity) {
            return canConnectToPipe(pipeBlockEntity, direction);
        }

        return canConnectToMachine(targetEntity, direction);
    }

    protected abstract boolean canConnectToPipe(BasePipeBlockEntity other, Direction direction);

    protected abstract boolean canConnectToMachine(BlockEntity target, Direction direction);

    protected abstract void transfer();
}
