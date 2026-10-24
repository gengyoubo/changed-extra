package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MimicSirenArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void changede$hideLeggings(PoseStack poses, MultiBufferSource buffers, LivingEntity entity,
                                      EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (slot == EquipmentSlot.LEGS && MimicSirenTailItem.isWornBy(entity)) {
            ci.cancel();
        }
    }
}
