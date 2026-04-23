package github.com.gengyoubo.events;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SignalCatcherTooltipEvents {
    private static final String SIGNAL_CATCHER_ITEM_CLASS = "net.foxyas.changedaddon.item.SignalCatcherItem";

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SIGNAL_CATCHER_ITEM_CLASS.equals(stack.getItem().getClass().getName())) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.signal_catcher.hold_shift"));
        event.getToolTip().add(Component.translatable("tooltip.signal_catcher.scan_normal"));
        event.getToolTip().add(Component.translatable("tooltip.signal_catcher.scan_super"));

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("set")) {
            event.getToolTip().add(Component.translatable("tooltip.signal_catcher.no_signal"));
            return;
        }

        int x = (int) Math.round(tag.getDouble("x"));
        int y = (int) Math.round(tag.getDouble("y"));
        int z = (int) Math.round(tag.getDouble("z"));
        event.getToolTip().add(Component.translatable("tooltip.signal_catcher.coords", x, y, z));

        Player player = event.getEntity();
        if (player != null) {
            double dx = tag.getDouble("x") - player.getX();
            double dy = tag.getDouble("y") - player.getY();
            double dz = tag.getDouble("z") - player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            event.getToolTip().add(Component.translatable("tooltip.signal_catcher.distance", distance));
        }
    }
}
