package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalBossCompat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's 1024-HP range would otherwise silently clamp the 2800-HP encounter. */
@Mixin(RangedAttribute.class)
public abstract class TwilightFinalHealthRangeMixin {
    @Inject(method = "sanitizeValue", at = @At("RETURN"), cancellable = true)
    private void changede$allowFinalBossHealth(double value, CallbackInfoReturnable<Double> cir) {
        if ((Object) this == Attributes.MAX_HEALTH && Double.isFinite(value) && value > cir.getReturnValue()
                && value <= TwilightFinalBossCompat.HEALTH) cir.setReturnValue(value);
    }
}
