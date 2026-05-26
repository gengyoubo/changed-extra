package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ltxprogrammer.changed.client.LatexCoveredBlocksRenderer;
import net.ltxprogrammer.changed.world.LatexCoverGetter;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LatexCoveredBlocksRenderer.class, remap = false)
public abstract class LatexCoveredBlocksRendererMixin {
    @Shadow @Final
    private ModelBlockRenderer modelRenderer;

    @Shadow @Final
    private static ThreadLocal<LatexCoverGetter> threadLocal;

    @Shadow
    private LatexCoveredBlocksRenderer.ModelSet getModelSet(BlockState blockState, LatexCoverState coverState) {
        throw new AssertionError();
    }

    @Shadow
    private int getLightColor(BlockAndTintGetter level, BlockPos blockPos) {
        throw new AssertionError();
    }

    @Shadow
    public abstract RenderType getRenderType(LatexCoverState coverState);

    @Inject(method = "wrappedTesselate", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$ensureThreadLocalCleanup(
            BlockAndTintGetter level,
            LatexCoverGetter latexCoverGetter,
            BlockPos blockPos,
            VertexConsumer bufferBuilder,
            BlockState blockState,
            LatexCoverState coverState,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LatexCoveredBlocksRenderer.ModelSet modelSet = this.getModelSet(blockState, coverState);
        if (blockState.isCollisionShapeFullBlock(level, blockPos)) {
            cir.setReturnValue(false);
            return;
        }

        int blockX0 = blockPos.getX() & 15;
        int blockY0 = blockPos.getY() & 15;
        int blockZ0 = blockPos.getZ() & 15;
        int lightColor = this.getLightColor(level, blockPos);
        boolean surfaceTop = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.UP) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.UP);
        boolean surfaceBottom = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.DOWN) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.DOWN);
        boolean surfaceNorth = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.NORTH) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.NORTH);
        boolean surfaceSouth = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.SOUTH) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.SOUTH);
        boolean surfaceEast = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.EAST) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.EAST);
        boolean surfaceWest = coverState.getProperties().contains(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.WEST) && coverState.getValue(net.ltxprogrammer.changed.entity.latex.SpreadingLatexType.WEST);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(blockX0, blockY0, blockZ0);
        long seed = coverState.getSeed(blockPos);
        RenderType renderType = this.getRenderType(coverState);

        threadLocal.set(latexCoverGetter);
        try {
            if (surfaceTop && modelSet.getModel(net.minecraft.core.Direction.UP) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.UP), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (surfaceBottom && modelSet.getModel(net.minecraft.core.Direction.DOWN) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.DOWN), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (surfaceNorth && modelSet.getModel(net.minecraft.core.Direction.NORTH) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.NORTH), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (surfaceSouth && modelSet.getModel(net.minecraft.core.Direction.SOUTH) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.SOUTH), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (surfaceEast && modelSet.getModel(net.minecraft.core.Direction.EAST) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.EAST), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (surfaceWest && modelSet.getModel(net.minecraft.core.Direction.WEST) != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getModel(net.minecraft.core.Direction.WEST), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
            if (modelSet.getExtraModel() != null) {
                this.modelRenderer.tesselateWithAO(level, modelSet.getExtraModel(), blockState, blockPos, poseStack, bufferBuilder, true, random, seed, lightColor, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
            }
        } finally {
            threadLocal.remove();
        }

        cir.setReturnValue(true);
    }
}
