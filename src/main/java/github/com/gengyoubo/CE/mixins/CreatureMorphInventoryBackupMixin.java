package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.synergy.CreatureInventoryBackup;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.parkabird.changedsynergy.ai.CreatureMorphContinuity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreatureMorphContinuity.class, remap = false)
public abstract class CreatureMorphInventoryBackupMixin {
    @Inject(method = "transferInternal", at = @At("HEAD"))
    private static void changede$backupBeforeMorph(ChangedEntity previous, ChangedEntity replacement,
                                                   ServerLevel level, CallbackInfo ci) {
        CreatureInventoryBackup.checkpoint(previous, "before_morph");
    }

    @Inject(method = "transferInternal", at = @At("TAIL"))
    private static void changede$transferInventoryBackups(ChangedEntity previous, ChangedEntity replacement,
                                                         ServerLevel level, CallbackInfo ci) {
        CreatureInventoryBackup.transfer(previous, replacement);
    }
}
