package github.com.gengyoubo.events;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class addEMC {
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();

//        if (stack.getItem() instanceof InactiveDarkLatex) {
//            ItemPE.setEmc(stack, 1024);
//        }
    }
}
