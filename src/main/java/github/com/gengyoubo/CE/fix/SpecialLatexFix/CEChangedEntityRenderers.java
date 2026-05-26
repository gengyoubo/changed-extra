package github.com.gengyoubo.CE.fix.SpecialLatexFix;

import github.com.gengyoubo.CE.LP.client.model.MimicYufengWingsModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEChangedEntityRenderers {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChangedEntitiesFix.SPECIAL_LATEX.get(), SpecialLatexRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MimicYufengWingsModel.LAYER_LOCATION, MimicYufengWingsModel::createBodyLayer);
    }
}
