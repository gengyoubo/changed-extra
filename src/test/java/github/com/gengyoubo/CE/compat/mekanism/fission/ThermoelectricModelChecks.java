package github.com.gengyoubo.CE.compat.mekanism.fission;

/** No game bootstrap is needed for these conservation checks. */
public final class ThermoelectricModelChecks {
    private static int checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("Check " + checks); checks++; }
    private static void near(double a, double b) { check(Math.abs(a - b) < 1.0E-7); }
    public static void main(String[] args) {
        near(ThermoelectricModel.request(300, 0, 100_000), 0);
        near(ThermoelectricModel.request(450, 0, 100_000), 0);
        near(ThermoelectricModel.request(1100, 0, 100_000), 100_000);
        near(ThermoelectricModel.request(1700, 0, 100_000), 100_000);
        near(ThermoelectricModel.request(600, -100, 100_000), ThermoelectricModel.request(600, 0, 100_000));
        near(ThermoelectricModel.request(600, 1_000_000, 100_000), ThermoelectricModel.request(700, 0, 100_000));
        check(ThermoelectricModel.request(600, 10, 100_000) > ThermoelectricModel.request(600, 0, 100_000));
        near(ThermoelectricModel.efficiency(1000, 300, .25), .175);
        near(ThermoelectricModel.efficiency(300, 350, .25), 0);
        near(ThermoelectricModel.plan(100_000, .2, 0, .25).heat(), 25_000);
        near(ThermoelectricModel.plan(100_000, .2, 10_000, .25).heat(), 75_000);
        near(ThermoelectricModel.plan(100_000, .2, 100_000, .25).heat(), 100_000);
        check(!ThermoelectricModel.mustFuse(1799, 99));
        check(ThermoelectricModel.mustFuse(1800, 0));
        check(ThermoelectricModel.mustFuse(1200, 100));
        check(!ThermoelectricModel.mustFuse(1199, 100));
        for (double space : new double[]{0, .000001, 1, 100, 5000, 25000, 1_000_000}) {
            for (double efficiency : new double[]{0, .01, .175, .25, 1}) {
                var plan = ThermoelectricModel.plan(100_000, efficiency, space, .25);
                near(plan.heat(), plan.electricity() + plan.environmentalHeat());
                check(plan.heat() <= 100_000 && plan.heat() >= 0);
                check(plan.electricity() <= space + 1.0E-7 && plan.environmentalHeat() >= 0);
                check(plan.passiveHeat() <= 25_000);
                for (double available : new double[]{0, .001, 100, 99999, 1_000_000}) {
                    double scale = ThermoelectricModel.allocationScale(available, plan.heat() * 8);
                    var allocated = plan.scale(scale);
                    check(allocated.heat() * 8 <= available + 1.0E-7);
                    near(allocated.heat(), allocated.electricity() + allocated.environmentalHeat());
                }
            }
        }
        // Finite cooling cannot stabilize arbitrarily large fuel heat input.
        check(1_000_000 - 8 * ThermoelectricModel.request(1799, 0, 100_000) > 0);
        System.out.println("THERMOELECTRIC MODEL PASS: " + checks + " checks");
    }
}
