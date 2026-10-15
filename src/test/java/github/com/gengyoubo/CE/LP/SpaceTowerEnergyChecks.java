package github.com.gengyoubo.CE.LP;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerAccess;
import github.com.gengyoubo.CE.LP.compat.SpaceTowerForgeEnergyStorage;
import github.com.gengyoubo.CE.LP.energy.SpaceTowerEnergyConversion;

public final class SpaceTowerEnergyChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-7, message + ": " + actual + " != " + expected);
    }

    public static void main(String[] args) {
        for (var type : SpaceTowerEnergyType.values()) {
            var received = SpaceTowerEnergyConversion.receive(0, 0, 10, type, 1);
            near(SpaceTowerEnergyConversion.joules(received), type.joulesPerUnit(), "Unit conversion: " + type);
            var extracted = SpaceTowerEnergyConversion.extract(received.lp(), received.remainder(), 10, type, 1);
            near(extracted.amount(), 1, "Round trip amount: " + type);
            near(SpaceTowerEnergyConversion.joules(extracted), 0, "Round trip conservation: " + type);
            var full = SpaceTowerEnergyConversion.receive(0, 0, 10, type, 1e20);
            near(SpaceTowerEnergyConversion.joules(full), 1000, "Capacity must bound storage");
            var drained = SpaceTowerEnergyConversion.extract(full.lp(), full.remainder(), 10, type, 1e20);
            near(SpaceTowerEnergyConversion.joules(drained), 0, "Extraction must not underflow");
        }
        for (double invalid : new double[]{-1, 0, Double.NaN, Double.POSITIVE_INFINITY}) {
            var unchanged = SpaceTowerEnergyConversion.receive(2, 3, 10, SpaceTowerEnergyType.RF, invalid);
            near(SpaceTowerEnergyConversion.joules(unchanged), 203, "Invalid receive must not modify storage");
            unchanged = SpaceTowerEnergyConversion.extract(2, 3, 10, SpaceTowerEnergyType.RF, invalid);
            near(SpaceTowerEnergyConversion.joules(unchanged), 203, "Invalid extraction must not modify storage");
        }
        var normalized = SpaceTowerEnergyConversion.normalize(20, Double.NaN, 10);
        near(SpaceTowerEnergyConversion.joules(normalized), 1000, "Loaded storage must be bounded");

        var tower = new Tower();
        var forge = new SpaceTowerForgeEnergyStorage(tower);
        check(forge.getMaxEnergyStored() == 40, "One LP capacity must expose 40 RF");
        check(forge.receiveEnergy(5, true) == 5 && forge.getEnergyStored() == 0, "Simulation must not receive energy");
        check(forge.receiveEnergy(5, false) == 5 && forge.getEnergyStored() == 5, "Fractional LP must retain RF");
        check(forge.receiveEnergy(Integer.MAX_VALUE, false) == 35 && !forge.canReceive(), "Receive must respect free capacity");
        check(forge.extractEnergy(1, false) == 0, "Input mode must not extract");
        tower.setMode(SpaceTowerEnergyType.RF, IOType.OUTPUT);
        check(forge.extractEnergy(5, true) == 5 && forge.getEnergyStored() == 40, "Simulation must not extract energy");
        check(forge.receiveEnergy(1, false) == 0, "Output mode must not receive");
        check(forge.extractEnergy(Integer.MAX_VALUE, false) == 40 && !forge.canExtract(), "Extraction must respect stored energy");
        tower.setMode(SpaceTowerEnergyType.RF, IOType.NONE);
        check(!forge.canReceive() && !forge.canExtract(), "Disabled mode must expose no transfer");
        check(forge.receiveEnergy(-1, false) == 0 && forge.extractEnergy(-1, false) == 0, "Negative requests must be rejected");
        System.out.println("All space tower energy checks passed");
    }

    private static final class Tower implements SpaceTowerAccess {
        private SpaceTowerEnergyConversion.Transfer energy = SpaceTowerEnergyConversion.normalize(0, 0, 1);
        private IOType mode = IOType.INPUT;
        public IOType getMode(SpaceTowerEnergyType type) { return type == SpaceTowerEnergyType.RF ? mode : IOType.NONE; }
        public void toggleMode(SpaceTowerEnergyType type) { setMode(type, IOType.values()[(mode.ordinal() + 1) % 3]); }
        public void setMode(SpaceTowerEnergyType type, IOType value) { if (type == SpaceTowerEnergyType.RF) mode = value; }
        public double receiveEnergyAsType(SpaceTowerEnergyType type, double amount, boolean simulate) {
            if (getMode(type) != IOType.INPUT) return 0;
            var transfer = SpaceTowerEnergyConversion.receive(energy.lp(), energy.remainder(), 1, type, amount);
            if (!simulate) energy = transfer;
            return transfer.amount();
        }
        public double extractEnergyAsType(SpaceTowerEnergyType type, double amount, boolean simulate) {
            if (getMode(type) != IOType.OUTPUT) return 0;
            var transfer = SpaceTowerEnergyConversion.extract(energy.lp(), energy.remainder(), 1, type, amount);
            if (!simulate) energy = transfer;
            return transfer.amount();
        }
        public double getStoredJoules() { return SpaceTowerEnergyConversion.joules(energy); }
        public void refundEnergyAsType(SpaceTowerEnergyType type, double amount) {
            energy = SpaceTowerEnergyConversion.receive(energy.lp(), energy.remainder(), 1, type, amount);
        }
        public int getEnergyStored() { return energy.lp(); }
        public int getMaxEnergyStored() { return 1; }
        public int getJouleBufferDisplay() { return (int) energy.remainder(); }
        public int getCeStoredLpDisplay() { return 0; }
        public int getMaxCeStoredLp() { return 0; }
        public int getCeRpm() { return 0; }
        public int getCeSu() { return 0; }
        public int getCeCostPerMinute() { return 0; }
        public void adjustCeRpm(int delta) {}
        public void adjustCeSu(int delta) {}
        public void setCeRpm(int rpm) {}
        public void setCeSu(int su) {}
    }
}
