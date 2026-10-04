package github.com.gengyoubo.CE.skill;

/** Pure nutrition rules shared by the food hooks and regression checks. */
public final class SkillNutrition {
    private SkillNutrition() { }
    public record Meal(int nutrition, float saturationModifier) { }
    /** Scale the saturation gain, which vanilla computes as nutrition * modifier * 2. */
    public static Meal dietMeal(int nutrition, float modifier) {
        int scaled = (int) Math.min(Integer.MAX_VALUE, Math.round(Math.max(0, nutrition) * 1.5));
        float saturation = scaled == 0 ? modifier : (float) (modifier * (double) nutrition * 1.5 / scaled);
        return new Meal(scaled, saturation);
    }
    public static float exhaustion(float amount, double reduction) {
        return amount > 0 ? (float) (amount * (1 - Math.max(0, Math.min(1, reduction)))) : amount;
    }
    public static float foodSaturation(float originalModifier, double multiplier) {
        return (float) (originalModifier * Math.max(0, multiplier));
    }
    public static float saturationCap(float foodLevel, double capacity) {
        return (float) (foodLevel + Math.max(0, capacity));
    }
    public static boolean canRegenerate(boolean naturalRegeneration, boolean hurt, int foodLevel) {
        return naturalRegeneration && hurt && foodLevel >= 18;
    }
    /** Fractional ticks preserve small bonuses instead of rounding a 10-tick interval. */
    public static double regenerationBonus(double speed) { return Math.max(0, speed - 1); }
    public static int wholeBonusTicks(double remainder) { return (int) Math.floor(remainder + 1e-9); }
}
