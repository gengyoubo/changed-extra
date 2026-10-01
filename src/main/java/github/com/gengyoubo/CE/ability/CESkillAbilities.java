package github.com.gengyoubo.CE.ability;

import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CESkillAbilities {
    public static final DeferredRegister<AbstractAbility<?>> REGISTRY = ChangedRegistry.ABILITY.createDeferred("changede");
    public static final RegistryObject<YufengFlightAbility> TAKEOFF = REGISTRY.register("yufeng_takeoff", () -> new YufengFlightAbility(true));
    public static final RegistryObject<YufengFlightAbility> BOOST = REGISTRY.register("yufeng_boost", () -> new YufengFlightAbility(false));

    @SubscribeEvent public static void abilities(TransfurVariant.UniversalAbilitiesEvent event) {
        // Registry IDs remain reserved, but active skill grants wait for the WLP/Power stage.
    }
}
