package github.com.gengyoubo.CE.skill;

/** Available XP is derived from the current level/bar, never the lifetime totalExperience field. */
public final class SkillExperience {
    private SkillExperience() { }
    public static int toNextLevel(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }
    public static int points(int level, float progress) {
        double base = level >= 32 ? 4.5 * level * level - 162.5 * level + 2220
                : level >= 17 ? 2.5 * level * level - 40.5 * level + 360 : (double) level * level + 6.0 * level;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, base + Math.round(progress * toNextLevel(level))));
    }
}
