package github.com.gengyoubo.CE.compat.tinkers;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

/** Linked only when Tinkers' Construct is present. */
@SuppressWarnings("deprecation")
public final class LatexTinkersCompat {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create("changede");
    public static final StaticModifier<LatexPlasticityModifier> PLASTICITY = MODIFIERS.register("latex_plasticity", LatexPlasticityModifier::new);
    public static final StaticModifier<LatexResilienceModifier> RESILIENCE = MODIFIERS.register("latex_resilience", LatexResilienceModifier::new);
    private LatexTinkersCompat() {}
    public static void initialize(IEventBus bus) {
        MODIFIERS.register(bus);
        github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryCompat.initialize(bus);
        if (Boolean.getBoolean("changede.verifyLatexTinkers")) MinecraftForge.EVENT_BUS.addListener(LatexTinkersRegressionChecks::started);
    }
    public static int level(int level) { return Math.max(0, Math.min(5, level)); }
}
