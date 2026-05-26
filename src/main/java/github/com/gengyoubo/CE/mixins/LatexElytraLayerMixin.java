package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.client.renderer.MimicYufengWingsRenderer;
import github.com.gengyoubo.CE.LP.item.MimicYufengWingsItem;
import net.ltxprogrammer.changed.client.renderer.layers.LatexElytraLayer;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LatexElytraLayer.class, remap = false)
public class LatexElytraLayerMixin<T extends ChangedEntity, M extends AdvancedHumanoidModel<T>> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$renderMimicYufengWings(ItemStack stack, T entity, CallbackInfoReturnable<Boolean> cir) {
        if (MimicYufengWingsItem.isMimicYufengWings(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$renderMimicYufengWingsModel(PoseStack p_116951_, MultiBufferSource p_116952_, int p_116953_, T p_116954_,
                                                      float p_116955_, float p_116956_, float p_116957_, float p_116958_,
                                                      float p_116959_, float p_116960_, CallbackInfo ci) {
        ItemStack stack = p_116954_.getItemBySlot(EquipmentSlot.CHEST);
        if (!MimicYufengWingsItem.isMimicYufengWings(stack)) {
            return;
        }
        @SuppressWarnings("unchecked")
        AdvancedHumanoidModel<?> parentModel = ((RenderLayer<T, M>) (Object) this).getParentModel();
        MimicYufengWingsRenderer.render(stack, p_116954_, parentModel, p_116951_, p_116952_, p_116953_, p_116955_, p_116956_, p_116958_, p_116959_, p_116960_);
        ci.cancel();
    }
}
