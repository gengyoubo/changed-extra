package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;

public interface SpaceTowerAccess {
    IOType getMode(SpaceTowerEnergyType type);

    void toggleMode(SpaceTowerEnergyType type);

    void setMode(SpaceTowerEnergyType type, IOType mode);

    default double receiveEnergyAsType(SpaceTowerEnergyType type, double amount) {
        return receiveEnergyAsType(type, amount, false);
    }

    double receiveEnergyAsType(SpaceTowerEnergyType type, double amount, boolean simulate);

    default double extractEnergyAsType(SpaceTowerEnergyType type, double requestedAmount) {
        return extractEnergyAsType(type, requestedAmount, false);
    }

    double extractEnergyAsType(SpaceTowerEnergyType type, double requestedAmount, boolean simulate);

    double getStoredJoules();

    void refundEnergyAsType(SpaceTowerEnergyType type, double amount);

    int getEnergyStored();

    int getMaxEnergyStored();

    int getJouleBufferDisplay();

    int getCeStoredLpDisplay();

    int getMaxCeStoredLp();

    int getCeRpm();

    int getCeSu();

    int getCeCostPerMinute();

    void adjustCeRpm(int delta);

    void adjustCeSu(int delta);

    void setCeRpm(int rpm);

    void setCeSu(int su);
}
