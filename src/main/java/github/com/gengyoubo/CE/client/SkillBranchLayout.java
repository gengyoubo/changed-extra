package github.com.gengyoubo.CE.client;

import java.util.*;

/** Compact only visible branches, keeping each group on its preferred side. */
public final class SkillBranchLayout {
    public enum Side { LEFT, RIGHT }
    public static final double DEFAULT_PADDING = .75, TRUNK_GAP = 1.5, BRANCH_GAP = .75;
    private static final List<String> LEFT = List.of("dark", "biped", "taur", "mer", "land", "feline",
            "dragon", "arthropod", "shark", "fish", "cephalopod", "other_mammal");
    private static final List<String> RIGHT = List.of("white", "feral", "snake", "sea", "air", "canine",
            "reptile", "bird", "ray", "marine_mammal", "plant", "humanoid");
    public record Node(String id, String branch, double x, double y, boolean global) { }
    public record Point(double x, double y) { }
    public record Bounds(double minX, double minY, double maxX, double maxY) {
        public double width() { return maxX - minX; }
        public double height() { return maxY - minY; }
        public Bounds padded(double padding) {
            return new Bounds(minX-padding, minY-padding, maxX+padding, maxY+padding);
        }
        public Bounds shifted(double x) { return new Bounds(minX+x, minY, maxX+x, maxY); }
        public Bounds union(Bounds other) {
            return new Bounds(Math.min(minX,other.minX), Math.min(minY,other.minY),
                    Math.max(maxX,other.maxX), Math.max(maxY,other.maxY));
        }
    }
    public record Layout(Map<String, Point> nodes, Map<String, Bounds> branches, Map<String, Bounds> groups) {
        public Layout { nodes=Map.copyOf(nodes); branches=Map.copyOf(branches); groups=Map.copyOf(groups); }
    }
    private SkillBranchLayout() { }
    private static String name(String branch) { return branch.startsWith("changede:") ? branch.substring(9) : branch; }
    private static String group(String branch) {
        return Set.of("changede:insect","changede:arachnid").contains(branch) ? "changede:arthropod" : branch;
    }
    public static Side preferredSide(String branch, double originalX) {
        String name=name(group(branch));
        return LEFT.contains(name) ? Side.LEFT : RIGHT.contains(name) ? Side.RIGHT : originalX<0 ? Side.LEFT : Side.RIGHT;
    }
    private static int order(String branch, Side side) {
        int index=(side==Side.LEFT ? LEFT : RIGHT).indexOf(name(branch));
        return index<0 ? Integer.MAX_VALUE : index;
    }
    public static Layout arrange(List<Node> visible, Map<String,Double> padding) {
        Map<String,Bounds> branches=new HashMap<>(), groups=new HashMap<>();
        for (Node node:visible) if (!node.global()) {
            Bounds point=new Bounds(node.x(),node.y(),node.x(),node.y());
            branches.merge(node.branch(),point,Bounds::union);
        }
        for (var branch:branches.entrySet()) {
            double pad=padding.getOrDefault(branch.getKey(),DEFAULT_PADDING);
            groups.merge(group(branch.getKey()),branch.getValue().padded(pad),Bounds::union);
        }
        Map<String,Double> shifts=new HashMap<>();
        for (Side side:Side.values()) {
            List<String> ordered=groups.keySet().stream()
                    .filter(g->preferredSide(g,(groups.get(g).minX()+groups.get(g).maxX())/2)==side)
                    .sorted(Comparator.comparingInt((String g)->order(g,side)).thenComparing(g->g)).toList();
            double cursor=side==Side.LEFT ? -TRUNK_GAP : TRUNK_GAP;
            for (String group:ordered) {
                Bounds bounds=groups.get(group);
                double shift=cursor-(side==Side.LEFT ? bounds.maxX() : bounds.minX());
                shifts.put(group,shift);
                cursor+=(side==Side.LEFT ? -1 : 1)*(bounds.width()+BRANCH_GAP);
            }
        }
        Map<String,Point> points=new HashMap<>();
        for (Node node:visible) points.put(node.id(),new Point(node.global() ? 0 : node.x()+shifts.get(group(node.branch())),node.y()));
        branches.replaceAll((branch,bounds)->bounds.shifted(shifts.get(group(branch))));
        groups.replaceAll((group,bounds)->bounds.shifted(shifts.get(group)));
        return new Layout(points,branches,groups);
    }
}
