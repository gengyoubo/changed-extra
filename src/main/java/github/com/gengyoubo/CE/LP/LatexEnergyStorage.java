package github.com.gengyoubo.CE.LP;

import net.minecraft.core.Direction;

public class LatexEnergyStorage implements ILatexEnergyHandler {

    private int energy;
    private final int capacity;

    public LatexEnergyStorage(int capacity) {
        this.capacity = capacity;
    }

    public void setEnergyStored(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    @Override
    public int receiveEnergy(int amount, Direction from) {
        int accepted = Math.min(Math.max(0, capacity - energy), Math.max(0, amount));
        energy += accepted;
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, Direction from) {
        int extracted = Math.min(Math.max(0, energy), Math.max(0, amount));
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
