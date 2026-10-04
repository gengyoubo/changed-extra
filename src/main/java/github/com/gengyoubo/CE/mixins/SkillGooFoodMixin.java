package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.SkillGooDiet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raw goo uses the ordinary item-use timer; permission is rechecked when consumption completes. */
@Mixin(Item.class)
public abstract class SkillGooFoodMixin {
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void changede$gooDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (SkillGooDiet.isGoo(stack)) cir.setReturnValue(32);
    }
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void changede$gooAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        if (SkillGooDiet.isGoo(stack)) cir.setReturnValue(UseAnim.EAT);
    }
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void changede$eatGoo(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (!SkillGooDiet.isGoo(stack)) return;
        if (!level.isClientSide && entity instanceof ServerPlayer player) SkillGooDiet.consume(player, stack);
        cir.setReturnValue(stack);
    }
}
