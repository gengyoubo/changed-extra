package github.com.gengyoubo.CE.LP.energy;

/** Pure per-tick accounting; both buffers must afford a step before either is charged. */
public final class DualEnergyProcess {
    public record Step(int white, int dark, int whiteRemainder, int darkRemainder) {
        public boolean affordable(int whiteStored, int darkStored) { return whiteStored >= white && darkStored >= dark; }
    }
    public static Step step(int whiteRate, int darkRate, int whiteRemainder, int darkRemainder, boolean finishing) {
        long white = (long) whiteRate + whiteRemainder, dark = (long) darkRate + darkRemainder;
        return new Step((int) (white / 20 + (finishing && white % 20 > 0 ? 1 : 0)),
                (int) (dark / 20 + (finishing && dark % 20 > 0 ? 1 : 0)),
                finishing ? 0 : (int) (white % 20), finishing ? 0 : (int) (dark % 20));
    }
    private DualEnergyProcess() {}
}
