package github.com.gengyoubo.CE.compat.mekanism.fission;

/** Pure heat accounting; all quantities are Joules per tick, never FE. */
public final class ThermoelectricModel {
    public static final double START_TEMPERATURE = 450;
    public static final double RATED_TEMPERATURE = 1100;
    public static final double WARNING_TEMPERATURE = 1200;
    public static final double FUSE_TEMPERATURE = 1800;

    public record Transfer(double heat, double electricity, double passiveHeat, double environmentalHeat) {
        public static final Transfer ZERO = new Transfer(0, 0, 0, 0);
        public Transfer scale(double factor) {
            return new Transfer(heat * factor, electricity * factor, passiveHeat * factor, environmentalHeat * factor);
        }
    }

    public static double request(double temperature, double risingKelvinPerSecond, double maximum) {
        double prediction = temperature + Math.min(100, 4 * Math.max(0, risingKelvinPerSecond));
        double load = Math.max(0, Math.min(1, (prediction - START_TEMPERATURE) / (RATED_TEMPERATURE - START_TEMPERATURE)));
        return Math.max(0, maximum) * Math.pow(load, 1.5);
    }

    public static double efficiency(double temperature, double ambient, double coefficient) {
        return temperature > 0 ? Math.max(0, Math.min(1, coefficient)) * Math.max(0, Math.min(1, 1 - ambient / temperature)) : 0;
    }

    public static boolean mustFuse(double temperature, double reactorDamage) {
        return temperature >= FUSE_TEMPERATURE || (temperature >= WARNING_TEMPERATURE && reactorDamage >= 100);
    }

    public static Transfer plan(double request, double efficiency, double energySpace, double passiveFraction) {
        if (request <= 0) return Transfer.ZERO;
        double converting = efficiency > 0 ? Math.min(request, Math.max(0, energySpace) / efficiency) : 0;
        double passive = Math.min(Math.max(0, request - converting), Math.max(0, Math.min(1, passiveFraction)) * request);
        double electricity = efficiency * converting;
        double heat = converting + passive;
        return new Transfer(heat, electricity, passive, heat - electricity);
    }

    /** Proportional allocation makes the result independent of module traversal order. */
    public static double allocationScale(double availableHeat, double requestedHeat) {
        return requestedHeat > 0 ? Math.min(1, Math.max(0, availableHeat) / requestedHeat) : 0;
    }
    private ThermoelectricModel() {}
}
