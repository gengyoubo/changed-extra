package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSpearCombatEvents {
    @SubscribeEvent public static void knockback(LivingKnockBackEvent event) {
        // Damage's built-in knockback would otherwise bypass fatigue or double the spear's impulse.
        if (LatexSpearItem.resolvingHitOn(event.getEntity())) event.setCanceled(true);
    }
}
