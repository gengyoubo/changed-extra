package github.com.gengyoubo;

import github.com.gengyoubo.events.SWEvents;
import github.com.gengyoubo.events.SalvageEvents;
import github.com.gengyoubo.events.ScorchingHeatEvents;
import github.com.gengyoubo.events.XPBoostEvents;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("changede")
public class changede {
    public changede() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEnchantments.ENCHANTMENTS.register(
                FMLJavaModLoadingContext.get().getModEventBus()
        );
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(new SalvageEvents());
        MinecraftForge.EVENT_BUS.register(new ScorchingHeatEvents());
        MinecraftForge.EVENT_BUS.register(new XPBoostEvents());
        MinecraftForge.EVENT_BUS.register(new SWEvents());
    }
    public static String getMiningStrength(TransfurVariant<?> variant) {
        return variant == null ? "unknown" : variant.miningStrength.name().toLowerCase();
    }
}