package github.com.gengyoubo.CE.client;

public final class SpearJabAnimationTest {
    public static void main(String[] args) {
        check(SpearJabAnimation.sample(0).equals(new SpearJabAnimation.Pose(0, 0, 0)), "Starts at rest");
        check(SpearJabAnimation.sample(1).equals(new SpearJabAnimation.Pose(0, 0, 0)), "Returns to rest");
        check(SpearJabAnimation.sample(0.1F).thrust() == 0, "Preparation precedes thrust");
        check(SpearJabAnimation.sample(0.3F).thrust() == 1, "Thrust reaches full extension");
        check(SpearJabAnimation.sample(0.45F).thrust() == 1, "Brief hold before recovery");
        float last = 1;
        for (int i = 450; i <= 1000; i++) {
            var sample = SpearJabAnimation.sample(i / 1000F);
            check(sample.thrust() <= last && sample.thrust() >= 0, "Recovery is monotonic");
            check(Float.isFinite(sample.armPitch()), "Finite arm rotation");
            last = sample.thrust();
        }
        check(SpearJabAnimation.DURATION_TICKS < 20, "Animation finishes before the next jab");
        System.out.println("All spear jab animation checks passed");
    }
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
