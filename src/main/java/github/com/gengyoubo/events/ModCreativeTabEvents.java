package github.com.gengyoubo.events;

import net.minecraft.world.item.enchantment.EnchantmentInstance;
import github.com.gengyoubo.ModCreativeTabs;
import github.com.gengyoubo.ModEnchantments;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabEvents {

    @SubscribeEvent
public static void addBooks(BuildCreativeModeTabContentsEvent event) {

    if (event.getTab() == ModCreativeTabs.EE.get()) {

        for (var enchantmentEntry : ModEnchantments.ENCHANTMENTS.getEntries()) {

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
