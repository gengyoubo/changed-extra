package github.com.gengyoubo;

import github.com.gengyoubo.events.SalvageEvents;
import github.com.gengyoubo.events.ScorchingHeatEvents;
import github.com.gengyoubo.events.XPBoostEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("changede")
public class changede {
    public changede() {
        ModEnchantments.ENCHANTMENTS.register(
                FMLJavaModLoadingContext.get().getModEventBus()
        );
        MinecraftForge.EVENT_BUS.register(new SalvageEvents());
        MinecraftForge.EVENT_BUS.register(new ScorchingHeatEvents());
        MinecraftForge.EVENT_BUS.register(new XPBoostEvents());
    }

}