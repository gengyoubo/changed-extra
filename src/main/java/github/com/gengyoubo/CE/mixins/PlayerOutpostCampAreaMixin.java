package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.parkabird.changedsynergy.ai.PlayerOutpostData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CS wild settlements also respect the space reserved by a CE camp. */
@Mixin(value = PlayerOutpostData.class, remap = false)
public abstract class PlayerOutpostCampAreaMixin {
    @Inject(method = "isAreaClaimed", at = @At("HEAD"), cancellable = true)
    private static void changede$campArea(ServerLevel level, BlockPos pos, double radius, CallbackInfoReturnable<Boolean> ci) {
        if (LatexSettlementService.isAreaClaimed(level, pos, radius)) ci.setReturnValue(true);
    }
}
