package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.client.LatexSpearAnimations;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.UseItemMode;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changed uses its own arm animator instead of HumanoidModel.setupAnim. */
@Mixin(value = HumanoidAnimator.class, remap = false)
public abstract class LatexSpearChangedAnimatorMixin {
    @Shadow @Final public AdvancedHumanoidModel<?> entityModel;
    @Shadow public float partialTicks;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void changede$stabChangedHand(ChangedEntity entity, float limbSwing, float limbAmount, float ageInTicks,
                                         float yaw, float pitch, CallbackInfo ci) {
        if (entity.getItemUseMode() != UseItemMode.NORMAL) return;
        LatexSpearAnimations.thirdPersonHand(entity, entityModel.getArm(HumanoidArm.RIGHT), entityModel.getHead(),
                HumanoidArm.RIGHT, partialTicks);
        LatexSpearAnimations.thirdPersonHand(entity, entityModel.getArm(HumanoidArm.LEFT), entityModel.getHead(),
                HumanoidArm.LEFT, partialTicks);
    }
}
