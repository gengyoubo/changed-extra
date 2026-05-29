package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import net.ltxprogrammer.changed.block.WhiteLatexBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WhiteLatexBlock.class, remap = false)
public class WhiteLatexBlockLatexSpaceMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void changede$keepWhiteLatexSolidInLatexSpace(BlockState state, BlockGetter level, BlockPos pos,
                                                          CollisionContext context,
                                                          CallbackInfoReturnable<VoxelShape> cir) {
        if (!(level instanceof Level actualLevel) || !actualLevel.dimension().equals(LatexPaintingPortalBlock.LATEX_SPACE)) {
            return;
        }

        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof LivingEntity living
                && living.fallDistance > 3.0F) {
            cir.setReturnValue(Shapes.block());
        }
    }
}
