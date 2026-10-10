package github.com.gengyoubo.CE.compat.create.burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Small voxel mascot; no world entity, entity inventory or AI is created for rendering. */
public final class LatexBurnerClient implements BlockEntityRenderer<LatexBurnerBlockEntity> {
    public LatexBurnerClient(BlockEntityRendererProvider.Context context) {}
    static void initialize(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(LatexBurnerCompat.ENTITY.get(), LatexBurnerClient::new));
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(LatexBurnerCompat.BLOCK.get(), RenderType.cutout())));
    }
    @Override public void render(LatexBurnerBlockEntity burner, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (!burner.hasCreature() || burner.profile() == null || burner.getLevel() == null) return;
        boolean white = burner.profile().latex().equals("white");
        Block latex = white ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE;
        float time = burner.getLevel().getGameTime() + partial;
        float pulse = (float) Math.sin(time * (burner.heat() == HeatLevel.SEETHING ? .23 : .08)) * .012f;
        pose.pushPose();
        pose.translate(.5, .22 + pulse, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - burner.getBlockState().getValue(LatexBurnerBlock.FACING).toYRot()));
        cube(pose, buffer, light, overlay, latex, -.22f, 0, -.18f, .44f, .24f, .36f);
        cube(pose, buffer, light, overlay, latex, -.19f, .20f, -.19f, .38f, .31f, .38f);
        cube(pose, buffer, light, overlay, latex, -.18f, .46f, -.10f, .10f, .15f, .13f);
        cube(pose, buffer, light, overlay, latex, .08f, .46f, -.10f, .10f, .15f, .13f);
        Block eye = burner.heat() == HeatLevel.SEETHING ? Blocks.LIGHT_BLUE_CONCRETE : white ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE;
        cube(pose, buffer, light, overlay, eye, -.12f, .36f, -.201f, .055f, .055f, .02f);
        cube(pose, buffer, light, overlay, eye, .065f, .36f, -.201f, .055f, .055f, .02f);
        if (burner.creatureType().contains("yufeng") || burner.creatureType().contains("dragon")) {
            cube(pose, buffer, light, overlay, latex, -.32f, .14f, .02f, .10f, .30f, .15f);
            cube(pose, buffer, light, overlay, latex, .22f, .14f, .02f, .10f, .30f, .15f);
        }
        pose.popPose();
    }
    private static void cube(PoseStack pose, MultiBufferSource buffer, int light, int overlay, Block block,
                             float x, float y, float z, float width, float height, float depth) {
        pose.pushPose(); pose.translate(x, y, z); pose.scale(width, height, depth);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), pose, buffer, light, overlay);
        pose.popPose();
    }
}
