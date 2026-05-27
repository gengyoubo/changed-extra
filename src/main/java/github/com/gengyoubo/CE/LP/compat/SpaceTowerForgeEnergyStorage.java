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
        SpaceTowerEnergyType type = getReceiveType();
        if (maxReceive <= 0 || type == null) {
            return 0;
        }

        int accepted = Math.min(maxReceive, getFreeForgeEnergy());
        if (!simulate && accepted > 0) {
            tower.receiveEnergyAsType(type, accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        SpaceTowerEnergyType type = getExtractType();
        if (maxExtract <= 0 || type == null) {
            return 0;
        }

        int extracted = Math.min(maxExtract, getEnergyStored());
        if (!simulate && extracted > 0) {
            return (int)Math.floor(tower.extractEnergyAsType(type, extracted));
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
        return getExtractType() != null && getEnergyStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return getReceiveType() != null && getFreeForgeEnergy() > 0;
    }

    private int getFreeForgeEnergy() {
        return Math.max(0, getMaxEnergyStored() - getEnergyStored());
    }

    public static SpaceTowerEnergyType getReceiveType(SpaceTowerAccess tower) {
        if (tower.getMode(SpaceTowerEnergyType.RF) == IOType.INPUT) {
            return SpaceTowerEnergyType.RF;
        }
        if (tower.getMode(SpaceTowerEnergyType.J) == IOType.INPUT) {
            return SpaceTowerEnergyType.J;
        }
        return null;
    }

    public static SpaceTowerEnergyType getExtractType(SpaceTowerAccess tower) {
        if (tower.getMode(SpaceTowerEnergyType.RF) == IOType.OUTPUT) {
            return SpaceTowerEnergyType.RF;
        }
        if (tower.getMode(SpaceTowerEnergyType.J) == IOType.OUTPUT) {
            return SpaceTowerEnergyType.J;
        }
        return null;
    }

    private SpaceTowerEnergyType getReceiveType() {
        return getReceiveType(tower);
    }

    private SpaceTowerEnergyType getExtractType() {
        return getExtractType(tower);
    }
}
