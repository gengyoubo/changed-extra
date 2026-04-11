package github.com.gengyoubo;

import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.world.Menu.CEMenus;
import github.com.gengyoubo.commands.ReloadEMCCMD;
import github.com.gengyoubo.events.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("changede")
public class changede {
    public static final Boolean PROJECTE =ModList.get().isLoaded("projecte");
    public changede(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        CERegister.ENCHANTMENTS.register(bus);
        CERegister.CREATIVE_MODE_TABS.register(bus);
        CERegister.ITEMS.register(bus);
        CELPRegister.ITEMS.register(bus);
        CELPRegister.WIRE_BLOCKS.register(bus);
        CELPRegister.BLOCK_ENTITIES.register(bus);
        CEMenus.REGISTRY.register(bus);
        CERegister.register();
        bus.addListener(latexStartEvents::setup);
        MinecraftForge.EVENT_BUS.register(new SalvageEvents());
        MinecraftForge.EVENT_BUS.register(new ScorchingHeatEvents());
        MinecraftForge.EVENT_BUS.register(new XPBoostEvents());
        MinecraftForge.EVENT_BUS.register(new SWEvents());
        //联动等价交换
        if (PROJECTE) {
            new ReloadEMCCMD();
            MinecraftForge.EVENT_BUS.register(new addEMCEvents());
        }
    }
}
