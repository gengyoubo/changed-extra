package github.com.gengyoubo.CE.mixins;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorMaterials.class)
public abstract class ChainmailArmorMaterialMixin {
    @Inject(method = "getDurabilityForType", at = @At("HEAD"), cancellable = true)
    private void changede$chainmailDurabilityBetweenIronAndDiamond(ArmorItem.Type type, CallbackInfoReturnable<Integer> cir) {
        return;

    }

    @Inject(method = "getDefenseForType", at = @At("HEAD"), cancellable = true)
    private void changede$chainmailDefenseBetweenIronAndDiamond(ArmorItem.Type type, CallbackInfoReturnable<Integer> cir) {
        return;

    }

    @Inject(method = "getToughness", at = @At("HEAD"), cancellable = true)
    private void changede$chainmailToughnessBetweenIronAndDiamond(CallbackInfoReturnable<Float> cir) {
    }
}
