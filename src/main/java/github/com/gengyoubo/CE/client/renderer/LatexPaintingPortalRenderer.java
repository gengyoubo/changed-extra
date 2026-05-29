package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import github.com.gengyoubo.CE.BlockEntity.LatexPaintingPortalBlockEntity;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LatexPaintingPortalRenderer implements BlockEntityRenderer<LatexPaintingPortalBlockEntity> {
    public LatexPaintingPortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull LatexPaintingPortalBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        ResourceLocation dimension = level.dimension().location();
        long gameTime = level.getGameTime();
        if (LatexPaintingPortalPreviewCache.shouldRequest(dimension, blockEntity.getBlockPos(), gameTime)) {
            CENetwork.sendToServer(new RequestLatexPaintingPortalPreviewPacket(blockEntity.getBlockPos()));
        }

        LatexPaintingPortalPreviewCache.Snapshot snapshot =
                LatexPaintingPortalPreviewCache.get(dimension, blockEntity.getBlockPos());

        BlockState portalState = blockEntity.getBlockState();
        Direction facing = portalState.hasProperty(LatexPaintingPortalBlock.FACING)
                ? portalState.getValue(LatexPaintingPortalBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        LatexPaintingPortalProjectionRenderer.renderCentered(poseStack, bufferSource, facing, snapshot);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull LatexPaintingPortalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

}
