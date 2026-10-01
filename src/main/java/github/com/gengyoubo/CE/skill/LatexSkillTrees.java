package github.com.gengyoubo.CE.skill;

import com.google.gson.*;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Scopes select applicable bonuses; graph depth and key-node count are independent of scope. */
@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSkillTrees extends SimpleJsonResourceReloadListener {
    private static List<Tree> trees = List.of();
    private static List<SkillNode> orderedNodes = List.of();
    private static final Set<String> ATTRIBUTES = Set.of("health", "attack", "armor", "speed");
    private static final Set<String> POWERS = Set.of("none", "health", "attack", "armor", "speed", "fall_resistance",
            "damage_vs_white", "damage_vs_dark", "flight_control", "yufeng_takeoff", "yufeng_boost");

    private record Tree(ResourceLocation id, String scope, String latexType, ResourceLocation entityTag,
                        List<ResourceLocation> forms, List<SkillNode> nodes) {
        boolean matches(Player player) {
            var variant = ProcessTransfur.getPlayerTransfurVariant(player);
            if (variant == null || variant.getLatexType() == ChangedLatexTypes.NONE.get()) return false;
            if (latexType.equals("dark") && variant.getLatexType() != ChangedLatexTypes.DARK_LATEX.get()) return false;
            if (latexType.equals("white") && variant.getLatexType() != ChangedLatexTypes.WHITE_LATEX.get()) return false;
            if (!forms.isEmpty() && !forms.contains(variant.getFormId())) return false;
            return entityTag == null || variant.getChangedEntity().getType().is(TagKey.create(Registries.ENTITY_TYPE, entityTag));
        }
    }

    private LatexSkillTrees() { super(new Gson(), "latex_skill_trees"); }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) { event.addListener(new LatexSkillTrees()); }

    public static List<SkillNode> forPlayer(Player player) {
        Set<ResourceLocation> matching = new HashSet<>();
        trees.stream().filter(t -> t.matches(player)).forEach(t -> matching.add(t.id()));
        return orderedNodes.stream().filter(n -> matching.contains(n.tree())).toList();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<Tree> next = new ArrayList<>();
        try {
            for (var entry : resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                ResourceLocation treeId = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();
                String scope = GsonHelper.getAsString(json, "scope");
                if (!Set.of("global", "group", "form").contains(scope)) throw new IllegalArgumentException("Invalid scope: " + treeId);
                String latexType = GsonHelper.getAsString(json, "latex_type", "any");
                if (!Set.of("any", "dark", "white").contains(latexType)) throw new IllegalArgumentException("Invalid latex_type");
                ResourceLocation tag = json.has("entity_tag") ? parse(json.get("entity_tag").getAsString()) : null;
                List<ResourceLocation> forms = new ArrayList<>();
                for (JsonElement form : GsonHelper.getAsJsonArray(json, "forms", new JsonArray())) forms.add(parse(form.getAsString()));
                if (scope.equals("global") && (!latexType.equals("any") || tag != null || !forms.isEmpty()))
                    throw new IllegalArgumentException("Global trunk must apply to all latex forms");
                if (!scope.equals("global") && latexType.equals("any") && tag == null && forms.isEmpty())
                    throw new IllegalArgumentException("Branches require a selector");
                List<SkillNode> nodes = new ArrayList<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "nodes")) {
                    JsonObject node = element.getAsJsonObject();
                    ResourceLocation id = parse(GsonHelper.getAsString(node, "id"));
                    List<ResourceLocation> parents = new ArrayList<>();
                    for (JsonElement parent : GsonHelper.getAsJsonArray(node, "parents", new JsonArray())) parents.add(parse(parent.getAsString()));
                    int cost = GsonHelper.getAsInt(node, "cost");
                    boolean key = GsonHelper.getAsBoolean(node, "key", false);
                    String power = GsonHelper.getAsString(node, "power", "none");
                    double amount = GsonHelper.getAsDouble(node, "amount", 0);
                    int x = GsonHelper.getAsInt(node, "x"), y = GsonHelper.getAsInt(node, "y");
                    if (cost < 0 || cost > 100 || !POWERS.contains(power)
                            || (scope.equals("global") && !ATTRIBUTES.contains(power) && !power.equals("none"))
                            || (power.equals("none") && amount != 0)
                            || !Double.isFinite(amount) || amount < 0 || amount > 100
                            || (!ATTRIBUTES.contains(power) && amount > 1)
                            || Math.abs((long) x) > 10000 || Math.abs((long) y) > 10000
                            || nodes.stream().anyMatch(n -> n.id().equals(id) || (n.x() == x && n.y() == y)))
                        throw new IllegalArgumentException("Invalid or duplicate skill node: " + id);
                    nodes.add(new SkillNode(treeId, scope, id, GsonHelper.getAsString(node, "title"),
                            GsonHelper.getAsString(node, "description"), cost, List.copyOf(parents), x, y, key, power, amount));
                }
                next.add(new Tree(treeId, scope, latexType, tag, List.copyOf(forms), List.copyOf(nodes)));
            }
            List<SkillNode> nodes = next.stream().flatMap(t -> t.nodes().stream()).toList();
            if (nodes.size() > 2048) throw new IllegalArgumentException("Too many nodes (maximum 2048)");
            List<SkillNode> ordered = SkillGraph.order(nodes, SkillNode::id, SkillNode::parents);
            trees = List.copyOf(next);
            orderedNodes = ordered;
        } catch (RuntimeException ex) {
            changede.LOGGER.error("Latex skill tree reload rejected; retaining previous trees: {}", ex.getMessage());
        }
    }

    private static ResourceLocation parse(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource id " + value);
        return id;
    }

}
