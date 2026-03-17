package github.com.gengyoubo.events;

import github.com.gengyoubo.CERegister;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changede")
public class XPBoostEvents {
    @SubscribeEvent
public static void onBlockXP(BlockEvent.BreakEvent event) {

    Player player = event.getPlayer();
    ItemStack tool = player.getMainHandItem();

    int level =
            tool.getEnchantmentLevel(CERegister.XPBOOST.get() );

    if (level <= 0) return;

    int xp = event.getExpToDrop();

    int extra = Math.round(
        xp * ((float) Math.log10(level + 1) * 2)
    );

    event.setExpToDrop(xp + extra);
}
}
