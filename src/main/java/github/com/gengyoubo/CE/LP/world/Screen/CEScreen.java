package github.com.gengyoubo.CE.LP.world.Screen;


import github.com.gengyoubo.CE.LP.world.Menu.CEMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEScreen {
    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CEMenus.BASIC_GENERATOR_BLOCK_ENTITY.get(), BasicGeneratorBlockEntityScreen::new);
            MenuScreens.register(CEMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(CEMenus.LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE.get(), LatexCreativeExtranalbodyCraftTableScreen::new);
        });
    }

}
