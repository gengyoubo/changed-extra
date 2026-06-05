package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache;
import github.com.gengyoubo.CE.changede;
import github.com.gengyoubo.CE.entity.LatexPaintingPortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

public class LatexPaintingPortalProjectionRenderer {
    private static final int GRID_SIZE = 129;
    private static final float INNER_MIN = -0.45F;
    private static final float INNER_MAX = 0.45F;
    private static final float CELL_SIZE = (INNER_MAX - INNER_MIN) / GRID_SIZE;
    private static final double PORTAL_HALF_WIDTH = 1.5D;
    private static final double PORTAL_HALF_HEIGHT = 1.5D;
    private static final double HORIZONTAL_VIEW_SPREAD = 0.95D;
    private static final double VERTICAL_VIEW_SPREAD = 0.68D;
    private static long lastDebugLogTick;

    private LatexPaintingPortalProjectionRenderer() {
    }

    public static void renderCentered(PoseStack poseStack, MultiBufferSource bufferSource, Direction facing,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        renderCentered(poseStack, bufferSource, facing, snapshot, false);
    }

    public static void renderCentered(PoseStack poseStack, MultiBufferSource bufferSource, Direction facing,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot, boolean reversed) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.scale((float) LatexPaintingPortalEntity.PORTAL_WIDTH, (float) LatexPaintingPortalEntity.PORTAL_HEIGHT, 1.0F);
        poseStack.translate(0.0D, 0.0D, -0.015D);
        if (reversed) {
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
        logSnapshotSize(snapshot);

        ProjectionBuffer projection = beginProjectionRender(poseStack);
        Matrix4f matrix = projection.matrix();
        BufferBuilder bufferBuilder = projection.bufferBuilder();

        int background = skyColorFor(snapshot, 0, 0);
        drawQuad(matrix, bufferBuilder, -0.48F, -0.48F, 0.48F, 0.48F, 0.000F, red(background), green(background), blue(background));

        Cell[] projected = flatten(snapshot);
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                Cell cell = projected[z * GRID_SIZE + x];
                int color = cell == null ? skyColorFor(snapshot, x, z) : colorFor(cell.state(), x, z);
                if (cell != null) {
                    color = shadeByDepth(color, cell.depth());
                }
                float x1 = INNER_MIN + x * CELL_SIZE;
                float x2 = x1 + CELL_SIZE;
                float y2 = INNER_MAX - z * CELL_SIZE;
                float y1 = y2 - CELL_SIZE;
                drawQuad(matrix, bufferBuilder, x1, y1, x2, y2, 0.000F, red(color), green(color), blue(color));
            }
        }

        endProjectionRender(poseStack, projection);
    }

    public static void renderBackCentered(PoseStack poseStack, Direction facing) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.scale((float) LatexPaintingPortalEntity.PORTAL_WIDTH, (float) LatexPaintingPortalEntity.PORTAL_HEIGHT, 1.0F);
        poseStack.translate(0.0D, 0.0D, 0.015D);

        ProjectionBuffer projection = beginProjectionRender(poseStack);
        Matrix4f matrix = projection.matrix();
        BufferBuilder bufferBuilder = projection.bufferBuilder();

        drawQuad(matrix, bufferBuilder, -0.48F, -0.48F, 0.48F, 0.48F, 0.000F, 92, 60, 34);
        drawQuad(matrix, bufferBuilder, -0.43F, -0.43F, 0.43F, 0.43F, 0.001F, 124, 83, 47);
        endProjectionRender(poseStack, projection);
    }

    private static ProjectionBuffer beginProjectionRender(PoseStack poseStack) {
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        return new ProjectionBuffer(matrix, bufferBuilder);
    }

    private static void endProjectionRender(PoseStack poseStack, ProjectionBuffer projection) {
        drawFrame(projection.matrix(), projection.bufferBuilder());
        BufferUploader.drawWithShader(projection.bufferBuilder().end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static int skyColorFor(LatexPaintingPortalPreviewCache.Snapshot snapshot, int x, int z) {
        int base = snapshot == null ? 0xD4DCE5 : snapshot.skyColor();
        int cloud = ((x * 31 + z * 17) & 31) == 0 ? 18 : 0;
        return pack(
                clamp(red(base) + cloud),
                clamp(green(base) + cloud),
                clamp(blue(base) + cloud)
        );
    }

    private static void logSnapshotSize(LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime - lastDebugLogTick < 100L) {
            return;
        }

        lastDebugLogTick = gameTime;
        changede.LOGGER.warn("Rendering latex painting portal projection, blocks={}", snapshot == null ? -1 : snapshot.blocks().size());
    }

    private static Cell[] flatten(LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Cell[] states = new Cell[GRID_SIZE * GRID_SIZE];
        int[] depths = new int[GRID_SIZE * GRID_SIZE];
        Arrays.fill(depths, Integer.MAX_VALUE);

        if (snapshot == null || snapshot.blocks().isEmpty()) {
            return states;
        }

        for (LatexPaintingPortalPreviewCache.PreviewBlock block : snapshot.blocks()) {
            int x;
            int y;
            if (block.dz() > 0) {
                double horizontalLimit = PORTAL_HALF_WIDTH + block.dz() * HORIZONTAL_VIEW_SPREAD;
                double verticalLimit = PORTAL_HALF_HEIGHT + block.dz() * VERTICAL_VIEW_SPREAD;
                double projectedX = block.dx() / horizontalLimit;
                double projectedY = block.dy() / verticalLimit;
                if (Math.abs(projectedX) > 1.0D || Math.abs(projectedY) > 1.0D) {
                    continue;
                }

                x = (int)Math.round((projectedX + 1.0D) * 0.5D * (GRID_SIZE - 1));
                y = (int)Math.round((1.0D - projectedY) * 0.5D * (GRID_SIZE - 1));
            } else {
                x = block.dx() + GRID_SIZE / 2;
                y = GRID_SIZE / 2 - block.dy();
            }

            splatBlock(states, depths, block, x, y);
        }
        return states;
    }

    private static void splatBlock(Cell[] states, int[] depths, LatexPaintingPortalPreviewCache.PreviewBlock block, int centerX, int centerY) {
        int radius = projectedBlockRadius(block.dz());
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            if (y < 0 || y >= GRID_SIZE) {
                continue;
            }

            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (x < 0 || x >= GRID_SIZE) {
                    continue;
                }

                int index = y * GRID_SIZE + x;
                if (block.dz() < depths[index]) {
                    depths[index] = block.dz();
                    states[index] = new Cell(block.state(), block.dz());
                }
            }
        }
    }

    private static int projectedBlockRadius(int depth) {
        double horizontalLimit = PORTAL_HALF_WIDTH + Math.max(1, depth) * HORIZONTAL_VIEW_SPREAD;
        int diameter = (int)Math.ceil((GRID_SIZE - 1) / (horizontalLimit * 2.0D));
        return Math.max(2, Math.min(8, diameter / 2));
    }

    private static int colorFor(BlockState state, int x, int z) {
        if (state == null || state.isAir()) {
            return ((x + z) & 1) == 0 ? 0xF0F4F7 : 0xCED5DD;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = id == null ? "" : id.toString();
        if (path.contains("dark_latex")) {
            if (path.contains("leaves")) {
                return 0x242C24;
            }
            if (path.contains("log")) {
                return 0x2F2724;
            }
            if (path.contains("stone") || path.contains("cobble")) {
                return 0x303030;
            }
            return 0x17191C;
        }
        if (path.contains("white_latex") || path.contains("pure_white")) {
            if (path.contains("leaves")) {
                return 0xE7EFE3;
            }
            if (path.contains("log")) {
                return 0xD7D0C6;
            }
            if (path.contains("stone") || path.contains("cobble")) {
                return 0xD8D8D8;
            }
            return 0xF2F2EF;
        }
        if (path.contains("water") || path.contains("ocean")) {
            return 0x315A82;
        }
        if (path.contains("grass") || path.contains("leaves")) {
            return 0x557C45;
        }
        if (path.contains("dirt")) {
            return 0x7A5637;
        }
        if (path.contains("stone") || path.contains("cobble")) {
            return 0x777777;
        }
        if (path.contains("planks") || path.contains("log")) {
            return 0x9B7146;
        }
        return 0x8D8D8D;
    }

    private static int shade(int color, int dy) {
        int light = Math.max(-28, Math.min(34, dy * 3));
        return pack(
                clamp(red(color) + light),
                clamp(green(color) + light),
                clamp(blue(color) + light)
        );
    }

    private static int shadeByDepth(int color, int depth) {
        int light = Math.max(-36, Math.min(24, 24 - depth * 2));
        return pack(
                clamp(red(color) + light),
                clamp(green(color) + light),
                clamp(blue(color) + light)
        );
    }

    private static int pack(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static void drawQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                 float z, int r, int g, int b) {
        drawColorQuad(matrix, bufferBuilder, x1, y1, x2, y2, z, r, g, b, 255);
        drawColorQuad(matrix, bufferBuilder, x2, y1, x1, y2, z, r, g, b, 255);
    }

    private static void drawColorQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                      float z, int r, int g, int b, int a) {
        bufferBuilder.vertex(matrix, x1, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x1, y2, z).color(r, g, b, a).endVertex();
    }

    private static void drawFrame(Matrix4f matrix, BufferBuilder bufferBuilder) {
        drawQuad(matrix, bufferBuilder, -0.54F, -0.54F, 0.54F, -0.48F, 0.000F, 23, 18, 20);
        drawQuad(matrix, bufferBuilder, -0.54F, 0.48F, 0.54F, 0.54F, 0.000F, 23, 18, 20);
        drawQuad(matrix, bufferBuilder, -0.54F, -0.48F, -0.48F, 0.48F, 0.000F, 23, 18, 20);
        drawQuad(matrix, bufferBuilder, 0.48F, -0.48F, 0.54F, 0.48F, 0.000F, 23, 18, 20);
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

    public static float rotationFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    private record Cell(BlockState state, int depth) {
    }

    private record ProjectionBuffer(Matrix4f matrix, BufferBuilder bufferBuilder) {
    }
}
