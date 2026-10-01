package github.com.gengyoubo.CE.client;

import java.util.Set;

/** Mutually exclusive species share canvas space without sharing learning records. */
public final class SkillCanvasLayer {
    private SkillCanvasLayer() { }

    public static boolean visible(String nodeType, String viewedType) {
        return nodeType.isEmpty() || nodeType.equals("any") || nodeType.equals(viewedType);
    }

    public static String select(String previousActual, String actual, String selected, Set<String> available) {
        if (!actual.equals(previousActual) && available.contains(actual)) return actual;
        if (available.contains(selected)) return selected;
        if (available.contains(actual)) return actual;
        return available.stream().sorted().findFirst().orElse("any");
    }
}
