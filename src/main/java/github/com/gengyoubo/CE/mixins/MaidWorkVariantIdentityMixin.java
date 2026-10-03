package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.maid.MaidWorkBody;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransfurVariant.class, remap = false)
public class MaidWorkVariantIdentityMixin {
    @Inject(method = "getEntityVariant", at = @At("HEAD"), cancellable = true)
    private static void changede$resolveWorkVariant(LivingEntity entity, CallbackInfoReturnable<TransfurVariant<?>> cir) {
        if (entity instanceof MaidWorkBody work && work.workBody() != null) cir.setReturnValue(work.workBody().getSelfVariant());
    }
}
