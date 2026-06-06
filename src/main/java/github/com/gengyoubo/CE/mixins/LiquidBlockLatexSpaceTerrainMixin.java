package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.events.LatexSpaceTerrainEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public class LiquidBlockLatexSpaceTerrainMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void changede$skipTerrainFluidOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
        if (LatexSpaceTerrainEvents.shouldSkipLatexSpaceFluidOnPlace()) {
            ci.cancel();
        }
    }
}
