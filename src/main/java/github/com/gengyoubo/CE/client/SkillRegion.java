package github.com.gengyoubo.CE.client;

import java.util.Map;
import java.util.Set;

/** Visual coordinates use the same grid as the graph, independent of nodes and form selectors. */
public record SkillRegion(String id, double x, double y, double width, double height,
                          String theme, double feather, int priority,
                          Map<String, String> themesByType, Set<String> latexTypes) {
    public SkillRegion(String id, double x, double y, double width, double height, String theme, double feather, int priority) {
        this(id, x, y, width, height, theme, feather, priority, Map.of(), Set.of());
    }

    public SkillRegion {
        if (id.isBlank() || theme.isBlank() || !Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0
                || !Double.isFinite(feather) || feather <= 0 || feather > 10
                || Math.abs(x) > 10000 || Math.abs(y) > 10000 || width > 20000 || height > 20000)
            throw new IllegalArgumentException("Invalid skill region " + id);
        themesByType = Map.copyOf(themesByType);
        latexTypes = Set.copyOf(latexTypes);
        if (!Set.of("dark", "white").containsAll(latexTypes)
                || !Set.of("dark", "white").containsAll(themesByType.keySet())
                || themesByType.values().stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Invalid skill region species " + id);
    }

    public String theme(String viewedType) { return themesByType.getOrDefault(viewedType, theme); }
    public boolean visible(String viewedType) { return latexTypes.isEmpty() || latexTypes.contains(viewedType); }

    /** Smooth coverage across all four edges, including corners and negative coordinates. */
    public double coverage(double px, double py) {
        double dx = Math.max(Math.max(x - px, px - x - width), 0);
        double dy = Math.max(Math.max(y - py, py - y - height), 0);
        double distance = dx > 0 || dy > 0 ? -Math.hypot(dx, dy)
                : Math.min(Math.min(px - x, x + width - px), Math.min(py - y, y + height - py));
        double t = Math.max(0, Math.min(1, 0.5 + distance / feather));
        return t * t * (3 - 2 * t);
    }
}
