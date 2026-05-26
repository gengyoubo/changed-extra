package github.com.gengyoubo.CE.LP.world.Screen;


import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.world.Menu.CEMenus;
import github.com.gengyoubo.CE.LP.ponder.CEPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEScreen {
    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CEMenus.BASIC_GENERATOR_BLOCK_ENTITY.get(), BasicGeneratorBlockEntityScreen::new);
            MenuScreens.register(CEMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(CEMenus.LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE.get(), LatexCreativeExtranalbodyCraftTableScreen::new);
            MenuScreens.register(CEMenus.SPACE_TOWER.get(), SpaceTowerScreen::new);
            ItemBlockRenderTypes.setRenderLayer(CELPBlock.SPACE_TOWER.get(), RenderType.cutout());
            PonderIndex.addPlugin(new CEPonderPlugin());
        });
    }

}
