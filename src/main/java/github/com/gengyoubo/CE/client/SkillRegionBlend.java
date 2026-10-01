package github.com.gengyoubo.CE.client;

import java.util.*;

/** Priority compositing lets a form region gradually overlay its surrounding group region. */
public final class SkillRegionBlend {
    private SkillRegionBlend() { }

    public static Map<String, Double> weights(List<SkillRegion> regions, String fallback, double x, double y) {
        return weights(regions, fallback, x, y, "any");
    }

    public static Map<String, Double> weights(List<SkillRegion> regions, String fallback, double x, double y, String viewedType) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put(fallback, 1.0);
        for (SkillRegion region : regions) {
            if (!region.visible(viewedType)) continue;
            double coverage = region.coverage(x, y);
            if (coverage == 0) continue;
            weights.replaceAll((theme, weight) -> weight * (1 - coverage));
            weights.merge(region.theme(viewedType), coverage, Double::sum);
        }
        weights.values().removeIf(weight -> weight < 0.000001);
        return weights;
    }

    public static int color(Map<String, Double> weights, java.util.function.ToIntFunction<String> palette) {
        double red = 0, green = 0, blue = 0;
        for (var layer : weights.entrySet()) {
            int color = palette.applyAsInt(layer.getKey());
            red += ((color >> 16) & 255) * layer.getValue();
            green += ((color >> 8) & 255) * layer.getValue();
            blue += (color & 255) * layer.getValue();
        }
        return 0xFF000000 | ((int) Math.round(red) << 16) | ((int) Math.round(green) << 8) | (int) Math.round(blue);
    }
}
