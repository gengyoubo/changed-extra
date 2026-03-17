package github.com.gengyoubo.events;

import github.com.gengyoubo.CERegister;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public class SalvageEvents {

    @SubscribeEvent
    public static void onBreak(PlayerDestroyItemEvent event) {

        ItemStack stack = event.getOriginal();

        if (stack.getEnchantmentLevel(CERegister.SALVAGE.get()) > 0) {

            Player player = event.getEntity();

            stack.setDamageValue(stack.getMaxDamage());
            InteractionHand hand = event.getHand();
            if (hand != null) {
                player.setItemInHand(hand, stack);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();

        if (stack.getEnchantmentLevel(CERegister.SALVAGE.get()) > 0) {

            if (stack.getDamageValue() >= stack.getMaxDamage()) {

                if (stack.getItem() instanceof PickaxeItem) {

                    player.drop(stack.copy(), true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
    }
}