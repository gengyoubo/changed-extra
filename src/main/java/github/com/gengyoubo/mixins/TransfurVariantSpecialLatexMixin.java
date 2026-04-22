package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.SpecialLatex.PatreonBenefitsFix;
import github.com.gengyoubo.fix.SpecialLatex.SpecialLatex;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = TransfurVariant.class, remap = false)
public abstract class TransfurVariantSpecialLatexMixin {
    @Unique
    private static final String SPECIAL_FORM_PREFIX = "special/form_";

    @Unique
    private static void changed_extra$bindSpecialForm(SpecialLatex specialLatex, ResourceLocation formId, UUID fallbackUuid) {
        if (PatreonBenefitsFix.isSpecialFormId(formId)) {
            String path = formId.getPath();
            if (path.startsWith(SPECIAL_FORM_PREFIX)) {
                try {
                    specialLatex.setSpecialForm(UUID.fromString(path.substring(SPECIAL_FORM_PREFIX.length())));
                    return;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (fallbackUuid != null) {
            specialLatex.setSpecialForm(fallbackUuid);
        }
    }

    @Inject(method = "generateForm", at = @At("RETURN"))
    private void changede$bindSpecialFormOnGenerateForm(Player player, Level level, CallbackInfoReturnable<ChangedEntity> cir) {
        ChangedEntity entity = cir.getReturnValue();
        if (entity instanceof SpecialLatex specialLatex) {
            changed_extra$bindSpecialForm(specialLatex, ((TransfurVariant<?>)(Object)this).getFormId(), player.getUUID());
        }
    }

    @Inject(method = "spawnAtEntity", at = @At("RETURN"))
    private void changede$bindSpecialFormOnSpawnAtEntity(LivingEntity entity, CallbackInfoReturnable<ChangedEntity> cir) {
        ChangedEntity changedEntity = cir.getReturnValue();
        if (changedEntity instanceof SpecialLatex specialLatex) {
            UUID fallbackUuid = entity instanceof Player player ? player.getUUID() : null;
            changed_extra$bindSpecialForm(specialLatex, ((TransfurVariant<?>)(Object)this).getFormId(), fallbackUuid);
        }
    }
}
