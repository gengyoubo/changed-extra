package github.com.gengyoubo.LP;

import net.minecraft.core.Direction;

public interface ILatexEnergyHandler {

    // 接收能量
    int receiveEnergy(int amount, Direction from);

    // 提供能量
    int extractEnergy(int amount, Direction from);

    // 当前能量
    int getEnergyStored();

    // 最大容量
    int getMaxEnergyStored();
}