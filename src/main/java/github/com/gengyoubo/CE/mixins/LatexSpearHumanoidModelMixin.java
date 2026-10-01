package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.client.LatexSpearAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class LatexSpearHumanoidModelMixin {
    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void changede$skipSlash(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        if (LatexSpearAnimations.jabProgress(entity, ageInTicks - entity.tickCount) >= 0) ci.cancel();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void changede$stabHand(LivingEntity entity, float limbSwing, float limbAmount, float ageInTicks,
                                  float yaw, float pitch, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float partialTick = ageInTicks - entity.tickCount;
        LatexSpearAnimations.thirdPersonHand(entity, model.rightArm, model.head, HumanoidArm.RIGHT, partialTick);
        LatexSpearAnimations.thirdPersonHand(entity, model.leftArm, model.head, HumanoidArm.LEFT, partialTick);
    }
}
