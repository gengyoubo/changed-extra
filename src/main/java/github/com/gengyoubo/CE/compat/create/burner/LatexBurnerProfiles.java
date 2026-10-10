package github.com.gengyoubo.CE.compat.create.burner;

import com.google.gson.*;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class LatexBurnerProfiles extends SimpleJsonResourceReloadListener {
    private static final JsonObject BUNDLED_ENTITIES = bundledEntities();
    private static final Map<String, LatexBurnerRules.Profile> BUILT_INS = builtIns(baseDefaults());
    private static volatile Map<String, LatexBurnerRules.Profile> entities = BUILT_INS;
    private static volatile Map<String, LatexBurnerRules.Profile> defaults = baseDefaults();

    public LatexBurnerProfiles() { super(new Gson(), "latex_burner"); }
    private static Map<String, LatexBurnerRules.Profile> baseDefaults() {
        return Map.of("dark", new LatexBurnerRules.Profile("dark", 4, 2400),
                "white", new LatexBurnerRules.Profile("white", 4, 3600));
    }
    private static JsonObject bundledEntities() {
        try (var stream = LatexBurnerProfiles.class.getResourceAsStream("/data/changede/latex_burner/profiles.json")) {
            if (stream != null) {
                JsonObject root = JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                return root.getAsJsonObject("entities");
            }
        } catch (Exception error) { changede.LOGGER.error("Unable to read bundled latex burner profiles", error); }
        return new JsonObject();
    }
    private static Map<String, LatexBurnerRules.Profile> builtIns(Map<String, LatexBurnerRules.Profile> fallback) {
        Map<String, LatexBurnerRules.Profile> result = new HashMap<>();
        BUNDLED_ENTITIES.entrySet().forEach(entry -> {
            JsonObject object = entry.getValue().getAsJsonObject();
            result.put(entry.getKey(), parse(object, object.get("latex").getAsString(), fallback));
        });
        return Map.copyOf(result);
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, LatexBurnerRules.Profile> nextDefaults = new HashMap<>(baseDefaults());
        Map<ResourceLocation, JsonObject> roots = new java.util.TreeMap<>();
        // Deterministic ordering also permits separate data-pack files to override individual species.
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject root = entry.getValue().getAsJsonObject();
                roots.put(entry.getKey(), root);
                if (root.has("defaults")) root.getAsJsonObject("defaults").entrySet().forEach(value -> {
                    try { nextDefaults.put(value.getKey(), parse(value.getValue().getAsJsonObject(), value.getKey(), nextDefaults)); }
                    catch (RuntimeException error) { changede.LOGGER.warn("Invalid latex burner default {} in {}", value.getKey(), entry.getKey(), error); }
                });
            } catch (RuntimeException error) { changede.LOGGER.warn("Invalid latex burner data {}", entry.getKey(), error); }
        });
        // Species without an explicit duration inherit the final defaults, irrespective of file order.
        Map<String, LatexBurnerRules.Profile> nextEntities = new HashMap<>(builtIns(nextDefaults));
        roots.forEach((id, root) -> {
            try {
                if (root.has("entities")) root.getAsJsonObject("entities").entrySet().forEach(value -> {
                    try {
                        if (ResourceLocation.tryParse(value.getKey()) == null) throw new IllegalArgumentException("Invalid entity id");
                        JsonObject object = value.getValue().getAsJsonObject();
                        nextEntities.put(value.getKey(), parse(object, object.get("latex").getAsString(), nextDefaults));
                    } catch (RuntimeException error) { changede.LOGGER.warn("Invalid latex burner species {} in {}", value.getKey(), id, error); }
                });
            } catch (RuntimeException error) { changede.LOGGER.warn("Invalid latex burner data {}", id, error); }
        });
        defaults = Map.copyOf(nextDefaults);
        entities = Map.copyOf(nextEntities);
    }
    private static LatexBurnerRules.Profile parse(JsonObject object, String latex, Map<String, LatexBurnerRules.Profile> fallback) {
        LatexBurnerRules.Profile base = fallback.get(latex);
        if (base == null) throw new IllegalArgumentException("Unknown latex group");
        double speed = object.has("speed") ? object.get("speed").getAsDouble() : base.speed();
        if (!Double.isFinite(speed) || speed * 4 != Math.rint(speed * 4)) throw new IllegalArgumentException("Speed must be a multiple of 0.25");
        double fuelTicks = object.has("fuel_ticks") ? object.get("fuel_ticks").getAsDouble() : base.fuelTicks();
        if (!Double.isFinite(fuelTicks) || fuelTicks != Math.rint(fuelTicks) || fuelTicks < 1 || fuelTicks > 72000)
            throw new IllegalArgumentException("Fuel ticks must be an integer between 1 and 72000");
        return new LatexBurnerRules.Profile(latex, (int) (speed * 4), (int) fuelTicks);
    }
    public static String runtimeLatex(ChangedEntity entity) {
        if (entity.getLatexType() == ChangedLatexTypes.DARK_LATEX.get()) return "dark";
        if (entity.getLatexType() == ChangedLatexTypes.WHITE_LATEX.get()) return "white";
        return "none";
    }
    public static LatexBurnerRules.Profile find(String entityId, String runtimeLatex) {
        if (entityId.isEmpty() || ResourceLocation.tryParse(entityId) == null) return null;
        return entities.getOrDefault(entityId, defaults.get(runtimeLatex));
    }
    public static LatexBurnerRules.Profile find(ChangedEntity entity) {
        return find(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())), runtimeLatex(entity));
    }
}
