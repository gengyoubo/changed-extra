package github.com.gengyoubo.CE.skill;

/** Pure arithmetic shared by runtime and regression tests. Values are final route values. */
public final class SkillMechanicRules {
    private SkillMechanicRules() { }
    public static double incoming(double amount, double waterGuard, double blastGuard, double firePenalty, double netherPenalty) {
        double remaining = Math.max(.2, (1 - waterGuard) * (1 - blastGuard));
        return amount * remaining * (1 + Math.max(firePenalty, netherPenalty));
    }
    public static int virtualEnchantment(int existing, int extra) { return existing >= 3 ? existing : Math.min(3, existing + extra); }
    public static double flightMultiplier(int armorSlots, double adaptation) { return 1 - Math.min(4,armorSlots)*.05*(1-adaptation); }
    public static double flightExhaustion(double base, double removed) { return base*(1+Math.max(0,.5-removed)); }
    public static float rescueHealth(float maximum) { return Math.min(maximum, 4); }
}
