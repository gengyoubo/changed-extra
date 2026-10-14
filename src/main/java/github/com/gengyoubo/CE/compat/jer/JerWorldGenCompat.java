package github.com.gengyoubo.CE.compat.jer;

import jeresources.api.distributions.DistributionBase;
import jeresources.api.drop.LootDrop;
import jeresources.api.restrictions.BiomeRestriction;
import jeresources.api.restrictions.DimensionRestriction;
import jeresources.api.restrictions.Restriction;
import jeresources.entry.WorldGenEntry;
import jeresources.jei.JEIConfig;
import jeresources.jei.worldgen.WorldGenWrapper;
import jeresources.registry.WorldGenRegistry;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Adds only server-proven recipes; replaces this session's recipes instead of merging distributions twice. */
public final class JerWorldGenCompat {
    private static IJeiRuntime runtime;
    private static List<WorldGenWrapper> recipes = List.of();
    private static List<OreGenerationData> displayed = List.of();
    private JerWorldGenCompat() {}

    public static void register(IRecipeRegistration registration) {
        runtime = null;
        displayed = JerOreClientState.snapshot();
        recipes = recipes(displayed);
        registration.addRecipes(JEIConfig.WORLD_GEN_TYPE, recipes);
    }

    public static void runtimeAvailable(IJeiRuntime value) { runtime = value; refresh(); }

    public static void refresh() {
        if (runtime == null || displayed == JerOreClientState.snapshot()) return;
        runtime.getRecipeManager().hideRecipes(JEIConfig.WORLD_GEN_TYPE, recipes);
        displayed = JerOreClientState.snapshot();
        recipes = recipes(displayed);
        runtime.getRecipeManager().addRecipes(JEIConfig.WORLD_GEN_TYPE, recipes);
    }

    public static void clear() { runtime = null; displayed = List.of(); recipes = List.of(); }

    private static List<WorldGenWrapper> recipes(List<OreGenerationData> entries) {
        List<WorldGenWrapper> result = new ArrayList<>();
        var existing = WorldGenRegistry.getInstance().getWorldGen();
        for (var data : entries) {
            var block = ForgeRegistries.BLOCKS.getValue(data.block());
            if (block == null || block == Blocks.AIR || data.biomes().isEmpty()) continue;
            // Preserve an explicit native adapter where it already supplies this block/dimension.
            if (existing.stream().anyMatch(entry -> entry.getBlock().getItem() == block.asItem()
                    && (entry.getDimension().equals("all") || entry.getDimension().equals(data.dimension().toString())))) continue;
            var biomeNames = data.biomes().stream().map(key -> "biome." + key.getNamespace() + "." + key.getPath().replace('/', '.')).toList();
            var biomes = new BiomeRestriction() {
                @Override public List<String> toStringList() { return biomeNames; }
            };
            var restriction = new Restriction(biomes, new DimensionRestriction(ResourceKey.create(Registries.DIMENSION, data.dimension())));
            var distribution = new DistributionBase(data.distribution().clone()) {
                { int best = 0; for (int i = 1; i < getDistribution().length; i++) if (getDistribution()[i] > getDistribution()[best]) best = i;
                    bestHeight = best + OreGenerationData.MIN_Y; }
            };
            LootDrop[] drops = data.drops().stream().map(drop -> new LootDrop(drop.stack(), drop.min(), drop.max())).toArray(LootDrop[]::new);
            result.add(new WorldGenWrapper(new WorldGenEntry(new ItemStack(block), distribution, restriction, true, drops)));
        }
        return List.copyOf(result);
    }
}
