package github.com.gengyoubo;

import com.mojang.logging.LogUtils;
import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.LP.network.CENetwork;
import github.com.gengyoubo.LP.world.Menu.CEMenus;
import github.com.gengyoubo.commands.ReloadEMCCommand;
import github.com.gengyoubo.events.*;
import github.com.gengyoubo.fix.SpecialLatex.CEChangedSounds;
import github.com.gengyoubo.fix.SpecialLatex.ChangedEntitiesFix;
import github.com.gengyoubo.fix.SpecialLatex.PatreonBenefitsFix;
import github.com.gengyoubo.projectextended.PERegister;
import github.com.gengyoubo.projectextended.PTotemOfUndying;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("changede")
public class changede {
    public static final Boolean PROJECTE =ModList.get().isLoaded("projecte");
    public static final Boolean PE=ModList.get().isLoaded("projectextended");
    public static final Logger LOGGER = LogUtils.getLogger();
    public changede(FMLJavaModLoadingContext context) throws InterruptedException {
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        CERegister.ENCHANTMENTS.register(bus);
        CERegister.CREATIVE_MODE_TABS.register(bus);
        CERegister.ITEMS.register(bus);
        CELPRegister.ITEMS.register(bus);
        CELPRegister.WIRE_BLOCKS.register(bus);
        CELPRegister.BLOCK_ENTITIES.register(bus);
        ChangedEntitiesFix.REGISTRY.register(bus);
        CEChangedSounds.REGISTRY.register(bus);
        CEMenus.REGISTRY.register(bus);
        PatreonBenefitsFix.REGISTRY.register(bus);
        CENetwork.register();
        CERegister.register();
        bus.addListener(latexStartEvents::setup);
        MinecraftForge.EVENT_BUS.register(new SalvageEvents());
        MinecraftForge.EVENT_BUS.register(new ScorchingHeatEvents());
        MinecraftForge.EVENT_BUS.register(new XPBoostEvents());
        MinecraftForge.EVENT_BUS.register(new SWEvents());
        //联动等价交换
        if (PROJECTE) {
            new ReloadEMCCommand();
            MinecraftForge.EVENT_BUS.register(new addEMCEvents());
            PTotemOfUndying.ITEMS.register(bus);
            if(PE){
                PERegister.ITEMS.register(bus);
            }
        }
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                PatreonBenefits.loadBenefits();
                PatreonBenefitsFix.readFields();
                PatreonBenefitsFix.SpecialForm.loadBenefits();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
