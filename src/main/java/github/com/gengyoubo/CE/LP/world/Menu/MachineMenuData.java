package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class MachineMenuData {
    static final int COUNT = 4;

    private MachineMenuData() {
    }

    static ContainerData create(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                return switch (index) {
                    case 0 -> blockEntity instanceof BaseEnergyBlockEntity energy ? energy.getEnergyStored() : 0;
                    case 1 -> blockEntity instanceof BaseEnergyBlockEntity energy ? energy.getMaxEnergyStored() : 0;
                    case 2 -> blockEntity instanceof MachineBlockEntity machine ? machine.getProgress() : 0;
                    case 3 -> blockEntity instanceof MachineBlockEntity machine ? machine.getMaxProgressValue() : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return COUNT;
            }
        };
    }
}
