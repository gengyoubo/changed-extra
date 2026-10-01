package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.init.CEWoodFamilies;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CEWoodClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CEWoodFamilies.SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(CEWoodFamilies.HANGING_SIGN.get(), HangingSignRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CEWoodFamilies.ALL.forEach(family -> Sheets.addWoodType(family.woodType)));
    }
}
