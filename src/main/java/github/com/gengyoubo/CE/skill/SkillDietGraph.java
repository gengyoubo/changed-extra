package github.com.gengyoubo.CE.skill;

import java.util.*;

/** Source diets propagate through candidates; vegetarian meals require every candidate and producer. */
public final class SkillDietGraph {
    public record Recipe<T>(T output, Set<T> ingredients) {
        public Recipe { ingredients = Set.copyOf(ingredients); }
    }
    public record VegetarianRecipe<T>(T output, List<Set<T>> ingredients) {
        public VegetarianRecipe { ingredients = ingredients.stream().map(Set::copyOf).toList(); }
    }
    private SkillDietGraph() { }
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
    public static <T> Set<T> inferVegetarian(Set<T> seeds, Collection<VegetarianRecipe<T>> recipes,
                                            Set<T> excluded, Set<T> animalFoods, Set<T> neutral) {
        Set<T> blocked = new HashSet<>(excluded);
        blocked.addAll(animalFoods);
        blocked.addAll(neutral);
        Set<T> trusted = new HashSet<>(seeds);
        trusted.removeAll(blocked);
        Map<T, List<VegetarianRecipe<T>>> producers = new HashMap<>();
        Map<T, Set<T>> dependents = new HashMap<>();
        for (var recipe : recipes) {
            producers.computeIfAbsent(recipe.output(), unused -> new ArrayList<>()).add(recipe);
            for (var slot : recipe.ingredients()) for (T input : slot)
                dependents.computeIfAbsent(input, unused -> new HashSet<>()).add(recipe.output());
        }
        // First find seed-backed proofs; a cycle alone cannot create a vegetarian classification.
        Set<T> candidates = new HashSet<>(trusted);
        ArrayDeque<VegetarianRecipe<T>> pending = new ArrayDeque<>(recipes);
        while (!pending.isEmpty()) {
            var recipe = pending.removeFirst();
            if (blocked.contains(recipe.output()) || candidates.contains(recipe.output())
                    || !vegetarianRecipe(recipe, candidates, neutral)) continue;
            candidates.add(recipe.output());
            for (T output : dependents.getOrDefault(recipe.output(), Set.of()))
                pending.addAll(producers.get(output));
        }
        // A second unknown/mixed producer invalidates its output and every derived proof.
        ArrayDeque<T> outputs = new ArrayDeque<>(producers.keySet());
        Set<T> queued = new HashSet<>(producers.keySet());
        while (!outputs.isEmpty()) {
            T output = outputs.removeFirst();
            queued.remove(output);
            if (trusted.contains(output) || !candidates.contains(output)
                    || producers.get(output).stream().allMatch(recipe -> vegetarianRecipe(recipe, candidates, neutral))) continue;
            candidates.remove(output);
            for (T dependent : dependents.getOrDefault(output, Set.of()))
                if (queued.add(dependent)) outputs.addLast(dependent);
        }
        return Set.copyOf(candidates);
    }
    private static <T> boolean vegetarianRecipe(VegetarianRecipe<T> recipe, Set<T> foods, Set<T> neutral) {
        if (recipe.ingredients().isEmpty()) return false;
        boolean food = false;
        for (var slot : recipe.ingredients()) {
            if (slot.isEmpty()) return false;
            for (T candidate : slot) {
                if (neutral.contains(candidate)) continue;
                if (!foods.contains(candidate)) return false;
                food = true;
            }
        }
        return food;
    }
}
