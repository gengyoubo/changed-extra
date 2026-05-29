package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.RequestLatexPaintingPortalPreviewPacket;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import github.com.gengyoubo.CE.changede;
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
    private static long lastDebugLogTick;

    public LatexPaintingPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull LatexPaintingPortalEntity entity, @NotNull Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z);
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
        logPortalRenderCoordinates(entity, snapshot);

        poseStack.pushPose();
        poseStack.scale((float) LatexPaintingPortalEntity.PORTAL_WIDTH, (float) LatexPaintingPortalEntity.PORTAL_HEIGHT, 1.0F);
        if (snapshot != null && !snapshot.blocks().isEmpty()) {
            LatexPaintingPortalProjectionRenderer.renderCentered(poseStack, bufferSource, entity.getFacing(), snapshot);
        } else if (LatexPortalRenderManager.getTarget(entity) == null) {
            LatexPaintingPortalProjectionRenderer.renderCentered(poseStack, bufferSource, entity.getFacing(), snapshot);
        } else {
            LatexPaintingPortalFramebufferRenderer.renderCentered(poseStack, entity.getFacing(), entity, partialTick, snapshot);
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LatexPaintingPortalEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static void logPortalRenderCoordinates(LatexPaintingPortalEntity entity, LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Level level = entity.level();
        long gameTime = level.getGameTime();
        if (gameTime - lastDebugLogTick < 100L) {
            return;
        }

        lastDebugLogTick = gameTime;
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;
        int minY = 0;
        int maxY = 0;
        int blocks = snapshot == null ? -1 : snapshot.blocks().size();
        if (snapshot != null && !snapshot.blocks().isEmpty()) {
            minX = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dx).min().orElse(0);
            maxX = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dx).max().orElse(0);
            minY = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dy).min().orElse(0);
            maxY = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dy).max().orElse(0);
            minZ = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dz).min().orElse(0);
            maxZ = snapshot.blocks().stream().mapToInt(LatexPaintingPortalPreviewCache.PreviewBlock::dz).max().orElse(0);
        }

        changede.LOGGER.warn(
                "Latex painting portal render coords: dimension={}, portalPos={}, facing={}, targetDimension={}, targetPos={}, targetFacing={}, localX={}..{}, localY={}..{}, localZ={}..{}, blocks={}",
                level.dimension().location(),
                entity.blockPosition(),
                entity.getFacing(),
                entity.getTargetDimension().location(),
                entity.getTargetPos(),
                entity.getTargetFacing(),
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ,
                blocks
        );
    }
}
