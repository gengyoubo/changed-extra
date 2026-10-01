package github.com.gengyoubo.CE.skill;

import java.util.*;
import java.util.function.Function;

/** Pure graph operations shared by loading and runtime prerequisite evaluation. */
public final class SkillGraph {
    private SkillGraph() { }

    public static <K, T> List<T> order(List<T> nodes, Function<T, K> id, Function<T, List<K>> parents) {
        Map<K, T> byId = new LinkedHashMap<>();
        Map<K, Integer> incoming = new HashMap<>();
        Map<K, List<K>> children = new HashMap<>();
        for (T node : nodes) {
            K key = id.apply(node);
            if (byId.putIfAbsent(key, node) != null) throw new IllegalArgumentException("Duplicate node " + key);
        }
        for (T node : nodes) {
            K key = id.apply(node);
            List<K> prerequisites = parents.apply(node);
            if (new HashSet<>(prerequisites).size() != prerequisites.size())
                throw new IllegalArgumentException("Duplicate prerequisite " + key);
            incoming.put(key, prerequisites.size());
            for (K parent : prerequisites) {
                if (!byId.containsKey(parent)) throw new IllegalArgumentException("Unknown prerequisite " + parent);
                children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(key);
            }
        }
        ArrayDeque<K> ready = new ArrayDeque<>();
        byId.keySet().stream().filter(k -> incoming.get(k) == 0).forEach(ready::add);
        List<T> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            K key = ready.remove();
            ordered.add(byId.get(key));
            for (K child : children.getOrDefault(key, List.of()))
                if (incoming.compute(child, (ignored, count) -> count - 1) == 0) ready.add(child);
        }
        if (ordered.size() != nodes.size()) throw new IllegalArgumentException("Cyclic prerequisites");
        return List.copyOf(ordered);
    }

    /** Input keeps topological order even after filtering by the current form. */
    public static <K, T> Set<K> enabled(List<T> ordered, Function<T, K> id,
                                      Function<T, List<K>> parents, Set<K> learned) {
        Set<K> enabled = new HashSet<>();
        for (T node : ordered) {
            K key = id.apply(node);
            if (learned.contains(key) && enabled.containsAll(parents.apply(node))) enabled.add(key);
        }
        return enabled;
    }
}
