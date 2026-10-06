package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;

/** Bounded LP storage with a fractional remainder, shared by every tower adapter. */
public final class SpaceTowerEnergyConversion {
    private static final double JOULES_PER_LP = SpaceTowerEnergyType.LP.joulesPerUnit();

    private SpaceTowerEnergyConversion() {
    }

    public record Transfer(int lp, double remainder, double amount) {
    }

    public static Transfer normalize(int lp, double remainder, int capacity) {
        double total = Math.max(0, Math.min(capacity, lp)) * JOULES_PER_LP;
        if (Double.isFinite(remainder) && remainder > 0) {
            total = Math.min(capacity * JOULES_PER_LP, total + remainder);
        }
        return split(total, 0);
    }

    public static Transfer receive(int lp, double remainder, int capacity, SpaceTowerEnergyType type, double requested) {
        Transfer current = normalize(lp, remainder, capacity);
        if (!Double.isFinite(requested) || requested <= 0) {
            return current;
        }
        double total = joules(current);
        double accepted = Math.min(requested, (capacity * JOULES_PER_LP - total) / type.joulesPerUnit());
        return split(Math.min(capacity * JOULES_PER_LP, total + accepted * type.joulesPerUnit()), accepted);
    }

    public static Transfer extract(int lp, double remainder, int capacity, SpaceTowerEnergyType type, double requested) {
        Transfer current = normalize(lp, remainder, capacity);
        if (!Double.isFinite(requested) || requested <= 0) {
            return current;
        }
        double total = joules(current);
        double extracted = Math.min(requested, total / type.joulesPerUnit());
        return split(Math.max(0, total - extracted * type.joulesPerUnit()), extracted);
    }

    public static double joules(Transfer storage) {
        return storage.lp() * JOULES_PER_LP + storage.remainder();
    }

    private static Transfer split(double total, double amount) {
        int lp = (int)Math.floor(total / JOULES_PER_LP);
        return new Transfer(lp, Math.max(0, total - lp * JOULES_PER_LP), amount);
    }
}
