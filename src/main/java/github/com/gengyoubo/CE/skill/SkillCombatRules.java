package github.com.gengyoubo.CE.skill;

/** Point bonuses and percentage resistance remain distinct, including repeated skill nodes. */
public final class SkillCombatRules {
    private SkillCombatRules() { }
    public static float outgoing(float damage, double legacyRatio, double targetBonus, double weaponBonus) {
        return (float) (Math.max(0, damage) * (1 + Math.max(0, legacyRatio))
                + Math.max(0, targetBonus) + Math.max(0, weaponBonus));
    }
    public static float incoming(float damage, double latexResistance, double fogResistance) {
        double resistance = Math.min(1, Math.max(0, latexResistance) + Math.max(0, fogResistance));
        return (float) (Math.max(0, damage) * (1 - resistance));
    }
    public static double equipmentArmor(boolean wearingArmor, boolean holdingWeapon, double armorBonus, double weaponBonus) {
        return (wearingArmor ? Math.max(0, armorBonus) : 0) + (holdingWeapon ? Math.max(0, weaponBonus) : 0);
    }
}
