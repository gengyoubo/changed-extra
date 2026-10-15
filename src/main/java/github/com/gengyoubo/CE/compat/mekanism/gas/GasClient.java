package github.com.gengyoubo.CE.compat.mekanism.gas;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

final class GasClient {
    static void initialize(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(GasCompat.MENU.get(), ReverserScreen::new)));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(GasCompat.GAS_PIPE_TILE.get(), mekanism.client.render.transmitter.RenderPressurizedTube::new));
    }
    private GasClient() {}
}
