package github.com.gengyoubo.events;

import net.minecraft.world.item.enchantment.EnchantmentInstance;
import github.com.gengyoubo.ModEnchantments;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabEvents {

    @SubscribeEvent
    public static void addBooks(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {

            for (int i = 1; i <= ModEnchantments.SCORCHINGHEAT.get().getMaxLevel(); i++) {

    event.accept(
        EnchantedBookItem.createForEnchantment(
            new EnchantmentInstance(
                ModEnchantments.SCORCHINGHEAT.get(), i
            )
        )
    );

}

        }
    }
}
