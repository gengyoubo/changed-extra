package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.PatreonBenefitsFix;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.util.TagUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSpecialVariantMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void changede$normalizeSpecialVariantBeforeRead(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("TransfurVariant")) {
            return;
        }

        ResourceLocation variantId = TagUtil.getResourceLocation(tag, "TransfurVariant");
        if (!PatreonBenefitsFix.isSpecialFormId(variantId)) {
            return;
        }

        tag.putString(PatreonBenefitsFix.PENDING_SPECIAL_FORM_ID_TAG, variantId.toString());
        ResourceLocation safeVariantId = ChangedTransfurVariants.FALLBACK_VARIANT.get().getFormId();
        if (safeVariantId != null) {
            tag.putString("TransfurVariant", safeVariantId.toString());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void changede$queueSpecialVariantRestore(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("TransfurVariant") && !tag.contains(PatreonBenefitsFix.PENDING_SPECIAL_FORM_ID_TAG)) {
            return;
        }

        ResourceLocation variantId = tag.contains(PatreonBenefitsFix.PENDING_SPECIAL_FORM_ID_TAG)
                ? ResourceLocation.tryParse(tag.getString(PatreonBenefitsFix.PENDING_SPECIAL_FORM_ID_TAG))
                : TagUtil.getResourceLocation(tag, "TransfurVariant");
        if (!PatreonBenefitsFix.isSpecialFormId(variantId)) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        CompoundTag persistentData = self.getPersistentData();
        persistentData.putString(PatreonBenefitsFix.PENDING_SPECIAL_FORM_ID_TAG, variantId.toString());
        persistentData.putInt(PatreonBenefitsFix.PENDING_SPECIAL_FORM_CONFIRM_TICKS_TAG, 40);
        if (tag.contains("TransfurVariantData")) {
            persistentData.put(PatreonBenefitsFix.PENDING_SPECIAL_FORM_DATA_TAG, tag.getCompound("TransfurVariantData").copy());
        }
    }
}
