package github.com.gengyoubo.CE.events;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class GooCoreTooltipEvents {
    private static final String GOO_CORE_BLOCK_CLASS = "net.foxyas.changedaddon.block.GooCoreBlock";

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        if (!GOO_CORE_BLOCK_CLASS.equals(blockItem.getBlock().getClass().getName())) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.goo_core_block.desc"));
    }
}
