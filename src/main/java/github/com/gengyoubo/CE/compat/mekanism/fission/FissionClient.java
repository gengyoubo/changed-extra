package github.com.gengyoubo.CE.compat.mekanism.fission;

import mekanism.generators.client.render.RenderFissionReactor;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class FissionClient {
    static void initialize(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                FissionCompat.TILES.values().forEach(tile -> event.registerBlockEntityRenderer(tile.get(), RenderFissionReactor::new)));
    }
    private FissionClient() {}
}
