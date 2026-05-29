package github.com.gengyoubo.CE.mixins;

import net.foxyas.changedaddon.entity.simple.DarkLatexYufengQueenEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DarkLatexYufengQueenEntity.class, remap = false)
public class DarkLatexYufengQueenBehaviorMixin {
    @Inject(method = "targetSelectorTest", at = @At("HEAD"), cancellable = true)
    private void changede$doNotTargetDarkLatexAllies(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        LatexType targetLatexType = LatexType.getEntityLatexType(target);
        if (targetLatexType == ChangedLatexTypes.DARK_LATEX.get()) {
            cir.setReturnValue(false);
        } else if (targetLatexType == ChangedLatexTypes.WHITE_LATEX.get()) {
            cir.setReturnValue(true);
        }
    }
}
