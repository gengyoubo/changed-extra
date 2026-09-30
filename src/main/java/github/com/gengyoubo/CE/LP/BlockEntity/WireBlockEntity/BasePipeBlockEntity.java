package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

public abstract class BasePipeBlockEntity extends BlockEntity {
    protected final TransportType type;
    private final Map<Direction, PipeConnectionMode> connectionModes = new EnumMap<>(Direction.class);
    private static final String CONNECTION_MODE_PREFIX = "PipeConnectionMode_";

    public BasePipeBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, TransportType type) {
        super(beType, pos, state);
        this.type = type;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (type == TransportType.ENERGY || PipeNetworkManager.isLeader(this)) transfer();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        PipeNetworkManager.invalidate(level);
    }

    @Override
    public void setRemoved() {
        PipeNetworkManager.invalidate(level);
        super.setRemoved();
    }

    public TransportType getTransportType() {
        return type;
    }

    public PipeConnectionMode getConnectionMode(Direction direction) {
        return connectionModes.getOrDefault(direction, PipeConnectionMode.BIDIRECTIONAL);
    }

    public PipeConnectionMode cycleConnectionMode(Direction direction) {
        PipeConnectionMode next = getConnectionMode(direction).next();
        connectionModes.put(direction, next);
        setChanged();
        return next;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (Direction direction : Direction.values()) {
            PipeConnectionMode mode = getConnectionMode(direction);
            if (mode != PipeConnectionMode.BIDIRECTIONAL) {
                tag.putString(CONNECTION_MODE_PREFIX + direction.getName(), mode.name());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        connectionModes.clear();
        for (Direction direction : Direction.values()) {
            String key = CONNECTION_MODE_PREFIX + direction.getName();
            if (tag.contains(key)) {
                try {
                    PipeConnectionMode mode = PipeConnectionMode.valueOf(tag.getString(key));
                    if (mode != PipeConnectionMode.BIDIRECTIONAL) connectionModes.put(direction, mode);
                } catch (IllegalArgumentException ignored) {
                    // Keep the default bidirectional mode for invalid saved values.
                }
            }
        }
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
