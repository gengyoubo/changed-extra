package github.com.gengyoubo.CE.compat.jer;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import github.com.gengyoubo.CE.worldgen.MorphicCrystalOreFeature;
import github.com.gengyoubo.CE.worldgen.TwilightOreFeature;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Only active biome features with understood feature/placement conditions are exposed. */
public final class OreGenerationScanner {
    private static final int SAMPLES = 8192;
    private OreGenerationScanner() {}

    public static List<OreGenerationData> scan(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Ore scans must run on the server thread");
        List<OreGenerationData> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            // The list includes Forge's applied biome modifiers, not merely registry declarations.
            Set<PlacedFeature> active = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().forEach(biome ->
                    biome.value().getGenerationSettings().features().forEach(features ->
                            features.stream().map(Holder::value).forEach(active::add)));
            Map<BlockState, float[]> curves = new LinkedHashMap<>();
            for (PlacedFeature placed : active) {
                var configured = placed.feature().value();
                if (!(configured.config() instanceof OreConfiguration ore)
                        || ore.size <= 0 || !allowedFeature(configured.feature(), level)) continue;
                float[] curve = distribution(placed, ore.size, level);
                if (curve == null) continue; // Unknown/disabled placement conditions are never guessed.
                // Duplicate target rules in a feature do not create a second generation attempt.
                Set<BlockState> targets = new LinkedHashSet<>();
                ore.targetStates.forEach(target -> targets.add(target.state));
                for (BlockState state : targets) {
                    var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (key == null || "minecraft".equals(key.getNamespace()) || state.getBlock().asItem() == Items.AIR) continue;
                    float[] sum = curves.computeIfAbsent(state, unused -> new float[OreGenerationData.HEIGHTS]);
                    for (int y = 0; y < sum.length; y++) sum[y] += curve[y];
                }
            }
            curves.forEach((state, curve) -> result.add(new OreGenerationData(
                    ForgeRegistries.BLOCKS.getKey(state.getBlock()), level.dimension().location(), curve, drops(state, level))));
        }
        return List.copyOf(result);
    }

    private static boolean allowedFeature(Feature<?> feature, ServerLevel level) {
        if (feature == Feature.ORE || feature == Feature.SCATTERED_ORE) return true;
        if (feature instanceof MorphicCrystalOreFeature) return level.dimension().location().equals(ResourceLocation.parse("changede:latex_space"));
        if (feature instanceof TwilightOreFeature) return level.dimension().location().equals(ResourceLocation.parse("twilightforest:twilight_forest"));
        // These Addon implementations have additional dimension/game-rule gates beyond OreConfiguration.
        return switch (feature.getClass().getName()) {
            case "net.foxyas.changedaddon.world.features.ores.IridiumoreFeature" -> level.dimension().equals(Level.OVERWORLD);
            case "net.foxyas.changedaddon.world.features.ores.PainiteOreFeature" -> level.dimension().equals(Level.OVERWORLD) && painiteEnabled(level);
            default -> false;
        };
    }

    public static boolean painiteEnabled(ServerLevel level) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) return false;
        return level.getGameRules().getBoolean(net.foxyas.changedaddon.init.ChangedAddonGameRules.PAINITE_GENERATION);
    }

    public static boolean createEnabled() {
        return !net.minecraftforge.fml.ModList.get().isLoaded("create")
                || !com.simibubi.create.infrastructure.config.AllConfigs.common().worldGen.disable.get();
    }

    /** Reads modifiers through their codecs; never executes worldgen or loads chunks. */
    static float[] distribution(PlacedFeature placed, int size, ServerLevel level) {
        HeightProvider heights = null;
        double attempts = 1;
        RandomSource random = RandomSource.create(0xCE0E1234L);
        for (PlacementModifier modifier : placed.placement()) {
            var encoded = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier).result();
            if (encoded.isEmpty() || !encoded.get().isJsonObject()) return null;
            JsonObject json = encoded.get().getAsJsonObject();
            switch (json.get("type").getAsString()) {
                case "minecraft:count" -> {
                    var count = IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("count")).result();
                    if (count.isEmpty()) return null;
                    double average = 0;
                    for (int i = 0; i < SAMPLES; i++) average += count.get().sample(random);
                    attempts *= average / SAMPLES;
                }
                case "minecraft:rarity_filter" -> {
                    int chance = json.get("chance").getAsInt();
                    if (chance <= 0) return null;
                    attempts /= chance;
                }
                case "minecraft:height_range" -> {
                    if (heights != null) return null;
                    heights = HeightProvider.CODEC.parse(JsonOps.INSTANCE, json.get("height")).result().orElse(null);
                    if (heights == null) return null;
                }
                case "minecraft:in_square", "minecraft:biome" -> { }
                case "create:config_filter" -> { if (!createEnabled()) return null; }
                default -> { return null; }
            }
        }
        if (heights == null || attempts <= 0) return null;
        var context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
        float[] curve = new float[OreGenerationData.HEIGHTS];
        float weight = (float) (size * attempts / (256 * SAMPLES));
        for (int i = 0; i < SAMPLES; i++) {
            int height = heights.sample(random, context);
            int index = height - OreGenerationData.MIN_Y;
            // Outside the dimension's build height cannot generate. JER cannot plot outside [-64,255].
            if (index >= 0 && index < curve.length && height >= level.getMinBuildHeight() && height < level.getMaxBuildHeight()) {
                curve[index] += weight;
            }
        }
        for (float chance : curve) if (chance > 0) return curve;
        return null;
    }

    private static List<OreGenerationData.Drop> drops(BlockState state, ServerLevel level) {
        // Use real, reloadable block loot tables rather than guessing a raw item from its name.
        var table = level.getServer().getLootData().getLootTable(state.getBlock().getLootTable());
        var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.ORIGIN, new Vec3(0, level.getMinBuildHeight() + 64, 0))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE))
                .create(LootContextParamSets.BLOCK);
        Map<net.minecraft.world.item.Item, int[]> counts = new LinkedHashMap<>();
        for (int seed = 1; seed <= 32; seed++) {
            Map<net.minecraft.world.item.Item, Integer> sample = new LinkedHashMap<>();
            table.getRandomItems(params, seed).forEach(stack -> sample.merge(stack.getItem(), stack.getCount(), Integer::sum));
            for (var entry : sample.entrySet()) {
                int[] range = counts.computeIfAbsent(entry.getKey(), unused -> new int[]{Integer.MAX_VALUE, 0});
                range[0] = Math.min(range[0], entry.getValue());
                range[1] = Math.max(range[1], entry.getValue());
            }
        }
        return counts.entrySet().stream().map(entry -> new OreGenerationData.Drop(
                new ItemStack(entry.getKey()), entry.getValue()[0], entry.getValue()[1])).toList();
    }
}
