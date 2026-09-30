package github.com.gengyoubo.CE.LP.energy;

import mekanism.api.Action;
import mekanism.api.energy.ISidedStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import net.minecraft.core.Direction;

/** Bridges Mekanism joules to the Changed machines' LP store (100 J per LP). */
public final class WorkbenchMekanismEnergyHandler implements ISidedStrictEnergyHandler {
    private static final long JOULES_PER_LP = 100;
    private final WorkbenchEnergyHolder holder;
    private FloatingLong fractionalJoules = FloatingLong.ZERO;

    public WorkbenchMekanismEnergyHandler(WorkbenchEnergyHolder holder) {
        this.holder = holder;
    }

    @Override public int getEnergyContainerCount(Direction side) { return 1; }
    @Override public FloatingLong getEnergy(int container, Direction side) {
        if (container != 0) return FloatingLong.ZERO;
        return FloatingLong.create((long) holder.changede$getWorkbenchEnergy().getEnergyStored() * JOULES_PER_LP)
                .add(fractionalJoules);
    }
    @Override public void setEnergy(int container, FloatingLong amount, Direction side) {
        if (container != 0) return;
        FloatingLong bounded = amount.max(FloatingLong.ZERO)
                .min(FloatingLong.create((long) holder.changede$getWorkbenchEnergy().getMaxEnergyStored() * JOULES_PER_LP));
        int wholeLp = bounded.divideToInt(FloatingLong.create(JOULES_PER_LP));
        holder.changede$getWorkbenchEnergy().setEnergyStored(wholeLp);
        fractionalJoules = bounded.subtract(FloatingLong.create((long) wholeLp * JOULES_PER_LP));
    }
    @Override public FloatingLong getMaxEnergy(int container, Direction side) {
        if (container != 0) return FloatingLong.ZERO;
        return FloatingLong.create((long) holder.changede$getWorkbenchEnergy().getMaxEnergyStored() * JOULES_PER_LP);
    }
    @Override public FloatingLong getNeededEnergy(int container, Direction side) {
        if (container != 0) return FloatingLong.ZERO;
        return getMaxEnergy(container, side).subtract(getEnergy(container, side)).max(FloatingLong.ZERO);
    }
    @Override public FloatingLong insertEnergy(int container, FloatingLong amount, Direction side, Action action) {
        if (container != 0 || amount.isZero() || amount.smallerThan(FloatingLong.ZERO)) return amount;
        FloatingLong accepted = amount.min(getNeededEnergy(container, side));
        if (accepted.isZero()) return amount;
        if (action.execute()) {
            FloatingLong total = fractionalJoules.add(accepted);
            int wholeLp = total.divideToInt(FloatingLong.create(JOULES_PER_LP));
            int received = holder.changede$getWorkbenchEnergy().receiveEnergy(wholeLp, false);
            fractionalJoules = total.subtract(FloatingLong.create((long) received * JOULES_PER_LP));
        }
        return amount.subtract(accepted);
    }
    @Override public FloatingLong extractEnergy(int container, FloatingLong amount, Direction side, Action action) {
        return FloatingLong.ZERO;
    }
}
