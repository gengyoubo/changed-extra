package github.com.gengyoubo.LP.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.LP.client.model.MimicYufengWingsModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class MimicYufengWingsRenderer {
    private static MimicYufengWingsModel model;

    private MimicYufengWingsRenderer() {
    }

    public static void render(ItemStack stack, LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        render(stack, entity, null, poseStack, bufferSource, packedLight, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    public static void render(ItemStack stack, LivingEntity entity, EntityModel<?> parentModel, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        MimicYufengWingsModel wingsModel = getModel();
        wingsModel.attackTime = entity.getAttackAnim(Minecraft.getInstance().getFrameTime());
        wingsModel.riding = entity.isPassenger();
        wingsModel.young = entity.isBaby();
        wingsModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (parentModel != null) {
            wingsModel.copyBodyPoseFrom(parentModel);
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.125D);
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource,
                RenderType.armorCutoutNoCull(MimicYufengWingsModel.TEXTURE),
                false,
                stack.hasFoil()
        );
        wingsModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, float ageInTicks) {
        MimicYufengWingsModel wingsModel = getModel();
        wingsModel.setupItemPose(ageInTicks);

        poseStack.pushPose();
        double yOffset = displayContext == ItemDisplayContext.GUI ? 1.15D : 1.35D;
        poseStack.translate(0.5D, yOffset, 0.5D);
        float scale = displayContext == ItemDisplayContext.GUI ? 0.62F : 0.48F;
        poseStack.scale(scale, -scale, -scale);
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource,
                RenderType.armorCutoutNoCull(MimicYufengWingsModel.TEXTURE),
                false,
                stack.hasFoil()
        );
        wingsModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static MimicYufengWingsModel getModel() {
        if (model == null) {
            model = new MimicYufengWingsModel(Minecraft.getInstance().getEntityModels().bakeLayer(MimicYufengWingsModel.LAYER_LOCATION));
        }
        return model;
    }
}
