package github.com.gengyoubo.LP.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MimicYufengWingsItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MimicYufengWingsItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        float ageInTicks = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + minecraft.getFrameTime();
        MimicYufengWingsRenderer.renderItem(stack, displayContext, poseStack, bufferSource, packedLight, ageInTicks);
    }
}
