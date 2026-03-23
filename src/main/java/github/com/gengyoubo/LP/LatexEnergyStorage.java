package github.com.gengyoubo.LP;

import net.minecraft.core.Direction;

public class LatexEnergyStorage implements ILatexEnergyHandler {

    private int energy;
    private final int capacity;

    public LatexEnergyStorage(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        int accepted = Math.min(capacity - energy, amount);
        energy += accepted;
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        int extracted = Math.min(energy, amount);
        energy -= extracted;
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }
}
