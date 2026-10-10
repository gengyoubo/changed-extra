package github.com.gengyoubo.CE.compat.create.burner;

/** Pure rules: a fuel payment belongs to one lane; processing never creates extra batches. */
public final class LatexBurnerRules {
    public static final int FLUID_PER_FUEL = 250;
    public static final int MAX_FUEL_TICKS = 72_000;
    public static final int SPEED_SCALE = 4;

    private LatexBurnerRules() {}

    public record Profile(String latex, int speedUnits, int fuelTicks) {
        public Profile {
            if ((!latex.equals("dark") && !latex.equals("white")) || speedUnits < 1 || speedUnits > 16
                    || fuelTicks < 1 || fuelTicks > MAX_FUEL_TICKS) throw new IllegalArgumentException("Invalid burner profile");
        }
        public double speed() { return speedUnits / (double) SPEED_SCALE; }
    }

    public static int heat(boolean captured, int itemTicks, int fluidTicks) {
        if (!captured) return 0;
        if (itemTicks > 0 && fluidTicks > 0) return 3;
        return itemTicks > 0 || fluidTicks > 0 ? 2 : 1;
    }

    /** Fixed-point progress, capped so Create's zero comparison always executes exactly once. */
    public static final class Progress {
        private int remainder;
        public int advance(int remaining, int speedUnits) {
            if (remaining <= 0) { reset(); return 0; }
            remainder += speedUnits;
            int steps = Math.min(remaining, remainder / SPEED_SCALE);
            remainder %= SPEED_SCALE;
            if (steps == remaining) reset();
            return remaining - steps;
        }
        public void reset() { remainder = 0; }
    }
}
