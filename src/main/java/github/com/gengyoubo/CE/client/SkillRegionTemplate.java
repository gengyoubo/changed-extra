package github.com.gengyoubo.CE.client;

import java.util.Map;
import java.util.Set;

/** Branch-relative resource-pack styling, resolved against visible world bounds. */
public record SkillRegionTemplate(String id, String branch, String theme, double feather, int priority,
                                  double padding, Map<String,String> themesByType, Set<String> latexTypes) {
    public SkillRegionTemplate {
        if (branch.isBlank() || !Double.isFinite(padding) || padding<=0 || padding>10)
            throw new IllegalArgumentException("Invalid branch region " + id);
        themesByType=Map.copyOf(themesByType); latexTypes=Set.copyOf(latexTypes);
        // Reuse the geometry/style validation shared by legacy absolute regions.
        new SkillRegion(id,0,0,1,1,theme,feather,priority,themesByType,latexTypes);
    }
    public SkillRegion resolve(SkillBranchLayout.Bounds bounds) {
        var padded=bounds.padded(padding);
        return new SkillRegion(id,padded.minX(),padded.minY(),padded.width(),padded.height(),
                theme,feather,priority,themesByType,latexTypes);
    }
}
