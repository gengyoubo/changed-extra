package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.CE.LP.client.model.MimicSirenTailVisibility;
import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class MimicSirenLowerBodyMixin {
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void changede$replaceLowerBody(EntityModel<?> model, PoseStack poses, VertexConsumer buffer,
                                          int light, int overlay, float red, float green, float blue, float alpha,
                                          LivingEntity entity, float yaw, float partialTick, PoseStack outerPoses,
                                          MultiBufferSource buffers, int packedLight) {
        Runnable render = () -> model.renderToBuffer(poses, buffer, light, overlay, red, green, blue, alpha);
        if (MimicSirenTailItem.isWornBy(entity)) {
            MimicSirenTailVisibility.renderUpperBody(model, render);
        } else {
            render.run();
        }
    }
}
