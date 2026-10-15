package github.com.gengyoubo.CE.LP;

import github.com.gengyoubo.CE.LP.energy.DualEnergyProcess;

public final class DualEnergyProcessChecks {
    private static int checks;
    private static void check(boolean condition) { if (!condition) throw new AssertionError("Dual energy check " + checks); checks++; }
    public static void main(String[] args) {
        for (int whiteRate : new int[]{1, 19, 20, 21, 200, 2_000_000}) for (int darkRate : new int[]{1, 13, 20, 201, 2_000_000}) {
            for (int ticks : new int[]{1, 3, 19, 20, 21, 200, 2001}) {
                long whiteTotal = 0, darkTotal = 0; int whiteRemainder = 0, darkRemainder = 0;
                for (int tick = 0; tick < ticks; tick++) {
                    var step = DualEnergyProcess.step(whiteRate, darkRate, whiteRemainder, darkRemainder, tick + 1 == ticks);
                    check(step.white() >= 0 && step.dark() >= 0);
                    check(step.affordable(step.white(), step.dark()));
                    if (step.white() > 0) check(!step.affordable(step.white() - 1, step.dark()));
                    if (step.dark() > 0) check(!step.affordable(step.white(), step.dark() - 1));
                    whiteTotal += step.white(); darkTotal += step.dark();
                    whiteRemainder = step.whiteRemainder(); darkRemainder = step.darkRemainder();
                }
                check(whiteTotal == ((long) whiteRate * ticks + 19) / 20);
                check(darkTotal == ((long) darkRate * ticks + 19) / 20);
                check(whiteRemainder == 0 && darkRemainder == 0);
            }
        }
        System.out.println("DUAL ENERGY PASS: " + checks + " checks");
    }
}
