package github.com.gengyoubo.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.LP.client.renderer.MimicYufengWingsRenderer;
import github.com.gengyoubo.LP.item.MimicYufengWingsItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$renderMimicYufengWings(ItemStack stack, T entity, CallbackInfoReturnable<Boolean> cir) {
        if (MimicYufengWingsItem.isMimicYufengWings(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void changede$renderMimicYufengWingsModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                                                      float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                                      float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!MimicYufengWingsItem.isMimicYufengWings(stack)) {
            return;
        }

        EntityModel<?> parentModel = ((RenderLayer<T, M>) (Object) this).getParentModel();
        MimicYufengWingsRenderer.render(stack, entity, parentModel, poseStack, bufferSource, packedLight, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ci.cancel();
    }
}
