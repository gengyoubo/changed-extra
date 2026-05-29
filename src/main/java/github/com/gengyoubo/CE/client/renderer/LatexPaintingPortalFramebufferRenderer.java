package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LatexPaintingPortalFramebufferRenderer {
    private static final int TARGET_SIZE = 1024;
    private static final float REMOTE_RENDER_SCALE = 0.08F;
    private static final Camera PORTAL_CAMERA = new Camera();
    private static Method cameraSetPositionMethod;
    private static boolean renderingPortalFrame;

    private LatexPaintingPortalFramebufferRenderer() {
    }

    public static boolean isRenderingPortalFrame() {
        return renderingPortalFrame;
    }

    public static void renderCentered(PoseStack poseStack, Direction facing, LatexPaintingPortalEntity entity, float partialTick,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        renderCentered(poseStack, facing, entity, partialTick, snapshot, false);
    }

    public static void renderCentered(PoseStack poseStack, Direction facing, LatexPaintingPortalEntity entity, float partialTick,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot, boolean reversed) {
        RenderTarget captured = LatexPortalRenderManager.getTarget(entity);
        if (captured == null) {
            return;
        }

        renderCentered(poseStack, facing, captured, reversed);
    }

    public static void renderCentered(PoseStack poseStack, Direction facing, RenderTarget captured) {
        renderCentered(poseStack, facing, captured, false);
    }

    public static void renderCentered(PoseStack poseStack, Direction facing, RenderTarget captured, boolean reversed) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(LatexPaintingPortalProjectionRenderer.rotationFor(facing)));
        poseStack.translate(0.0D, 0.0D, -0.015D);
        if (reversed) {
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, captured.getColorTextureId());
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        drawTexturedQuad(matrix, bufferBuilder, -0.48F, -0.48F, 0.48F, 0.48F, 0.0F, 255, 255, 255, 255);
        drawTexturedQuad(matrix, bufferBuilder, -0.54F, -0.54F, 0.54F, -0.48F, 0.0F, 23, 18, 20, 255);
        drawTexturedQuad(matrix, bufferBuilder, -0.54F, 0.48F, 0.54F, 0.54F, 0.0F, 23, 18, 20, 255);
        drawTexturedQuad(matrix, bufferBuilder, -0.54F, -0.48F, -0.48F, 0.48F, 0.0F, 23, 18, 20, 255);
        drawTexturedQuad(matrix, bufferBuilder, 0.48F, -0.48F, 0.54F, 0.48F, 0.0F, 23, 18, 20, 255);

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    public static TextureTarget ensureTarget(TextureTarget target) {
        if (target == null || target.viewWidth != TARGET_SIZE || target.viewHeight != TARGET_SIZE) {
            if (target != null) {
                target.destroyBuffers();
            }
            TextureTarget newTarget = new TextureTarget(TARGET_SIZE, TARGET_SIZE, true, Minecraft.ON_OSX);
            newTarget.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            return newTarget;
        }

        return target;
    }

    public static void renderToTarget(TextureTarget target, LatexPaintingPortalEntity entity, float partialTick,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getCameraEntity() == null || renderingPortalFrame || target == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        int previousRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        renderingPortalFrame = true;
        boolean projectionBackedUp = false;
        try {
            target.bindWrite(true);
            target.clear(Minecraft.ON_OSX);
            RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight);

            RenderSystem.backupProjectionMatrix();
            projectionBackedUp = true;

            renderRemoteSnapshot(entity, snapshot);
        } finally {
            if (projectionBackedUp) {
                RenderSystem.restoreProjectionMatrix();
            }
            renderingPortalFrame = false;
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            main.bindWrite(false);
            RenderSystem.viewport(0, 0, main.viewWidth, main.viewHeight);
        }
    }

    private static void renderRemoteSnapshot(LatexPaintingPortalEntity entity, LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-1.0F, 1.0F, -1.0F, 1.0F, -1.0F, 1.0F);
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            drawSnapshotToFramebuffer(entity.getTargetFacing(), snapshot);
        } finally {
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void drawSnapshotToFramebuffer(Direction targetFacing, LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f matrix = new Matrix4f();
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawColorQuad(matrix, bufferBuilder, -1.0F, -1.0F, 1.0F, 1.0F, 0.0F, 32, 48, 72, 255);
        if (snapshot == null || snapshot.blocks().isEmpty()) {
            drawColorQuad(matrix, bufferBuilder, -0.72F, -0.72F, 0.72F, 0.72F, -0.02F, 180, 40, 40, 255);
        } else {
            drawRemoteBlocks(bufferBuilder, targetFacing, snapshot);
        }

        drawColorQuad(matrix, bufferBuilder, -1.0F, -1.0F, 1.0F, -0.92F, -0.04F, 22, 18, 20, 255);
        drawColorQuad(matrix, bufferBuilder, -1.0F, 0.92F, 1.0F, 1.0F, -0.04F, 22, 18, 20, 255);
        drawColorQuad(matrix, bufferBuilder, -1.0F, -0.92F, -0.92F, 0.92F, -0.04F, 22, 18, 20, 255);
        drawColorQuad(matrix, bufferBuilder, 0.92F, -0.92F, 1.0F, 0.92F, -0.04F, 22, 18, 20, 255);

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawRemoteBlocks(BufferBuilder bufferBuilder, Direction targetFacing, LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        List<LatexPaintingPortalPreviewCache.PreviewBlock> blocks = new ArrayList<>(snapshot.blocks());
        blocks.sort(Comparator.<LatexPaintingPortalPreviewCache.PreviewBlock>comparingDouble(
                block -> depthForBlock(block, targetFacing)
        ).reversed());

        Matrix4f matrix = new Matrix4f();
        for (LatexPaintingPortalPreviewCache.PreviewBlock block : blocks) {
            drawRemoteBlock(matrix, bufferBuilder, targetFacing, block);
        }
    }

    private static double depthForBlock(LatexPaintingPortalPreviewCache.PreviewBlock block, Direction targetFacing) {
        int forward = block.dx() * targetFacing.getStepX() + block.dz() * targetFacing.getStepZ();
        return forward * 1.8D + block.dy() * 0.25D;
    }

    private static void drawRemoteBlock(Matrix4f matrix, BufferBuilder bufferBuilder, Direction targetFacing,
                                        LatexPaintingPortalPreviewCache.PreviewBlock block) {
        int base = shade(colorFor(block.state()), block.dy());
        Direction right = targetFacing.getClockWise();
        float x = (block.dx() * right.getStepX() + block.dz() * right.getStepZ()) * REMOTE_RENDER_SCALE;
        float y = (block.dy() - 2.0F) * REMOTE_RENDER_SCALE;
        float z = (block.dx() * targetFacing.getStepX() + block.dz() * targetFacing.getStepZ()) * REMOTE_RENDER_SCALE;
        float s = REMOTE_RENDER_SCALE;

        Vector3f p000 = project(x - s / 2.0F, y - s / 2.0F, z - s / 2.0F);
        Vector3f p100 = project(x + s / 2.0F, y - s / 2.0F, z - s / 2.0F);
        Vector3f p110 = project(x + s / 2.0F, y + s / 2.0F, z - s / 2.0F);
        Vector3f p010 = project(x - s / 2.0F, y + s / 2.0F, z - s / 2.0F);
        Vector3f p001 = project(x - s / 2.0F, y - s / 2.0F, z + s / 2.0F);
        Vector3f p101 = project(x + s / 2.0F, y - s / 2.0F, z + s / 2.0F);
        Vector3f p111 = project(x + s / 2.0F, y + s / 2.0F, z + s / 2.0F);
        Vector3f p011 = project(x - s / 2.0F, y + s / 2.0F, z + s / 2.0F);

        drawProjectedQuad(matrix, bufferBuilder, p010, p110, p111, p011, base, 32);
        drawProjectedQuad(matrix, bufferBuilder, p000, p010, p011, p001, base, -18);
        drawProjectedQuad(matrix, bufferBuilder, p100, p101, p111, p110, base, -32);
        drawProjectedQuad(matrix, bufferBuilder, p001, p011, p111, p101, base, -8);
        drawProjectedQuad(matrix, bufferBuilder, p000, p100, p110, p010, base, -42);
    }

    private static Vector3f project(float x, float y, float z) {
        float yaw = (float) Math.toRadians(35.0D);
        float pitch = (float) Math.toRadians(26.0D);

        float yawX = (float) (x * Math.cos(yaw) - z * Math.sin(yaw));
        float yawZ = (float) (x * Math.sin(yaw) + z * Math.cos(yaw));
        float pitchY = (float) (y * Math.cos(pitch) - yawZ * Math.sin(pitch));
        float pitchZ = (float) (y * Math.sin(pitch) + yawZ * Math.cos(pitch));

        float distance = 3.6F;
        float perspective = distance / Math.max(0.7F, distance + pitchZ);
        return new Vector3f(yawX * perspective, pitchY * perspective, -0.05F + pitchZ * 0.001F);
    }

    private static void drawProjectedQuad(Matrix4f matrix, BufferBuilder bufferBuilder, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                          int color, int lightOffset) {
        int shaded = pack(clamp(red(color) + lightOffset), clamp(green(color) + lightOffset), clamp(blue(color) + lightOffset));
        drawProjectedQuadRaw(matrix, bufferBuilder, a, b, c, d, red(shaded), green(shaded), blue(shaded), 255);
    }

    private static void drawProjectedQuadRaw(Matrix4f matrix, BufferBuilder bufferBuilder, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                             int r, int g, int bColor, int alpha) {
        bufferBuilder.vertex(matrix, a.x(), a.y(), a.z()).color(r, g, bColor, alpha).endVertex();
        bufferBuilder.vertex(matrix, b.x(), b.y(), b.z()).color(r, g, bColor, alpha).endVertex();
        bufferBuilder.vertex(matrix, c.x(), c.y(), c.z()).color(r, g, bColor, alpha).endVertex();
        bufferBuilder.vertex(matrix, d.x(), d.y(), d.z()).color(r, g, bColor, alpha).endVertex();
    }

    private static int colorFor(BlockState state) {
        if (state == null || state.isAir()) {
            return 0xD4DCE5;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = id == null ? "" : id.toString();
        if (path.contains("dark_latex")) {
            if (path.contains("leaves")) {
                return 0x273127;
            }
            if (path.contains("log")) {
                return 0x352C29;
            }
            if (path.contains("stone") || path.contains("cobble")) {
                return 0x343434;
            }
            return 0x16181C;
        }
        if (path.contains("white_latex") || path.contains("pure_white")) {
            if (path.contains("leaves")) {
                return 0xE7F1E6;
            }
            if (path.contains("log")) {
                return 0xDCD3C8;
            }
            if (path.contains("stone") || path.contains("cobble")) {
                return 0xDBDBDB;
            }
            return 0xF1F1EE;
        }
        if (path.contains("water") || path.contains("ocean")) {
            return 0x315B86;
        }
        if (path.contains("grass") || path.contains("leaves")) {
            return 0x567F47;
        }
        if (path.contains("dirt")) {
            return 0x7A5637;
        }
        if (path.contains("stone") || path.contains("cobble")) {
            return 0x777777;
        }
        if (path.contains("planks") || path.contains("log")) {
            return 0xA17447;
        }
        return 0x8D8D8D;
    }

    private static int shade(int color, int dy) {
        int light = Math.max(-28, Math.min(34, dy * 3));
        return pack(clamp(red(color) + light), clamp(green(color) + light), clamp(blue(color) + light));
    }

    private static int pack(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int red(int color) {
        return color >> 16 & 255;
    }

    private static int green(int color) {
        return color >> 8 & 255;
    }

    private static int blue(int color) {
        return color & 255;
    }

    private static void setupPortalCamera(Entity cameraEntity, LatexPaintingPortalEntity portal, float partialTick) {
        PORTAL_CAMERA.setup(portal.level(), cameraEntity, false, false, partialTick);
        Direction facing = portal.getFacing();

        Vec3 viewerOffset = cameraEntity.getEyePosition(partialTick).subtract(portal.position());
        Vec3 targetCenter = Vec3.atBottomCenterOf(portal.getTargetPos())
                .add(0.0D, LatexPaintingPortalEntity.PORTAL_HEIGHT / 2.0D, 0.0D);
        Vec3 cameraPos = targetCenter.add(viewerOffset);

        PORTAL_CAMERA.setAnglesInternal(facing.toYRot(), 0.0F);
        setCameraPosition(cameraPos);
    }

    private static void setCameraPosition(Vec3 position) {
        try {
            if (cameraSetPositionMethod == null) {
                cameraSetPositionMethod = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                cameraSetPositionMethod.setAccessible(true);
            }
            cameraSetPositionMethod.invoke(PORTAL_CAMERA, position);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to move latex painting portal camera", exception);
        }
    }

    private static void drawTexturedQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                         float z, int r, int g, int b, int a) {
        bufferBuilder.vertex(matrix, x1, y1, z).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, z).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, z).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x1, y2, z).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
    }

    private static void drawColorQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                      float z, int r, int g, int b, int a) {
        bufferBuilder.vertex(matrix, x1, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x1, y2, z).color(r, g, b, a).endVertex();
    }

}
