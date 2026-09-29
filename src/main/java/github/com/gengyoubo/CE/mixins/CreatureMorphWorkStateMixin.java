package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.maid.LatexMaidCompat;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.parkabird.changedsynergy.ai.CreatureMorphContinuity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries CE's work assignment through Changed: Synergy's species replacement. */
@Mixin(value = CreatureMorphContinuity.class, remap = false)
public abstract class CreatureMorphWorkStateMixin {
    @Inject(method = "transferInternal", at = @At("TAIL"))
    private static void changede$transferWorkState(ChangedEntity previous, ChangedEntity replacement,
                                                    ServerLevel level, CallbackInfo ci) {
        LatexMaidCompat.transferWorkState(previous, replacement);
    }
}
