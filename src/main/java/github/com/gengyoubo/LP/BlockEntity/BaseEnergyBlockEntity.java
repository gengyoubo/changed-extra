package github.com.gengyoubo.LP.BlockEntity;

import github.com.gengyoubo.LP.ILatexEnergyHandler;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BaseEnergyBlockEntity extends BlockEntity implements ILatexEnergyHandler {
    protected final LatexEnergyStorage energy;

    public BaseEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.energy = new LatexEnergyStorage(capacity);
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        int received = energy.receiveEnergy(amount, from);
        if (received > 0) {
            setChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        int extracted = energy.extractEnergy(amount, from);
        if (extracted > 0) {
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

    protected void pushEnergy() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));

            if (neighbor instanceof ILatexEnergyHandler handler) {
                int extracted = this.extractEnergy(100, dir);
                int received = handler.receiveEnergy(extracted, dir.getOpposite());

                if (received < extracted) {
                    this.receiveEnergy(extracted - received, dir);
                }
            }
        }
    }

    public void tick() {
        pushEnergy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.receiveEnergy(tag.getInt("Energy"), null);
    }
}
