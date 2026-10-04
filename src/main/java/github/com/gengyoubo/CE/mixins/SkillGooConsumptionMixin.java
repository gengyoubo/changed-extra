package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.SkillGooDiet;
import net.ltxprogrammer.changed.item.AbstractLatexItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Consume skill meals before Changed can assimilate the eater and invalidate the active diet. */
@Mixin(AbstractLatexItem.class)
public abstract class SkillGooConsumptionMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void changede$eatGoo(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (!SkillGooDiet.isGoo(stack) || !(entity instanceof Player player)
                || !SkillGooDiet.finishSkillMeal(player, stack)) return;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) SkillGooDiet.consume(serverPlayer, stack);
        cir.setReturnValue(stack);
    }
}
