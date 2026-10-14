package github.com.gengyoubo.CE.compat.jer;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import mekanism.common.config.MekanismConfig;
import mekanism.common.resource.ore.OreType;
import mekanism.common.world.ResizableOreFeature;
import mekanism.common.world.ResizableOreFeatureConfig;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Explicit adapter for Mekanism's configurable natural ore generation; excludes retrogen. */
public final class MekanismOreCompat {
    private MekanismOreCompat() {}

    public static OreConfiguration configuration(ConfiguredFeature<?, ?> feature) {
        // OreRetrogenFeature subclasses ResizableOreFeature: do not count it as another natural vein.
        if (feature.feature().getClass() != ResizableOreFeature.class
                || !(feature.config() instanceof ResizableOreFeatureConfig config)) return null;
        var vein = MekanismConfig.world.getVeinConfig(config.oreVeinType());
        int size = config.size().getAsInt();
        if (!vein.shouldGenerate().getAsBoolean() || size <= 0) return null;
        return new OreConfiguration(config.targetStates(), size, config.discardChanceOnAirExposure().getAsFloat());
    }

    public static boolean enabledPlacement(JsonObject json) {
        if (!json.has("oreVeinType") || !json.has("retroGen") || json.get("retroGen").getAsBoolean()) return false;
        var type = OreType.OreVeinType.CODEC.parse(JsonOps.INSTANCE, json.get("oreVeinType")).result();
        return type.isPresent() && MekanismConfig.world.getVeinConfig(type.get()).shouldGenerate().getAsBoolean();
    }

    /** Include all dynamic parameters so config changes refresh the server-confirmed charts. */
    public static int configFingerprint() {
        int hash = 1;
        for (OreType ore : OreType.values()) {
            for (int index = 0; index < ore.getBaseConfigs().size(); index++) {
                var vein = MekanismConfig.world.getVeinConfig(new OreType.OreVeinType(ore, index));
                var range = vein.range();
                hash = 31 * hash + Boolean.hashCode(vein.shouldGenerate().getAsBoolean());
                hash = 31 * hash + vein.perChunk().getAsInt();
                hash = 31 * hash + vein.maxVeinSize().getAsInt();
                hash = 31 * hash + range.shape().get().ordinal();
                hash = 31 * hash + range.plateau().getAsInt();
                hash = 31 * hash + range.minInclusive().anchorType().get().ordinal();
                hash = 31 * hash + range.minInclusive().value().getAsInt();
                hash = 31 * hash + range.maxInclusive().anchorType().get().ordinal();
                hash = 31 * hash + range.maxInclusive().value().getAsInt();
                hash = 31 * hash + Float.floatToIntBits(vein.discardChanceOnAirExposure().getAsFloat());
            }
        }
        return hash;
    }
}
