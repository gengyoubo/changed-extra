package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class MimicSirenDepthStriderMixin {
    @Inject(method = "getDepthStrider", at = @At("RETURN"), cancellable = true)
    private static void changede$sirenWaterEfficiency(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (MimicSirenTailItem.isWornBy(entity)) {
            cir.setReturnValue(Math.max(2, cir.getReturnValue()));
        }
    }
}
