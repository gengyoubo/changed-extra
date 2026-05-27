package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.networking.IManagedGridNode;
import appeng.capabilities.Capabilities;
import appeng.me.helpers.IGridConnectedBlockEntity;
import github.com.gengyoubo.CE.LP.compat.Ae2SpaceTowerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class CreateAe2SpaceTowerBlockEntity extends CreateSpaceTowerBlockEntity implements IGridConnectedBlockEntity {
    private final Ae2SpaceTowerBridge ae2Bridge = new Ae2SpaceTowerBridge(this, this);
    private final LazyOptional<IGridConnectedBlockEntity> ae2Host = LazyOptional.of(() -> this);

    public CreateAe2SpaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ae2Bridge.onLoad(level, worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        ae2Bridge.tick();
    }

    @Override
    public void remove() {
        ae2Bridge.destroy();
        super.remove();
    }

    @Override
    public void onChunkUnloaded() {
        ae2Bridge.destroy();
        super.onChunkUnloaded();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        ae2Bridge.save(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        ae2Bridge.load(tag);
    }

    @Override
    public IGridNode getGridNode(Direction side) {
        return ae2Bridge.getGridNode(side);
    }

    @Override
    public IManagedGridNode getMainNode() {
        return ae2Bridge.getMainNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction side) {
        return ae2Bridge.getCableConnectionType(side);
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return ae2Host.cast();
        }
        return super.getCapability(cap, side);
    }
}
