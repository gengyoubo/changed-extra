package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.changede;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

public class addEMCEvents {
    public static void onItemPickup(EntityItemPickupEvent event) {
        if(!changede.PROJECTE){return;}
//        InterModComms.sendTo(
//                ProjectEAPI.PROJECTE_MODID,
//                IMCMethods.REGISTER_CUSTOM_EMC,
//                () -> new CustomEMCRegistration(NSSItem.createItem(Items.DIAMOND), 8192L)
//        );
    }
}
