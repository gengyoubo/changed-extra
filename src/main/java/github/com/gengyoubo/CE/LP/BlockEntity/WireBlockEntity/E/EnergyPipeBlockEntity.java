package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

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

        List<Direction> machines = new ArrayList<>();
        List<Direction> pipes = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (!canConnect(dir) || !(neighbor instanceof ILatexEnergyHandler) || neighbor instanceof GeneratorBlockEntity) {
                continue;
            }

            if (neighbor instanceof EnergyPipeBlockEntity) {
                pipes.add(dir);
            } else {
                machines.add(dir);
            }
        }

        for (Direction dir : machines) {
            transferTo(dir);
        }
        for (Direction dir : pipes) {
            transferTo(dir);
        }
    }

    private void transferTo(Direction dir) {
        if (level == null) {
            return;
        }

        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
        if (neighbor instanceof ILatexEnergyHandler handler) {
            int extracted = extractEnergy(maxTransfer, dir);
            if (extracted <= 0) {
                return;
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
