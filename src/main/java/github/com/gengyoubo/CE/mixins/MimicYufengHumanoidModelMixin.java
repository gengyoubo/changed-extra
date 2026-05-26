package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.LP.client.model.MimicYufengFlightPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class MimicYufengHumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void changede$poseMimicYufengFlight(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                                float netHeadYaw, float headPitch, CallbackInfo ci) {
        MimicYufengFlightPose.apply((HumanoidModel<?>) (Object) this, entity, ageInTicks);
    }
}
