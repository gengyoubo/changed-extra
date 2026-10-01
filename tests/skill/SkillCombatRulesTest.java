import github.com.gengyoubo.CE.skill.SkillCombatRules;

public class SkillCombatRulesTest {
    static void near(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-5) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        // A point of damage is a flat point, independent of original hit strength.
        near(SkillCombatRules.outgoing(10, 0, 1, 0), 11);
        near(SkillCombatRules.outgoing(2, 0, 1, 0), 3);
        near(SkillCombatRules.outgoing(10, 0, 3, 4), 17);
        near(SkillCombatRules.outgoing(10, 0, 0, 4), 14);
        near(SkillCombatRules.outgoing(10, .2, 3, 4), 19);
        near(SkillCombatRules.outgoing(10, 0, 0, 0), 10);
        // Repeated resistances add: two 10% nodes reduce a 10-point hit to 8.
        near(SkillCombatRules.incoming(10, .1, 0), 9);
        near(SkillCombatRules.incoming(10, .2, 0), 8);
        near(SkillCombatRules.incoming(10, 0, .1), 9);
        near(SkillCombatRules.incoming(10, 0, 0), 10);
        near(SkillCombatRules.incoming(10, .2, .1), 7);
        near(SkillCombatRules.incoming(10, 2, 0), 0);
        // Equipment bonuses disappear independently as items are removed.
        near(SkillCombatRules.equipmentArmor(false, false, 2, 3), 0);
        near(SkillCombatRules.equipmentArmor(true, false, 2, 3), 2);
        near(SkillCombatRules.equipmentArmor(false, true, 2, 3), 3);
        near(SkillCombatRules.equipmentArmor(true, true, 2, 3), 5);
        near(SkillCombatRules.equipmentArmor(true, true, 0, 0), 0);
        System.out.println("SkillCombatRules tests passed: flat damage, additive resistance and conditional armor.");
    }
}
