package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SwitchTransfurModeAbility;
import net.ltxprogrammer.changed.entity.TransfurMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SwitchTransfurModeAbility.class, remap = false)
public abstract class TransfurModeNoticeMixin {
    @Inject(method = "startUsing", at = @At("RETURN"))
    private void changede$explainMindlessAbsorption(IAbstractChangedEntity entity, CallbackInfo ci) {
        if (entity.getEntity() instanceof ServerPlayer
                && entity.getTransfurMode() == TransfurMode.REPLICATION
                && !ChangedSynergyConfig.COMMON.allowMindlessMobTransfur.get()) {
            // Keep Changed's mode selection in the action bar; put the rule explanation in chat.
            entity.displayClientMessage(Component.translatable("ability.changede.switch_transfur_mode.mindless_notice")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
    }
}
