package github.com.gengyoubo.mixins;

import net.foxyas.changedaddon.item.SignalCatcherItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(SignalCatcherItem.class)
public class SignalCatcherItemMixin {

    /**
     * @author gengyoubo
     * @reason 替换硬编码的 Tooltip 为翻译键
     */
    @Overwrite
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, 
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag pIsAdvanced) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        CompoundTag tag = stack.getOrCreateTag();
        double x = tag.getDouble("x");
        double y = tag.getDouble("y");
        double z = tag.getDouble("z");
        double deltaX = x - player.getX();
        double deltaY = y - player.getY();
        double deltaZ = z - player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.signal_catcher.hold_shift"));
        } else {
            tooltip.add(Component.translatable("tooltip.signal_catcher.scan_normal"));
            tooltip.add(Component.translatable("tooltip.signal_catcher.scan_super"));
        }
        
        tooltip.add(Component.literal(("§oCoords §l" + x + " " + y + " " + z)));
        if (stack.getOrCreateTag().getBoolean("set")) {
            tooltip.add(Component.literal(("§oDistance §l" + Math.round(distance))));
        }
    }
     /**
     * @author gengyoubo
     * @reason 替换硬编码的消息为翻译键
     */
    @Overwrite
    public void releaseUsing(@NotNull ItemStack itemstack, @NotNull Level world, 
                             @NotNull LivingEntity entity, int time) {
        if (!itemstack.getOrCreateTag().getBoolean("set")) {
            if (entity instanceof Player player && !player.level().isClientSide()) {
                // 原代码：player.displayClientMessage(Component.literal("§o§bNo Location Found §l[Not Close Enough]"), false);
                // 修改为：
                player.displayClientMessage(Component.translatable("message.signal_catcher.no_location"), false);
            }
        }
    }
}