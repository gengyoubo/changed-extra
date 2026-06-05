package github.com.gengyoubo.CE.LP.world.Screen;


import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.world.Menu.CEMenus;
import github.com.gengyoubo.CE.client.renderer.LatexPaintingPortalEntityRenderer;
import github.com.gengyoubo.CE.client.renderer.LatexPaintingPortalRenderer;
import github.com.gengyoubo.CE.client.renderer.LatexPortalRenderManager;
import github.com.gengyoubo.CE.init.CEBlock;
import github.com.gengyoubo.CE.init.CEBlockEntity;
import github.com.gengyoubo.CE.init.CEEntity;
import github.com.gengyoubo.CE.changede;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEScreen {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CEBlockEntity.LATEX_PAINTING_PORTAL.get(), LatexPaintingPortalRenderer::new);
        event.registerEntityRenderer(CEEntity.LATEX_PAINTING_PORTAL.get(), LatexPaintingPortalEntityRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CEMenus.BASIC_GENERATOR_BLOCK_ENTITY.get(), BasicGeneratorBlockEntityScreen::new);
            MenuScreens.register(CEMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(CEMenus.LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE.get(), LatexCreativeExtranalbodyCraftTableScreen::new);
            MenuScreens.register(CEMenus.SPACE_TOWER.get(), SpaceTowerScreen::new);
            ItemBlockRenderTypes.setRenderLayer(CELPBlock.SPACE_TOWER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CEBlock.DARK_LATEX_LEAVES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CEBlock.WHITE_LATEX_LEAVES.get(), RenderType.cutout());
            MinecraftForge.EVENT_BUS.addListener(LatexPortalRenderManager::onRenderLevelStage);
            registerPonderPluginIfAvailable();
        });
    }

    private static void registerPonderPluginIfAvailable() {
        if (!ModList.get().isLoaded("ponder") || !ModList.get().isLoaded("create")) {
            return;
        }

        try {
            Class<?> ponderPluginClass = Class.forName("net.createmod.ponder.api.registration.PonderPlugin");
            Class<?> ponderIndexClass = Class.forName("net.createmod.ponder.foundation.PonderIndex");
            Object plugin = Class.forName("github.com.gengyoubo.CE.LP.ponder.CEPonderPlugin")
                    .getDeclaredConstructor()
                    .newInstance();
            Method addPlugin = findPonderAddPluginMethod(ponderIndexClass, ponderPluginClass);
            addPlugin.invoke(null, plugin);
        } catch (ReflectiveOperationException | LinkageError exception) {
            changede.LOGGER.warn("Failed to register changede ponder plugin", exception);
        }
    }

    private static Method findPonderAddPluginMethod(Class<?> ponderIndexClass, Class<?> ponderPluginClass)
            throws NoSuchMethodException {
        for (Method method : ponderIndexClass.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals("addPlugin")
                    && parameterTypes.length == 1
                    && parameterTypes[0].isAssignableFrom(ponderPluginClass)) {
                return method;
            }
        }
        throw new NoSuchMethodException("PonderIndex.addPlugin(PonderPlugin)");
    }

}
