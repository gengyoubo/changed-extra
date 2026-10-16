package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class LatexSpearShieldMixin {
    @Inject(method = "blockUsingShield", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;blockUsingShield(Lnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void changede$useActualWeapon(LivingEntity attacker, CallbackInfo ci) {
        // MC-304593: 1.20.1 asks about the main-hand axe even when the off-hand spear hit.
        // Keep shield knockback, then skip only the spear hit's main-hand weapon check.
        // Leave canDisableShield available to other mods' redirects (issue #40).
        if (LatexSpearItem.blocksMainHandShieldCheck(attacker, (Player) (Object) this)) ci.cancel();
    }
}
