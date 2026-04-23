package github.com.gengyoubo.events;

import github.com.gengyoubo.changede;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.imc.CustomEMCRegistration;
import moze_intel.projecte.api.imc.IMCMethods;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;

public class addEMCEvents {
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if(!changede.PROJECTE){return;}
//        InterModComms.sendTo(
//                ProjectEAPI.PROJECTE_MODID,
//                IMCMethods.REGISTER_CUSTOM_EMC,
//                () -> new CustomEMCRegistration(NSSItem.createItem(Items.DIAMOND), 8192L)
//        );
    }
}
