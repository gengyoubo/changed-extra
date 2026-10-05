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

        int accepted = (int)Math.floor(tower.receiveEnergyAsType(type, maxReceive, true));
        return simulate ? accepted : (int)Math.floor(tower.receiveEnergyAsType(type, accepted, false));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        SpaceTowerEnergyType type = getExtractType();
        if (maxExtract <= 0 || type == null) {
            return 0;
        }

        int extracted = (int)Math.floor(tower.extractEnergyAsType(type, maxExtract, true));
        return simulate ? extracted : (int)Math.floor(tower.extractEnergyAsType(type, extracted, false));
    }

    @Override
    public int getEnergyStored() {
        return (int)Math.min(Integer.MAX_VALUE, Math.floor(tower.getStoredJoules() / SpaceTowerEnergyType.RF.joulesPerUnit()));
    }

    @Override
    public int getMaxEnergyStored() {
        return (int)Math.min(Integer.MAX_VALUE, tower.getMaxEnergyStored() * SpaceTowerEnergyType.LP.joulesPerUnit() / SpaceTowerEnergyType.RF.joulesPerUnit());
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
        return null;
    }

    public static SpaceTowerEnergyType getExtractType(SpaceTowerAccess tower) {
        if (tower.getMode(SpaceTowerEnergyType.RF) == IOType.OUTPUT) {
            return SpaceTowerEnergyType.RF;
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
