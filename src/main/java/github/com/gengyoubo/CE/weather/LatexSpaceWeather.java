package github.com.gengyoubo.CE.weather;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class LatexSpaceWeather {
    public static final ResourceLocation DIMENSION = ResourceLocation.fromNamespaceAndPath("changede", "latex_space");
    public static final TagKey<Biome> WHITE_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("changede", "white_latex_weather"));
    public static final TagKey<Biome> DARK_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("changede", "dark_latex_weather"));

    private LatexSpaceWeather() { }
    public static boolean isWhite(Holder<Biome> biome) { return biome.is(WHITE_BIOMES); }
    public static boolean isDark(Holder<Biome> biome) { return biome.is(DARK_BIOMES); }
    public static boolean isDarkForest(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().equals(
                ResourceLocation.fromNamespaceAndPath("changede", "dark_latex_forest"))).orElse(false);
    }
}
