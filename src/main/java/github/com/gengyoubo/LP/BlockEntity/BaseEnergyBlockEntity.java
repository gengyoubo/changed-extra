package github.com.gengyoubo.LP.BlockEntity;

import github.com.gengyoubo.LP.ILatexEnergyHandler;
import github.com.gengyoubo.LP.LatexEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BaseEnergyBlockEntity extends BlockEntity implements ILatexEnergyHandler {
    protected final LatexEnergyStorage energy;

    public BaseEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.energy = new LatexEnergyStorage(capacity);
    }

    // ===== 能量接口 =====
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

    // ===== 统一传电逻辑 =====
    protected void pushEnergy() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));

            if (neighbor instanceof ILatexEnergyHandler handler) {
                int extracted = this.extractEnergy(100, dir);
                int received = handler.receiveEnergy(extracted, dir.getOpposite());

                // 回退多余能量
                if (received < extracted) {
                    this.receiveEnergy(extracted - received, dir);
                }
            }
        }
    }

    // ===== tick入口（子类可以重写）=====
    public void tick() {
        pushEnergy(); // 默认自动输出
    }
}
