package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CECreativeModeTab;
import github.com.gengyoubo.CE.init.CEEnchantment;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabEvents {

    @SubscribeEvent
public static void addBooks(BuildCreativeModeTabContentsEvent event) {

    if (event.getTab() == CECreativeModeTab.EE.get()) {

        for (var enchantmentEntry : CEEnchantment.ENCHANTMENTS.getEntries()) {

            var enchantment = enchantmentEntry.get();

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {

                event.accept(
                        EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(enchantment, level)
                        )
                );
            }
        }
    }
}}
