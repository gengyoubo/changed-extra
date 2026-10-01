package github.com.gengyoubo.CE.skill;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Physical traits exposed as attributes; these do not grant active abilities or flight permission. */
@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SkillAttributes {
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "changede");
    public static final RegistryObject<Attribute> FLIGHT_CONTROL = trait("flight_control");
    public static final RegistryObject<Attribute> LANDING_RESISTANCE = trait("landing_resistance");
    public static final RegistryObject<Attribute> DAMAGE_VS_WHITE = trait("damage_vs_white");
    public static final RegistryObject<Attribute> DAMAGE_VS_DARK = trait("damage_vs_dark");
    private static RegistryObject<Attribute> trait(String id) {
        return REGISTRY.register(id, () -> new RangedAttribute("attribute.changede." + id, 0, 0, 1).setSyncable(true));
    }
    @SubscribeEvent public static void add(EntityAttributeModificationEvent event) {
        for (var attribute : REGISTRY.getEntries()) event.add(EntityType.PLAYER, attribute.get());
    }
}
