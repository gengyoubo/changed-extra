import github.com.gengyoubo.CE.skill.SkillMechanicRules;

public class SkillMechanicRulesTest {
    private static void near(double actual,double expected) {
        if(Math.abs(actual-expected)>1e-6)throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        near(SkillMechanicRules.incoming(10,0,0,.25,.15),12.5);
        near(SkillMechanicRules.incoming(10,.3,.5,.25,.15),4.375);
        near(SkillMechanicRules.incoming(10,.9,.9,0,0),2);
        near(SkillMechanicRules.incoming(10,0,0,.125,.075),11.25);
        near(SkillMechanicRules.flightMultiplier(4,0),.8);
        near(SkillMechanicRules.flightMultiplier(4,.5),.9);
        near(SkillMechanicRules.flightMultiplier(4,1),1);
        near(SkillMechanicRules.flightExhaustion(.025,0),.0375);
        near(SkillMechanicRules.flightExhaustion(.025,.25),.03125);
        near(SkillMechanicRules.flightExhaustion(.025,.5),.025);
        near(SkillMechanicRules.rescueHealth(2),2);
        near(SkillMechanicRules.rescueHealth(20),4);
        for(int existing=0;existing<10;existing++) {
            int expected=existing>=3?existing:Math.min(3,existing+1);
            if(SkillMechanicRules.virtualEnchantment(existing,1)!=expected)throw new AssertionError("Virtual enchantment lowered or exceeded the allowed level");
        }
        System.out.println("Skill mechanics: environment overlap, mixed defenses, flight compensation, rescue health and enchantment caps passed.");
    }
}
