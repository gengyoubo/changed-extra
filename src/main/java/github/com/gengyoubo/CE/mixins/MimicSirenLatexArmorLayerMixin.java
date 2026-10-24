package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.ltxprogrammer.changed.client.renderer.layers.LatexHumanoidArmorLayer;
import net.ltxprogrammer.changed.client.renderer.model.armor.LatexHumanoidArmorModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LatexHumanoidArmorLayer.class, remap = false)
public abstract class MimicSirenLatexArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$hideLeggings(PoseStack poses, MultiBufferSource buffers, ChangedEntity entity,
                                      EquipmentSlot slot, int light, LatexHumanoidArmorModel<?, ?> armor, CallbackInfo ci) {
        if (slot == EquipmentSlot.LEGS && MimicSirenTailItem.isWornBy(entity)) {
            ci.cancel();
        }
    }
}
