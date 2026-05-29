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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

public class LatexPaintingPortalProjectionRenderer {
    private static final int GRID_SIZE = 65;
    private static final float INNER_MIN = -0.45F;
    private static final float INNER_MAX = 0.45F;
    private static final float CELL_SIZE = (INNER_MAX - INNER_MIN) / GRID_SIZE;

    private LatexPaintingPortalProjectionRenderer() {
    }

    public static void renderCentered(PoseStack poseStack, MultiBufferSource bufferSource, Direction facing,
                                      LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.translate(0.0D, 0.0D, -0.031D);

        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawQuad(matrix, bufferBuilder, -0.48F, -0.48F, 0.48F, 0.48F, 0.000F, 6, 8, 10, 255);

        Cell[] projected = flatten(snapshot);
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                Cell cell = projected[z * GRID_SIZE + x];
                int color = colorFor(cell == null ? null : cell.state(), x, z);
                color = shade(color, cell == null ? 0 : cell.dy());
                float x1 = INNER_MIN + x * CELL_SIZE;
                float x2 = x1 + CELL_SIZE;
                float y2 = INNER_MAX - z * CELL_SIZE;
                float y1 = y2 - CELL_SIZE;
                drawQuad(matrix, bufferBuilder, x1, y1, x2, y2, -0.012F, red(color), green(color), blue(color), 255);
            }
        }

        drawFrame(matrix, bufferBuilder);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static Cell[] flatten(LatexPaintingPortalPreviewCache.Snapshot snapshot) {
        Cell[] states = new Cell[GRID_SIZE * GRID_SIZE];
        int[] heights = new int[GRID_SIZE * GRID_SIZE];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = Integer.MIN_VALUE;
        }

        if (snapshot == null || snapshot.blocks().isEmpty()) {
            return states;
        }

        for (LatexPaintingPortalPreviewCache.PreviewBlock block : snapshot.blocks()) {
            int x = block.dx() + GRID_SIZE / 2;
            int z = block.dz() + GRID_SIZE / 2;
            if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
                continue;
            }

            int index = z * GRID_SIZE + x;
            if (block.dy() >= heights[index]) {
                heights[index] = block.dy();
                states[index] = new Cell(block.state(), block.dy());
            }
        }
        return states;
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

    private static int pack(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static void drawQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                 float z, int r, int g, int b, int a) {
        drawColorQuad(matrix, bufferBuilder, x1, y1, x2, y2, z, r, g, b, a);
        drawColorQuad(matrix, bufferBuilder, x2, y1, x1, y2, z, r, g, b, a);
    }

    private static void drawColorQuad(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2,
                                      float z, int r, int g, int b, int a) {
        bufferBuilder.vertex(matrix, x1, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x1, y2, z).color(r, g, b, a).endVertex();
    }

    private static void drawFrame(Matrix4f matrix, BufferBuilder bufferBuilder) {
        drawQuad(matrix, bufferBuilder, -0.54F, -0.54F, 0.54F, -0.48F, -0.024F, 23, 18, 20, 255);
        drawQuad(matrix, bufferBuilder, -0.54F, 0.48F, 0.54F, 0.54F, -0.024F, 23, 18, 20, 255);
        drawQuad(matrix, bufferBuilder, -0.54F, -0.48F, -0.48F, 0.48F, -0.024F, 23, 18, 20, 255);
        drawQuad(matrix, bufferBuilder, 0.48F, -0.48F, 0.54F, 0.48F, -0.024F, 23, 18, 20, 255);
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

    private record Cell(BlockState state, int dy) {
    }
}
