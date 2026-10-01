package github.com.gengyoubo.CE.client;

/** Shared, frame-rate independent jab curve: prepare, thrust, hold, recover. */
public final class SpearJabAnimation {
    public static final int DURATION_TICKS = 10;

    public record Pose(float raise, float thrust, float armPitch) { }

    public static Pose sample(float progress) {
        float prepare = smooth(range(progress, 0, 0.1F));
        float attack = range(progress, 0.1F, 0.3F);
        attack *= attack;
        float recover = smooth(range(progress, 0.45F, 1));
        float raise = prepare - recover;
        float thrust = attack - recover;
        return new Pose(raise, thrust, (15 * prepare - 80 * attack + 65 * recover) * raise);
    }

    private static float range(float value, float start, float end) {
        return Math.max(0, Math.min(1, (value - start) / (end - start)));
    }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }
    private SpearJabAnimation() { }
}
