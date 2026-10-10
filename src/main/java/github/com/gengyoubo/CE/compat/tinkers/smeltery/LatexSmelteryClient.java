package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.smeltery.client.render.TankBlockEntityRenderer;

public final class LatexSmelteryClient {
    private LatexSmelteryClient() {}
    public static void initialize(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(LatexSmelteryCompat.TANK_ENTITY.get(), TankBlockEntityRenderer::new));
        if (Boolean.getBoolean("changede.verifyLatexTinkersClient"))
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(LatexSmelteryClientChecks::tick);
    }
}
