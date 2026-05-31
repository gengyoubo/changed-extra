package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.BlockEntity.LatexPaintingPortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<LatexPaintingPortalBlockEntity>> LATEX_PAINTING_PORTAL =
            BLOCK_ENTITIES.register("latex_painting_portal",
                    () -> BlockEntityType.Builder.of(
                            LatexPaintingPortalBlockEntity::new,
                            CEBlock.LATEX_PAINTING_PORTAL.get()
                    ).build(null));

}
