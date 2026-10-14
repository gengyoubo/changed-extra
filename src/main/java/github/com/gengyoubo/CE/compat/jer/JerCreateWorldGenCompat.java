package github.com.gengyoubo.CE.compat.jer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Create 6 moved zinc generation from worldgen.v2 config entries to datapack features. */
public final class JerCreateWorldGenCompat {
    private JerCreateWorldGenCompat() {}

    public static Map<String, Number> zincOreValues() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        JsonObject placed;
        int size;
        int minY = -64;
        int height = 384;
        if (server != null) {
            var access = server.registryAccess();
            var feature = access.registryOrThrow(Registries.PLACED_FEATURE)
                    .get(ResourceLocation.fromNamespaceAndPath("create", "zinc_ore"));
            if (feature == null || !(feature.feature().value().config() instanceof OreConfiguration ore)) {
                throw new IllegalStateException("Missing Create zinc ore feature");
            }
            size = ore.size;
            placed = PlacedFeature.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, access), feature)
                    .getOrThrow(false, message -> {}).getAsJsonObject();
            minY = server.overworld().getMinBuildHeight();
            height = server.overworld().getHeight();
        } else {
            // Dedicated servers do not sync placed/configured features. Use Create's packaged defaults,
            // just as JER's vanilla ore charts do, rather than inventing old config values.
            size = readFeature("configured_feature").getAsJsonObject("config").get("size").getAsInt();
            placed = readFeature("placed_feature");
        }
        Integer count = null;
        Integer min = null;
        Integer max = null;
        for (var element : placed.getAsJsonArray("placement")) {
            JsonObject modifier = element.getAsJsonObject();
            String type = modifier.get("type").getAsString();
            if ("minecraft:count".equals(type)) {
                count = modifier.get("count").getAsInt();
            } else if ("minecraft:height_range".equals(type)) {
                var range = modifier.getAsJsonObject("height");
                if (!"minecraft:uniform".equals(range.get("type").getAsString())) {
                    throw new IllegalStateException("JER's square zinc chart requires a uniform height range");
                }
                min = resolveAnchor(range.getAsJsonObject("min_inclusive"), minY, height);
                max = resolveAnchor(range.getAsJsonObject("max_inclusive"), minY, height);
            }
        }
        if (count == null || min == null || max == null || min > max) {
            throw new IllegalStateException("Unsupported Create zinc ore placement: " + placed);
        }
        return Map.of("clusterSize", size, "frequency", count, "minHeight", min, "maxHeight", max);
    }

    private static int resolveAnchor(JsonObject anchor, int minY, int height) {
        if (anchor.has("absolute")) return anchor.get("absolute").getAsInt();
        if (anchor.has("above_bottom")) return minY + anchor.get("above_bottom").getAsInt();
        if (anchor.has("below_top")) return minY + height - 1 - anchor.get("below_top").getAsInt();
        throw new IllegalStateException("Unknown zinc ore height anchor: " + anchor);
    }

    private static JsonObject readFeature(String kind) {
        String path = "/data/create/worldgen/" + kind + "/zinc_ore.json";
        var stream = AllBlocks.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing Create feature resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Create feature resource: " + path, exception);
        }
    }
}
