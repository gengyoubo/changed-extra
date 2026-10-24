package github.com.gengyoubo.CE.LP.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.client.model.MimicSirenTailModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MimicSirenTailRenderer extends BlockEntityWithoutLevelRenderer {
    private static MimicSirenTailModel model;

    public MimicSirenTailRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static MimicSirenTailModel getModel() {
        if (model == null) {
            model = new MimicSirenTailModel(Minecraft.getInstance().getEntityModels().bakeLayer(MimicSirenTailModel.LAYER_LOCATION));
        }
        return model;
    }

    public static MimicSirenTailModel getArmorModel(LivingEntity entity, HumanoidModel<?> original) {
        Minecraft minecraft = Minecraft.getInstance();
        MimicSirenTailModel tail = getModel();
        tail.setupAnim(entity, entity.walkAnimation.position(), entity.walkAnimation.speed(),
                entity.tickCount + minecraft.getFrameTime(), 0.0F, 0.0F);
        EntityModel<?> parent = original;
        if (minecraft.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer<?, ?> renderer) {
            parent = renderer.getModel();
        }
        tail.copyBodyPoseFrom(parent);
        return tail;
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context, @NotNull PoseStack poses,
                             @NotNull MultiBufferSource buffers, int light, int overlay) {
        MimicSirenTailModel tail = getModel();
        tail.setupItemPose();
        poses.pushPose();
        poses.translate(0.5D, 0.75D, 0.85D);
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45.0F));
        float scale = context == ItemDisplayContext.GUI ? 0.40F : 0.48F;
        poses.scale(scale, -scale, -scale);
        tail.renderToBuffer(poses, ItemRenderer.getArmorFoilBuffer(buffers,
                RenderType.armorCutoutNoCull(MimicSirenTailModel.TEXTURE), false, stack.hasFoil()),
                light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poses.popPose();
    }
}
