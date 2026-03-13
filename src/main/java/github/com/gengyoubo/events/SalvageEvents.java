package github.com.gengyoubo.events;

import github.com.gengyoubo.ModEnchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public class SalvageEvents {

    @SubscribeEvent
    public static void onBreak(PlayerDestroyItemEvent event) {

        ItemStack stack = event.getOriginal();

        if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SALVAGE.get(), stack) > 0) {

            Player player = (Player) event.getEntity();

            stack.setDamageValue(stack.getMaxDamage());

            player.setItemInHand(event.getHand(), stack);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();

        if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SALVAGE.get(), stack) > 0) {

            if (stack.getDamageValue() >= stack.getMaxDamage()) {

                if (stack.getItem() instanceof PickaxeItem) {

                    player.drop(stack.copy(), true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
    }
}