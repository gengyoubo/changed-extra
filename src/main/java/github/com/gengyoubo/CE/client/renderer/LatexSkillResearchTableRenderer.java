package github.com.gengyoubo.CE.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.util.Mth;

/** Vanilla-sized research book above the twelve-pixel-high pedestal. */
public final class LatexSkillResearchTableRenderer implements BlockEntityRenderer<LatexSkillResearchBlockEntity> {
    private final BookModel book;

    public LatexSkillResearchTableRenderer(BlockEntityRendererProvider.Context context) {
        book=new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override public void render(LatexSkillResearchBlockEntity station,float partialTick,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        float time=station.bookTime+partialTick;
        pose.pushPose();
        pose.translate(0.5,0.85+Mth.sin(time*0.1F)*0.01,0.5);
        pose.mulPose(Axis.YP.rotation(-Mth.lerp(partialTick,station.previousBookRotation,station.bookRotation)));
        pose.mulPose(Axis.ZP.rotationDegrees(80));
        float flip=Mth.lerp(partialTick,station.previousBookFlip,station.bookFlip);
        book.setupAnim(time,Mth.clamp(Mth.frac(flip+0.25F)*1.6F-0.3F,0,1),
                Mth.clamp(Mth.frac(flip+0.75F)*1.6F-0.3F,0,1),
                Mth.lerp(partialTick,station.previousBookOpen,station.bookOpen));
        book.render(pose,EnchantTableRenderer.BOOK_LOCATION.buffer(buffers,RenderType::entitySolid),
                light,overlay,1,1,1,1);
        pose.popPose();
    }
}
