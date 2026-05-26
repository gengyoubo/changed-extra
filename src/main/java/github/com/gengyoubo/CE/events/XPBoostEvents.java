package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CEEnchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
public class XPBoostEvents {
public static void onBlockXP(BlockEvent.BreakEvent event) {

    Player player = event.getPlayer();
    ItemStack tool = player.getMainHandItem();

    int level =
            tool.getEnchantmentLevel(CEEnchantment.XPBOOST.get() );

    if (level <= 0) return;

    int xp = event.getExpToDrop();

    int extra = Math.round(
        xp * ((float) Math.log10(level + 1) * 2)
    );

    event.setExpToDrop(xp + extra);
}
}
