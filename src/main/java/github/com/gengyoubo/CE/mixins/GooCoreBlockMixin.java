package github.com.gengyoubo.CE.mixins;

import net.foxyas.changedaddon.block.GooCoreBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GooCoreBlock.class)
public class GooCoreBlockMixin {

    @Inject(
            method = "appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/BlockGetter;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("TAIL"),
            cancellable = true)
    private void changede$appendTooltip(ItemStack itemstack, @Nullable BlockGetter world, List<Component> list, TooltipFlag flag, CallbackInfo ci) {
        list.add(Component.translatable("tooltip.goo_core_block.desc"));
        ci.cancel();
    }
}