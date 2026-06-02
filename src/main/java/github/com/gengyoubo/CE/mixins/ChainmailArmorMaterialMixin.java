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
        if ((Object) this != ArmorMaterials.CHAIN) {
            return;
        }

        int baseDurability = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        cir.setReturnValue(baseDurability * 24);
    }

    @Inject(method = "getDefenseForType", at = @At("HEAD"), cancellable = true)
    private void changede$chainmailDefenseBetweenIronAndDiamond(ArmorItem.Type type, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != ArmorMaterials.CHAIN) {
            return;
        }

        cir.setReturnValue(switch (type) {
            case HELMET -> 2;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
            case BOOTS -> 2;
        });
    }

    @Inject(method = "getToughness", at = @At("HEAD"), cancellable = true)
    private void changede$chainmailToughnessBetweenIronAndDiamond(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == ArmorMaterials.CHAIN) {
            cir.setReturnValue(1.0F);
        }
    }
}
