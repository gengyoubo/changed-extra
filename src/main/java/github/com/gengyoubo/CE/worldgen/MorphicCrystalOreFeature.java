package github.com.gengyoubo.CE.worldgen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Biome modifiers alone cannot enforce a dimension if another pack reuses a biome. */
public final class MorphicCrystalOreFeature extends OreFeature {
    private static final ResourceLocation LATEX_SPACE=ResourceLocation.fromNamespaceAndPath("changede","latex_space");
    public MorphicCrystalOreFeature() { super(OreConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        return context.level().getLevel().dimension().location().equals(LATEX_SPACE) && super.place(context);
    }
}
