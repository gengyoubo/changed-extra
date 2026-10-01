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
    public static final RegistryObject<Attribute> REGENERATION_SPEED = growthTrait("regeneration_speed", 1, 64);
    public static final RegistryObject<Attribute> EXHAUSTION_REDUCTION = growthTrait("exhaustion_reduction", 0, 1);
    public static final RegistryObject<Attribute> FOOD_SATURATION = growthTrait("food_saturation", 1, 64);
    public static final RegistryObject<Attribute> SATURATION_CAPACITY = growthTrait("saturation_capacity", 0, 100);
    public static final RegistryObject<Attribute> FLAT_DAMAGE_VS_WHITE = growthTrait("flat_damage_vs_white", 0, 100);
    public static final RegistryObject<Attribute> WHITE_LATEX_RESISTANCE = trait("white_latex_resistance");
    public static final RegistryObject<Attribute> WHITE_FOG_RESISTANCE = trait("white_fog_resistance");
    public static final RegistryObject<Attribute> ARMOR_ADAPTATION = growthTrait("armor_adaptation", 0, 100);
    public static final RegistryObject<Attribute> WEAPON_ADAPTATION = growthTrait("weapon_adaptation", 0, 100);
    public static final RegistryObject<Attribute> LATEX_WEAPON_DAMAGE = growthTrait("latex_weapon_damage", 0, 100);
    private static RegistryObject<Attribute> growthTrait(String id, double base, double maximum) {
        return REGISTRY.register(id, () -> new RangedAttribute("attribute.changede." + id, base, 0, maximum).setSyncable(true));
    }
    public static boolean isUniversalGrowth(net.minecraft.resources.ResourceLocation id) {
        return id.getNamespace().equals("changede") && java.util.Set.of("regeneration_speed", "exhaustion_reduction",
                "food_saturation", "saturation_capacity").contains(id.getPath());
    }
    private static RegistryObject<Attribute> trait(String id) {
        return REGISTRY.register(id, () -> new RangedAttribute("attribute.changede." + id, 0, 0, 1).setSyncable(true));
    }
    @SubscribeEvent public static void add(EntityAttributeModificationEvent event) {
        for (var attribute : REGISTRY.getEntries()) event.add(EntityType.PLAYER, attribute.get());
    }
}
