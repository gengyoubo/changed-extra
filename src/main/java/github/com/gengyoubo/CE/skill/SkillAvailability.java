package github.com.gengyoubo.CE.skill;

import java.util.List;

/** Learning history, current effects and purchasing are deliberately separate. */
public record SkillAvailability(boolean unlocked, boolean active, boolean purchasable, List<SkillBlockReason> reasons) {
    public SkillAvailability { reasons = List.copyOf(reasons); }
    public static SkillAvailability of(boolean unlocked, boolean active, List<SkillBlockReason> reasons) {
        return new SkillAvailability(unlocked, active, !unlocked && reasons.isEmpty(), reasons);
    }
}
