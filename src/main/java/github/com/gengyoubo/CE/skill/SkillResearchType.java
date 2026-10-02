package github.com.gengyoubo.CE.skill;

/** Optional, data-driven research requirement; existing learned skills retain their effects. */
public enum SkillResearchType {
    NONE, RACE, CORE;
    public static SkillResearchType parse(String value) {
        return switch(value) {
            case "none" -> NONE;
            case "race" -> RACE;
            case "core" -> CORE;
            default -> throw new IllegalArgumentException("Unknown skill research type: "+value);
        };
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean requiresStation() { return this!=NONE; }
    public boolean canLearn(boolean learned,boolean researched) { return !requiresStation() || learned || researched; }
}
