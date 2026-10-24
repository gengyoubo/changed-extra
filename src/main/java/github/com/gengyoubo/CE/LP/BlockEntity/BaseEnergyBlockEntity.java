package github.com.gengyoubo.CE.LP.BlockEntity;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

public class BaseEnergyBlockEntity extends BlockEntity implements ILatexEnergyHandler {
    protected final LatexEnergyStorage energy;
    private int litTicks;
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

    /**
     * Drives BlockStateProperties.LIT, for blocks that have it, from whether the machine worked this tick.
     * The flame lingers for {@code linger} ticks so bursty or power-starved work does not flicker the model.
     */
    protected void updateLit(boolean working, int linger) {
        if (level == null || level.isClientSide) return;
        if (working) litTicks = linger;
        else if (litTicks > 0) litTicks--;
        BlockState state = getBlockState();
        boolean lit = litTicks > 0;
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy.setEnergyStored(tag.getInt("Energy"));
    }
}
