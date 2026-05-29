package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEEntity {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "changede");

    public static final RegistryObject<EntityType<LatexPaintingPortalEntity>> LATEX_PAINTING_PORTAL =
            ENTITY_TYPES.register("latex_painting_portal", () ->
                    EntityType.Builder.<LatexPaintingPortalEntity>of(LatexPaintingPortalEntity::new, MobCategory.MISC)
                            .sized(3.0F, 3.0F)
                            .clientTrackingRange(10)
                            .updateInterval(10)
                            .build("latex_painting_portal"));
}
