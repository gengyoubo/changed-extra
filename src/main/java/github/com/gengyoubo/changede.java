package github.com.gengyoubo;

import com.mojang.logging.LogUtils;
import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.network.CENetwork;
import github.com.gengyoubo.LP.world.Menu.CEMenus;
import github.com.gengyoubo.commands.ReloadEMCCMD;
import github.com.gengyoubo.events.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

@Mod("changede")
public class changede {
    public static final Boolean PROJECTE =ModList.get().isLoaded("projecte");
    public static final Logger LOGGER = LogUtils.getLogger();
    public changede(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        CERegister.ENCHANTMENTS.register(bus);
        CERegister.CREATIVE_MODE_TABS.register(bus);
        CERegister.ITEMS.register(bus);
        CELPRegister.ITEMS.register(bus);
        CELPRegister.WIRE_BLOCKS.register(bus);
        CELPRegister.BLOCK_ENTITIES.register(bus);
        CEMenus.REGISTRY.register(bus);
        CENetwork.register();
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
