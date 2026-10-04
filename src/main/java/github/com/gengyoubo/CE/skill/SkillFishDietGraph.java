package github.com.gengyoubo.CE.skill;

import java.util.*;

/** Fish origins propagate through recipe candidates and intermediates; unseeded cycles stay empty. */
public final class SkillFishDietGraph {
    public record Recipe<T>(T output, Set<T> ingredients) {
        public Recipe { ingredients = Set.copyOf(ingredients); }
    }
    private SkillFishDietGraph() { }
    public static <T> Set<T> infer(Set<T> seeds, Collection<Recipe<T>> recipes, Set<T> excluded) {
        Map<T, Set<T>> outputs = new HashMap<>();
        for (Recipe<T> recipe : recipes) {
            if (excluded.contains(recipe.output())) continue;
            for (T ingredient : recipe.ingredients())
                outputs.computeIfAbsent(ingredient, unused -> new HashSet<>()).add(recipe.output());
        }
        Set<T> result = new HashSet<>(seeds);
        result.removeAll(excluded);
        ArrayDeque<T> pending = new ArrayDeque<>(result);
        while (!pending.isEmpty()) {
            for (T output : outputs.getOrDefault(pending.removeFirst(), Set.of()))
                if (result.add(output)) pending.addLast(output);
        }
        return Set.copyOf(result);
    }
}
