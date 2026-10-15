package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.nbt.CompoundTag;

/** Per-module output-only storage. Every side/API consumes the same per-world-tick output budget. */
public final class ThermoelectricEnergy implements IStrictEnergyHandler, IEnergyStorage {
    private final DoubleSupplier capacity;
    private final IntSupplier output;
    private final LongSupplier clock;
    private final Runnable changed;
    private double joules;
    private long budgetTick = Long.MIN_VALUE;
    private double exportedJoules;

    public ThermoelectricEnergy(DoubleSupplier capacity, IntSupplier output, LongSupplier clock, Runnable changed) {
        this.capacity = capacity; this.output = output; this.clock = clock; this.changed = changed;
    }
    public static double fromFE(int fe) { return EnergyUnit.FORGE_ENERGY.convertFrom(FloatingLong.create((long) Math.max(0, fe))).doubleValue(); }
    public static double toFE(double joules) { return EnergyUnit.FORGE_ENERGY.convertTo(FloatingLong.create(Math.max(0, joules))).doubleValue(); }
    public double storedJoules() { return joules; }
    public double neededJoules() { return Math.max(0, capacity.getAsDouble() - joules); }
    public void loadJoules(double value) {
        joules = Double.isFinite(value) ? Math.max(0, value) : 0;
        // Preserve an existing balance when a server lowers its configured capacity: stop generation until drained.
    }
    public double generate(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return 0;
        double accepted = Math.min(amount, neededJoules());
        if (accepted > 0) { joules += accepted; changed.run(); }
        return accepted;
    }
    public void refundOutput(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || clock.getAsLong() != budgetTick) return;
        double refund = Math.min(amount, exportedJoules);
        // A refund restores an existing balance even if the capacity was lowered while it was stored.
        joules += refund; exportedJoules -= refund; changed.run();
    }
    public void save(CompoundTag tag) {
        tag.putDouble("Joules", joules); tag.putLong("OutputTick", budgetTick); tag.putDouble("ExportedJoules", exportedJoules);
    }
    public void load(CompoundTag tag) {
        loadJoules(tag.getDouble("Joules"));
        budgetTick = tag.contains("OutputTick") ? tag.getLong("OutputTick") : Long.MIN_VALUE;
        double used = tag.getDouble("ExportedJoules");
        exportedJoules = Double.isFinite(used) ? Math.max(0, used) : 0;
    }
    private double remainingOutput() {
        return Math.max(0, fromFE(output.getAsInt()) - (clock.getAsLong() == budgetTick ? exportedJoules : 0));
    }
    private void deduct(double amount) {
        long tick = clock.getAsLong();
        if (tick != budgetTick) { budgetTick = tick; exportedJoules = 0; }
        exportedJoules += amount;
        joules = Math.max(0, joules - amount);
        changed.run();
    }
    @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
    @Override public int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int accepted = Math.min(amount, (int) Math.min(Integer.MAX_VALUE, toFE(Math.min(joules, remainingOutput()))));
        double cost = fromFE(accepted);
        // Conversion has fixed-point precision. Never give out even one FE more than the stored balance buys.
        if (cost > joules || cost > remainingOutput()) { accepted = Math.max(0, accepted - 1); cost = fromFE(accepted); }
        if (!simulate && accepted > 0) deduct(cost);
        return accepted;
    }
    @Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, toFE(joules)); }
    @Override public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, toFE(capacity.getAsDouble())); }
    @Override public boolean canExtract() { return true; }
    @Override public boolean canReceive() { return false; }
    @Override public int getEnergyContainerCount() { return 1; }
    @Override public FloatingLong getEnergy(int container) { return container == 0 ? FloatingLong.create(joules) : FloatingLong.ZERO; }
    @Override public void setEnergy(int container, FloatingLong amount) { throw new UnsupportedOperationException("Thermoelectric output storage cannot be filled externally"); }
    @Override public FloatingLong getMaxEnergy(int container) { return container == 0 ? FloatingLong.create(capacity.getAsDouble()) : FloatingLong.ZERO; }
    @Override public FloatingLong getNeededEnergy(int container) { return container == 0 ? FloatingLong.create(neededJoules()) : FloatingLong.ZERO; }
    @Override public FloatingLong insertEnergy(int container, FloatingLong amount, Action action) { return amount; }
    @Override public FloatingLong extractEnergy(int container, FloatingLong amount, Action action) {
        if (container != 0) return FloatingLong.ZERO;
        var accepted = FloatingLong.create(Math.min(amount.doubleValue(), Math.min(joules, remainingOutput())));
        if (action.execute() && !accepted.isZero()) deduct(accepted.doubleValue());
        return accepted;
    }
}
