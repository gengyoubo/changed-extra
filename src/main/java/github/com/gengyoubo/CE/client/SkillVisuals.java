package github.com.gengyoubo.CE.client;

import com.google.gson.*;
import github.com.gengyoubo.CE.changede;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Client resource-pack data; regions never participate in learning or form matching. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SkillVisuals extends SimpleJsonResourceReloadListener {
    public static final String LABORATORY = "changede:laboratory";
    public record Theme(ResourceLocation tile, ResourceLocation stripe, int tint, int active,
                        int available, int dormant, int locked, int surface, String motif) { }
    private static final Theme FALLBACK = new Theme(sprite("changed:block/wall_white"),
            sprite("changed:block/wall_blue_striped"), 0xFFFFFF, 0x74CE99, 0xE6C66B,
            0xA6BAC9, 0x566478, 0x172335, "grid");
    private static Map<String, Theme> themes = Map.of(LABORATORY, FALLBACK);
    private static List<SkillRegion> regions = List.of();

    private SkillVisuals() { super(new Gson(), "latex_skill_visuals"); }
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SkillVisuals());
    }
    public static Theme theme(String id) { return themes.getOrDefault(id, FALLBACK); }
    public static Map<String, Double> weights(double x, double y, String viewedType) {
        return SkillRegionBlend.weights(regions, LABORATORY, x, y, viewedType);
    }
    public static String dominant(Map<String, Double> weights) {
        return weights.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }
    public static int color(Map<String, Double> weights, java.util.function.ToIntFunction<Theme> color) {
        return SkillRegionBlend.color(weights, id -> color.applyAsInt(theme(id)));
    }
    private static ResourceLocation sprite(String id) {
        ResourceLocation result = ResourceLocation.tryParse(id);
        if (result == null) throw new IllegalArgumentException("Invalid visual id " + id);
        return result;
    }
    private static int color(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid color " + field);
        return Integer.parseInt(value.substring(1), 16);
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        try {
            Map<String, Theme> nextThemes = new HashMap<>();
            nextThemes.put(LABORATORY, FALLBACK);
            List<SkillRegion> nextRegions = new ArrayList<>();
            Set<String> regionIds = new HashSet<>(), themeIds = new HashSet<>();
            for (var entry : resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                JsonObject json = entry.getValue().getAsJsonObject();
                for (var element : GsonHelper.getAsJsonArray(json, "themes", new JsonArray())) {
                    JsonObject t = element.getAsJsonObject();
                    String id = sprite(GsonHelper.getAsString(t, "id")).toString();
                    if (!themeIds.add(id)) throw new IllegalArgumentException("Duplicate theme " + id);
                    String motif = GsonHelper.getAsString(t, "motif", "grid");
                    if (!Set.of("grid", "latex", "wings").contains(motif)) throw new IllegalArgumentException("Invalid motif " + motif);
                    nextThemes.put(id, new Theme(sprite(GsonHelper.getAsString(t, "tile")),
                            sprite(GsonHelper.getAsString(t, "stripe", GsonHelper.getAsString(t, "tile"))),
                            color(t, "tint"), color(t, "active"), color(t, "available"), color(t, "dormant"),
                            color(t, "locked"), color(t, "surface"), motif));
                }
                for (var element : GsonHelper.getAsJsonArray(json, "regions", new JsonArray())) {
                    JsonObject r = element.getAsJsonObject();
                    String id = sprite(GsonHelper.getAsString(r, "id")).toString();
                    if (!regionIds.add(id)) throw new IllegalArgumentException("Duplicate region " + id);
                    Map<String, String> variants = new HashMap<>();
                    if (r.has("themes_by_type")) for (var variant : r.getAsJsonObject("themes_by_type").entrySet())
                        variants.put(variant.getKey(), sprite(variant.getValue().getAsString()).toString());
                    Set<String> types = new HashSet<>();
                    for (var type : GsonHelper.getAsJsonArray(r, "latex_types", new JsonArray())) types.add(type.getAsString());
                    nextRegions.add(new SkillRegion(id, GsonHelper.getAsDouble(r, "x"), GsonHelper.getAsDouble(r, "y"),
                            GsonHelper.getAsDouble(r, "width"), GsonHelper.getAsDouble(r, "height"),
                            sprite(GsonHelper.getAsString(r, "theme")).toString(),
                            GsonHelper.getAsDouble(r, "feather", 1), GsonHelper.getAsInt(r, "priority", 0), variants, types));
                }
            }
            if (nextRegions.size() > 256 || nextThemes.size() > 64) throw new IllegalArgumentException("Too many skill visuals");
            for (SkillRegion region : nextRegions) {
                if (!nextThemes.containsKey(region.theme())) throw new IllegalArgumentException("Unknown theme " + region.theme());
                for (String variant : region.themesByType().values())
                    if (!nextThemes.containsKey(variant)) throw new IllegalArgumentException("Unknown theme " + variant);
            }
            nextRegions.sort(Comparator.comparingInt(SkillRegion::priority).thenComparing(SkillRegion::id));
            themes = Map.copyOf(nextThemes);
            regions = List.copyOf(nextRegions);
        } catch (RuntimeException ex) {
            changede.LOGGER.error("Skill visual reload rejected; retaining previous visuals: {}", ex.getMessage());
        }
    }
}
