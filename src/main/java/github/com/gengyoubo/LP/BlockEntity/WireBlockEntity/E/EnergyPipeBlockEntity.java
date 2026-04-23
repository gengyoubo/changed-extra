package github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.LP.ILatexEnergyHandler;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyPipeBlockEntity extends BasePipeBlockEntity implements ILatexEnergyHandler {
    protected final LatexEnergyStorage energy;
    protected final int maxTransfer;

    public EnergyPipeBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
        super(beType, pos, state, TransportType.ENERGY);
        this.energy = new LatexEnergyStorage(capacity);
        this.maxTransfer = maxTransfer;
    }

    @Override
    protected boolean canConnectToPipe(BasePipeBlockEntity other, Direction direction) {
        return other.getTransportType() == TransportType.ENERGY;
    }

    @Override
    protected boolean canConnectToMachine(BlockEntity target, Direction direction) {
        return target instanceof ILatexEnergyHandler;
    }

    @Override
    protected void transfer() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!canConnect(dir)) {
                continue;
            }

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (!(neighbor instanceof ILatexEnergyHandler handler)) {
                continue;
            }

            int extracted = extractEnergy(maxTransfer, dir);
            if (extracted <= 0) {
                continue;
            }

            int received = handler.receiveEnergy(extracted, dir.getOpposite());
            if (received < extracted) {
                receiveEnergy(extracted - received, dir);
            }
        }
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        return energy.receiveEnergy(amount, from);
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        return energy.extractEnergy(amount, from);
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }
}
