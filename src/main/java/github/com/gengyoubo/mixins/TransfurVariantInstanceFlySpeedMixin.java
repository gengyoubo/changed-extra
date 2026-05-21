package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.SpecialLatexFix.OldTransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransfurVariantInstance.class, remap = false)
public abstract class TransfurVariantInstanceFlySpeedMixin {
    @Shadow protected TransfurVariant<?> parent;
    @Shadow private Player host;

    @Unique
    private float changede$defaultFlyingSpeed = Float.NaN;

    @Inject(method = "tickFlying", at = @At("HEAD"))
    private void changede$applyVariantFlySpeed(CallbackInfo ci) {
        if (this.host == null) return;

        Abilities abilities = this.host.getAbilities();
        if (Float.isNaN(this.changede$defaultFlyingSpeed)) {
            this.changede$defaultFlyingSpeed = abilities.getFlyingSpeed();
        }

        float targetSpeed = this.changede$defaultFlyingSpeed;
        if (this.parent instanceof OldTransfurVariant<?> oldVariant) {
            float multiplier = oldVariant.flySpeed();
            if (multiplier > 0.0F) {
                targetSpeed = this.changede$defaultFlyingSpeed * multiplier;
            }
        }

        if (Float.compare(abilities.getFlyingSpeed(), targetSpeed) != 0) {
            abilities.setFlyingSpeed(targetSpeed);
        }
    }

    @Inject(method = "unhookAll", at = @At("HEAD"))
    private void changede$restoreDefaultFlySpeed(Player player, CallbackInfo ci) {
        if (player == null || Float.isNaN(this.changede$defaultFlyingSpeed)) return;
        player.getAbilities().setFlyingSpeed(this.changede$defaultFlyingSpeed);
    }
}
