package github.com.gengyoubo.CE.worldgen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public final class TwilightOreFeature extends OreFeature {
    public TwilightOreFeature() { super(OreConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        return context.level().getLevel().dimension().location().equals(ResourceLocation.parse("twilightforest:twilight_forest")) && super.place(context);
    }
}
