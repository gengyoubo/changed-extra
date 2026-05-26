package github.com.gengyoubo.CE.LP.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MimicYufengWingsItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MimicYufengWingsItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack,
                             @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        float ageInTicks = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + minecraft.getFrameTime();
        MimicYufengWingsRenderer.renderItem(stack, displayContext, poseStack, bufferSource, packedLight, ageInTicks);
    }
}
