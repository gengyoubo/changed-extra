import github.com.gengyoubo.CE.skill.SkillGraph;
import java.util.*;

/** Run with javac/java; no Minecraft client, Forge bootstrap or JUnit dependency needed. */
public class SkillGraphTest {
    record Node(String id, List<String> parents) { }
    static Node node(String id, String... parents) { return new Node(id, List.of(parents)); }
    static List<Node> order(List<Node> nodes) { return SkillGraph.order(nodes, Node::id, Node::parents); }
    static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    static void rejects(List<Node> nodes) {
        try { order(nodes); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected graph rejection");
    }
    public static void main(String[] args) {
        // Reversed input, cross-file junctions, two forks and an optional side route.
        List<Node> nodes = order(List.of(node("landing", "yufeng"), node("flight", "yufeng"),
                node("yufeng", "growth2"), node("growth2", "growth1"), node("counter", "dark"),
                node("growth1", "dark"), node("dark", "defense"), node("defense", "speed"),
                node("speed", "health"), node("health", "root"), node("root")));
        Set<String> learned = new HashSet<>();
        nodes.forEach(n -> learned.add(n.id()));
        Set<String> enabled = SkillGraph.enabled(nodes, Node::id, Node::parents, learned);
        check(enabled.equals(learned));
        learned.remove("counter");
        check(SkillGraph.enabled(nodes, Node::id, Node::parents, learned).contains("flight"));
        learned.remove("dark");
        enabled = SkillGraph.enabled(nodes, Node::id, Node::parents, learned);
        check(!enabled.contains("yufeng") && !enabled.contains("flight") && enabled.contains("defense"));
        // A learned but no longer applicable parent disables its whole descendant chain.
        List<Node> filtered = nodes.stream().filter(n -> !n.id().equals("growth1")).toList();
        learned.add("dark");
        check(!SkillGraph.enabled(filtered, Node::id, Node::parents, learned).contains("landing"));
        List<Node> deep = new ArrayList<>();
        for (int i = 0; i < 2048; i++) deep.add(i == 0 ? node("0") : node("" + i, "" + (i - 1)));
        Collections.reverse(deep);
        check(order(deep).get(2047).id().equals("2047"));
        rejects(List.of(node("a", "b"), node("b", "a")));
        rejects(List.of(node("a", "a")));
        rejects(List.of(node("a", "missing")));
        rejects(List.of(node("a"), node("a")));
        rejects(List.of(node("a"), node("b", "a", "a")));
        check(order(List.of()).isEmpty());
        System.out.println("SkillGraph tests passed: deep chains, forks, inactive prerequisites and invalid graphs.");
    }
}
