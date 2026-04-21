package github.com.gengyoubo.fix;

import com.mojang.datafixers.util.Pair;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChangedEntitiesFix {
    public static final DeferredRegister<EntityType<?>> REGISTRY;
    public static final RegistryObject<EntityType<SpecialLatex>> SPECIAL_LATEX;
    private static final List<Pair<Supplier<EntityType<? extends ChangedEntity>>, Supplier<AttributeSupplier.Builder>>> ATTR_FUNC_REGISTRY = new ArrayList<>();

    static {
        REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "changed");
        SPECIAL_LATEX=registerNoEgg("special_latex",
                EntityType.Builder.of(SpecialLatex::new, MobCategory.MONSTER).clientTrackingRange(10).sized(0.7F, 1.93F));
    }
    private static <T extends ChangedEntity> RegistryObject<EntityType<T>> registerNoEgg(
            String name,
            EntityType.Builder<T> builder) {
        String regName = Changed.modResource(name).toString();
        RegistryObject<EntityType<T>> entityType = REGISTRY.register(name, () -> builder.build(regName));
        ATTR_FUNC_REGISTRY.add(new Pair<>(entityType::get, T::createLatexAttributes));
        return entityType;
    }
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        for (Pair<Supplier<EntityType<? extends ChangedEntity>>, Supplier<AttributeSupplier.Builder>> pair : ATTR_FUNC_REGISTRY) {
            event.put(pair.getFirst().get(), pair.getSecond().get().build());
        }
    }
}
