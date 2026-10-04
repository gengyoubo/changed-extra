import github.com.gengyoubo.CE.skill.SkillNutrition;

public class SkillNutritionTest {
    static void near(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-5) throw new AssertionError(actual + " != " + expected);
    }
    static void check(boolean result) { if (!result) throw new AssertionError(); }
    public static void main(String[] args) {
        near(SkillNutrition.exhaustion(4, 0), 4);
        near(SkillNutrition.exhaustion(4, 0.1), 3.6);
        near(SkillNutrition.exhaustion(4, 0.3), 2.8);
        near(SkillNutrition.exhaustion(4, 2), 0);
        near(SkillNutrition.exhaustion(-4, 0.5), -4);
        near(SkillNutrition.foodSaturation(0.6F, 1), 0.6);
        near(SkillNutrition.foodSaturation(0.6F, 2), 1.2);
        near(SkillNutrition.foodSaturation(0.6F, 4), 2.4);
        // Scaling both vanilla arguments by 1.5 would incorrectly give 2.25x saturation.
        for (int nutrition : new int[] {0, 1, 2, 3, 5, 7, 12}) {
            var meal = SkillNutrition.dietMeal(nutrition, 0.6F);
            near(meal.nutrition(), Math.round(nutrition * 1.5));
            near(meal.nutrition() * meal.saturationModifier() * 2, nutrition * 0.6 * 2 * 1.5);
            near(meal.nutrition() * SkillNutrition.foodSaturation(meal.saturationModifier(), 2) * 2,
                    nutrition * 0.6 * 2 * 1.5 * 2);
        }
        near(SkillNutrition.saturationCap(20, 1), 21);
        near(SkillNutrition.saturationCap(12, 3), 15);
        near(SkillNutrition.saturationCap(20, 0), 20);
        near(SkillNutrition.saturationCap(20, -1), 20);
        check(SkillNutrition.canRegenerate(true, true, 18));
        check(!SkillNutrition.canRegenerate(false, true, 20));
        check(!SkillNutrition.canRegenerate(true, false, 20));
        check(!SkillNutrition.canRegenerate(true, true, 17));
        check(!SkillNutrition.canRegenerate(true, true, 0));
        near(SkillNutrition.regenerationBonus(1), 0);
        near(SkillNutrition.regenerationBonus(1.2), 0.2);
        double remainder = 0;
        int extra = 0;
        for (int tick = 0; tick < 100; tick++) {
            remainder += SkillNutrition.regenerationBonus(1.2);
            int whole = SkillNutrition.wholeBonusTicks(remainder);
            extra += whole;
            remainder = Math.max(0, remainder - whole);
        }
        near(extra, 20);
        near(remainder, 0);
        System.out.println("SkillNutrition tests passed: default behavior, exhaustion, meal saturation, capacity and natural regeneration.");
    }
}
