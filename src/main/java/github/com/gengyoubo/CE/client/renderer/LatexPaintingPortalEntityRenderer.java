package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class LatexPaintingPortalEntityRenderer extends EntityRenderer<LatexPaintingPortalEntity> {

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
        if (!isViewingFront(entity)) {
            poseStack.pushPose();
            LatexPaintingPortalProjectionRenderer.renderBackCentered(poseStack, entity.getFacing());
            poseStack.popPose();
            return;
        }

        Level level = entity.level();
        ResourceLocation dimension = level.dimension().location();
        if (LatexPaintingPortalPreviewCache.shouldRequest(dimension, entity.blockPosition(), level.getGameTime())) {
            CENetwork.sendToServer(new RequestLatexPaintingPortalPreviewPacket(entity.blockPosition(), entity.getId()));
        }

        LatexPaintingPortalPreviewCache.Snapshot snapshot =
                LatexPaintingPortalPreviewCache.get(dimension, entity.blockPosition());
        boolean reversed = shouldReversePortalView(entity);

        poseStack.pushPose();
        LatexPaintingPortalProjectionRenderer.renderCentered(poseStack, bufferSource, entity.getFacing(), snapshot, reversed);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static boolean isViewingFront(LatexPaintingPortalEntity entity) {
        Vec3 camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 toCamera = camera.subtract(entity.position());
        return toCamera.x * entity.getFacing().getStepX() + toCamera.z * entity.getFacing().getStepZ() > 0.0D;
    }

    private static boolean shouldReversePortalView(LatexPaintingPortalEntity entity) {
        boolean reversed = entity.isRenderReversed();
        if (entity.level().dimension().equals(LatexPaintingPortalBlock.LATEX_SPACE)) {
            return !reversed;
        }
        return reversed;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull ResourceLocation getTextureLocation(@NotNull LatexPaintingPortalEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

}
