package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementService;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.parkabird.changedsynergy.ai.CreatureMorphContinuity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreatureMorphContinuity.class, remap = false)
public abstract class CreatureMorphCampStateMixin {
    @Inject(method = "transferInternal", at = @At("TAIL"))
    private static void changede$transferCamp(ChangedEntity previous, ChangedEntity replacement, ServerLevel level, CallbackInfo ci) {
        LatexSettlementService.transfer(previous, replacement, level);
    }
}
