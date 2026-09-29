package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E;

import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.GeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.TransportType;
import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class EnergyPipeBlockEntity extends BasePipeBlockEntity implements ILatexEnergyHandler {
    private static final String ENERGY_TAG = "PipeEnergy";
    private static final String INPUT_DIRECTION_TAG = "PipeInputDirection";
    protected final LatexEnergyStorage energy;
    protected final int maxTransfer;
    private Direction lastInputDirection;

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
            if (!canConnect(dir) || neighbor instanceof GeneratorBlockEntity) {
                continue;
            }

            if (neighbor instanceof EnergyPipeBlockEntity) {
                if (dir == lastInputDirection) {
                    continue;
                }
                pipes.add(dir);
            } else if (neighbor instanceof ILatexEnergyHandler) {
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

            int received = Math.max(0, Math.min(extracted, handler.receiveEnergy(extracted, dir.getOpposite())));
            if (received < extracted) {
                restoreEnergy(extracted - received);
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY_TAG, energy.getEnergyStored());
        if (lastInputDirection != null) {
            tag.putString(INPUT_DIRECTION_TAG, lastInputDirection.getName());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy.receiveEnergy(tag.getInt(ENERGY_TAG), null);
        if (tag.contains(INPUT_DIRECTION_TAG)) {
            lastInputDirection = Direction.byName(tag.getString(INPUT_DIRECTION_TAG));
        }
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        int received = energy.receiveEnergy(amount, from);
        if (received > 0) {
            lastInputDirection = from;
            setChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        int extracted = energy.extractEnergy(amount, from);
        if (extracted > 0) {
            if (energy.getEnergyStored() == 0) {
                lastInputDirection = null;
            }
            setChanged();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    private void restoreEnergy(int amount) {
        if (energy.receiveEnergy(amount, null) > 0) {
            setChanged();
        }
    }
}
