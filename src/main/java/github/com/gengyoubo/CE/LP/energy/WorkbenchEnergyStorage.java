package github.com.gengyoubo.CE.LP.energy;

import net.minecraftforge.energy.EnergyStorage;

public class WorkbenchEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public WorkbenchEnergyStorage(int capacity, Runnable onChanged) {
        super(capacity, capacity, 0);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            onChanged.run();
        }
        return received;
    }

    public boolean consumeEnergy(int amount, boolean simulate) {
        if (energy < amount) {
            return false;
        }
        if (!simulate) {
            energy -= amount;
            onChanged.run();
        }
        return true;
    }

    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(energy, capacity));
        onChanged.run();
    }
}
