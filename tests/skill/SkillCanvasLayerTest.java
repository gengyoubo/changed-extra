import github.com.gengyoubo.CE.client.SkillCanvasLayer;
import github.com.gengyoubo.CE.client.SkillRegion;
import github.com.gengyoubo.CE.client.SkillRegionBlend;
import java.util.*;

public class SkillCanvasLayerTest {
    static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    static void near(double actual, double expected) { check(Math.abs(actual - expected) < 1e-6); }
    public static void main(String[] args) {
        check(SkillCanvasLayer.visibleApplicable(true,false,false));
        check(SkillCanvasLayer.visibleApplicable(false,true,false));
        check(!SkillCanvasLayer.visibleApplicable(false,false,false));
        check(SkillCanvasLayer.visibleApplicable(false,false,true));
        Set<String> choices = Set.of("dark", "white");
        check(SkillCanvasLayer.select("", "white", "any", choices).equals("white"));
        check(SkillCanvasLayer.select("dark", "dark", "white", choices).equals("white")); // Keep preview across snapshots.
        check(SkillCanvasLayer.select("dark", "white", "dark", choices).equals("white")); // Follow a real species change.
        check(SkillCanvasLayer.select("white", "any", "white", choices).equals("white")); // Human form retains viewed history.
        check(SkillCanvasLayer.select("any", "any", "white", Set.of("dark")).equals("dark")); // Removed layer.
        check(SkillCanvasLayer.select("any", "any", "dark", Set.of()).equals("any"));
        check(SkillCanvasLayer.visible("any", "dark"));
        check(SkillCanvasLayer.visible("any", "white"));
        check(SkillCanvasLayer.visible("dark", "dark"));
        check(!SkillCanvasLayer.visible("white", "dark"));
        check(!SkillCanvasLayer.visible("dark", "white"));
        SkillRegion shared = new SkillRegion("group", -4, 10.5, 4.5, 16.5, "dark", .8, 10,
                Map.of("dark", "dark", "white", "white"), Set.of());
        SkillRegion special = new SkillRegion("yufeng", -5.5, 16.5, 4, 6, "wings", 1, 20,
                Map.of(), Set.of("dark"));
        for (String type : choices) {
            var weights = SkillRegionBlend.weights(List.of(shared, special), "lab", -1, 12, type);
            near(weights.get(type), 1);
            near(weights.values().stream().mapToDouble(Double::doubleValue).sum(), 1);
            var border = SkillRegionBlend.weights(List.of(shared, special), "lab", -1, 10.5, type);
            near(border.get("lab"), .5);
            near(border.get(type), .5);
        }
        var dark = SkillRegionBlend.weights(List.of(shared, special), "lab", -3, 18, "dark");
        near(dark.get("wings"), 1);
        var white = SkillRegionBlend.weights(List.of(shared, special), "lab", -3, 18, "white");
        check(!white.containsKey("wings"));
        near(white.get("white"), 1);
        System.out.println("SkillCanvasLayer tests passed: shared slots, preview persistence, species changes and regional theme variants.");
    }
}
