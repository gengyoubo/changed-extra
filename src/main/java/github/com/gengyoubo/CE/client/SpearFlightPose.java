package github.com.gengyoubo.CE.client;

/** Angles in degrees. Matches the body rotation applied by the player/Changed renderer. */
public final class SpearFlightPose {
    public static float bodyPitch(float entityPitch, float flyingTicks) {
        float amount = Math.max(0, Math.min(1, flyingTicks * flyingTicks / 100));
        return amount * (-90 - entityPitch);
    }

    public static float aimPitch(float viewPitch, float entityPitch, float flyingTicks) {
        // The renderer flips model Y before drawing it, so add its body pitch here.
        return viewPitch + bodyPitch(entityPitch, flyingTicks);
    }

    private SpearFlightPose() { }
}
