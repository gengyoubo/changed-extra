package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.maid.MaidWorkBody;
import net.ltxprogrammer.changed.util.EntityUtil;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityUtil.class, remap = false)
public class MaidWorkEntityIdentityMixin {
    @Inject(method = "maybeGetOverlaying", at = @At("HEAD"), cancellable = true)
    private static void changede$resolveWorkBody(LivingEntity entity, CallbackInfoReturnable<LivingEntity> cir) {
        if (entity instanceof MaidWorkBody work && work.workBody() != null) cir.setReturnValue(work.workBody());
    }
}
