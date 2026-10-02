import github.com.gengyoubo.CE.skill.SkillResearchType;

public class SkillResearchTypeTest {
    static void check(boolean value) { if(!value)throw new AssertionError(); }
    public static void main(String[] args) {
        for(var type:SkillResearchType.values()) {
            check(SkillResearchType.parse(type.id())==type);
            check(type.canLearn(false,true));
            check(type.canLearn(true,false)); // Learned effects survive leaving/breaking the station.
            check(type.canLearn(false,false)==(type==SkillResearchType.NONE));
        }
        for(String invalid:new String[]{"CORE","unknown",""}) {
            try { SkillResearchType.parse(invalid);throw new AssertionError(); }
            catch(IllegalArgumentException expected) { }
        }
        System.out.println("PASS: normal learning, race/core research qualification, learned persistence and invalid research rejection.");
    }
}
