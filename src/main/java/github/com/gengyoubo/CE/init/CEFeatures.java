package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.worldgen.MorphicCrystalOreFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CEFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"changede");
    public static final RegistryObject<Feature<OreConfiguration>> MORPHIC_CRYSTAL_ORE=
            FEATURES.register("morphic_crystal_ore",MorphicCrystalOreFeature::new);
    private CEFeatures() { }
}
