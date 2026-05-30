package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractDarkLatexEntity.class, remap = false)
public class DarkLatexFriendlyFireMixin {
    @Inject(method = "onDamagedBy", at = @At("HEAD"), cancellable = true)
    private void changede$ignoreDarkLatexAggroBroadcast(LivingEntity attacker, CallbackInfo ci) {
        if (attacker instanceof AbstractDarkLatexEntity) {
            ci.cancel();
            return;
        }

        if (LatexType.getEntityLatexType(attacker) == ChangedLatexTypes.DARK_LATEX.get()) {
            ci.cancel();
        }
    }
}
