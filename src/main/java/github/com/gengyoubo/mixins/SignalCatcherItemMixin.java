package github.com.gengyoubo.mixins;

import net.foxyas.changedaddon.item.SignalCatcherItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SignalCatcherItem.class)
public class SignalCatcherItemMixin {

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void changede$appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
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

            tooltip.add(Component.literal("§oCoords §l" + x + " " + y + " " + z));
            if (tag.getBoolean("set")) {
                tooltip.add(Component.literal("§oDistance §l" + Math.round(distance)));
            }
        }
        ci.cancel();
    }
    @Redirect(method = "releaseUsing",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void releaseUsing(Player player, Component p_36216_, boolean p_36217_) {
        player.displayClientMessage(Component.translatable("message.signal_catcher.no_location"), false);
    }
}