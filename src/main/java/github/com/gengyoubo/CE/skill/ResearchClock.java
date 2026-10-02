package github.com.gengyoubo.CE.skill;

/** One paid second is indivisible: insufficient energy never advances or charges a job. */
public final class ResearchClock {
    private ResearchClock() { }
    public record Step(int progressTicks, int energySpent, boolean advanced, boolean completed) { }
    public static Step second(int progressTicks, int durationTicks, int wlpPerSecond, int availableWlp, boolean running) {
        if (durationTicks < 0 || durationTicks % 20 != 0 || wlpPerSecond < 0)
            throw new IllegalArgumentException("Research duration must be whole seconds and costs non-negative");
        int progress = Math.max(0, Math.min(progressTicks, durationTicks));
        if (progress == durationTicks) return new Step(progress, 0, false, true);
        if (!running || availableWlp < wlpPerSecond) return new Step(progress, 0, false, false);
        int next = Math.min(durationTicks, progress + 20);
        return new Step(next, wlpPerSecond, true, next == durationTicks);
    }
}
