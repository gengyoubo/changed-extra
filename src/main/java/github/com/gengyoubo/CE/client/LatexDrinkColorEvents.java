package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.init.CEItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LatexDrinkColorEvents {
    private static final int DARK_LATEX_COFFEE_COLOR = 0x303030;

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? DARK_LATEX_COFFEE_COLOR : 0xFFFFFF,
                CEItem.BLACK_LATEX_COFFEE_POWDER.get());
    }
}
