package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.PatreonBenefitsFix;
import github.com.gengyoubo.fix.SpecialLatex;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.TagUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSpecialVariantMixin {
    @Unique
    private static final String SPECIAL_FORM_PREFIX = "special/form_";

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void changede$restoreSpecialVariant(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("TransfurVariant")) {
            return;
        }

        ResourceLocation variantId = TagUtil.getResourceLocation(tag, "TransfurVariant");
        if (!PatreonBenefitsFix.isSpecialFormId(variantId)) {
            return;
        }

        TransfurVariant<?> variant = PatreonBenefitsFix.resolveVariant(variantId);
        if (variant == null) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        ProcessTransfur.setPlayerTransfurVariant(self, variant, null, 1.0f, false, entity -> {
            if (tag.contains("Leash", 10)) {
                entity.getChangedEntity().setLeashInfoTag(tag.getCompound("Leash"));
            }
        });

        ProcessTransfur.getPlayerTransfurVariantSafe(self).ifPresent(instance -> {
            if (tag.contains("TransfurVariantData")) {
                instance.load(tag.getCompound("TransfurVariantData"));
            }

            if (instance.getChangedEntity() instanceof SpecialLatex specialLatex) {
                String path = variantId.getPath();
                if (path.startsWith(SPECIAL_FORM_PREFIX)) {
                    try {
                        specialLatex.setSpecialForm(UUID.fromString(path.substring(SPECIAL_FORM_PREFIX.length())));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        });
    }
}
