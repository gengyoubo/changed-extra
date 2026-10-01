package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.synergy.CreatureInventoryBackup;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LatexSocialMemory.class, remap = false)
public abstract class LatexSocialInventoryBackupMixin {
    @Inject(method = "promoteNativePet", at = @At("HEAD"))
    private static void changede$backupBeforeTaming(ChangedEntity creature, ServerPlayer player, CallbackInfo ci) {
        CreatureInventoryBackup.beforePromotion(creature);
    }

    @Inject(method = "promoteNativePet", at = @At("RETURN"))
    private static void changede$preserveHandsAfterTaming(ChangedEntity creature, ServerPlayer player, CallbackInfo ci) {
        CreatureInventoryBackup.afterPromotion(creature);
    }
}
