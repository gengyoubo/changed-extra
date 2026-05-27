package github.com.gengyoubo.CE.LP.compat;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerAccess;
import net.minecraftforge.energy.IEnergyStorage;

public class SpaceTowerForgeEnergyStorage implements IEnergyStorage {
    private final SpaceTowerAccess tower;

    public SpaceTowerForgeEnergyStorage(SpaceTowerAccess tower) {
        this.tower = tower;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0 || !canReceive()) {
            return 0;
        }

        int accepted = Math.min(maxReceive, getFreeForgeEnergy());
        if (!simulate && accepted > 0) {
            tower.receiveEnergyAsType(SpaceTowerEnergyType.J, accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || !canExtract()) {
            return 0;
        }

        int extracted = Math.min(maxExtract, getEnergyStored());
        if (!simulate && extracted > 0) {
            return (int)Math.floor(tower.extractEnergyAsType(SpaceTowerEnergyType.J, extracted));
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return Math.min(Integer.MAX_VALUE, tower.getEnergyStored() * (int)SpaceTowerEnergyType.LP.joulesPerUnit());
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.min(Integer.MAX_VALUE, tower.getMaxEnergyStored() * (int)SpaceTowerEnergyType.LP.joulesPerUnit());
    }

    @Override
    public boolean canExtract() {
        return tower.getMode(SpaceTowerEnergyType.J) == IOType.OUTPUT && getEnergyStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return tower.getMode(SpaceTowerEnergyType.J) == IOType.INPUT && getFreeForgeEnergy() > 0;
    }

    private int getFreeForgeEnergy() {
        return Math.max(0, getMaxEnergyStored() - getEnergyStored());
    }
}
