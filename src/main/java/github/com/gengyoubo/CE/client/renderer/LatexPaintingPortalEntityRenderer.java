package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LatexPaintingPortalEntityRenderer extends EntityRenderer<LatexPaintingPortalEntity> {
    public LatexPaintingPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull LatexPaintingPortalEntity entity, @NotNull Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(@NotNull LatexPaintingPortalEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (LatexPaintingPortalFramebufferRenderer.isRenderingPortalFrame()) {
            return;
        }

        Level level = entity.level();
        ResourceLocation dimension = level.dimension().location();
        if (LatexPaintingPortalPreviewCache.shouldRequest(dimension, entity.blockPosition(), level.getGameTime())) {
            CENetwork.sendToServer(new RequestLatexPaintingPortalPreviewPacket(entity.blockPosition(), entity.getId()));
        }

        LatexPaintingPortalPreviewCache.Snapshot snapshot =
                LatexPaintingPortalPreviewCache.get(dimension, entity.blockPosition());

        poseStack.pushPose();
        poseStack.scale((float) LatexPaintingPortalEntity.PORTAL_SIZE, (float) LatexPaintingPortalEntity.PORTAL_SIZE, 1.0F);
        LatexPaintingPortalFramebufferRenderer.renderCentered(poseStack, entity.getFacing(), entity, partialTick, snapshot);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LatexPaintingPortalEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
