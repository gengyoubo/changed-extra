package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class LatexSpearShieldMixin {
    @Redirect(method = "blockUsingShield", at = @At(value = "INVOKE", remap = false, target =
            "Lnet/minecraft/world/item/ItemStack;canDisableShield(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean changede$useActualWeapon(ItemStack mainHand, ItemStack shield, LivingEntity defender, LivingEntity attacker) {
        // MC-304593: 1.20.1 asks about the main-hand axe even when the off-hand spear hit.
        return !LatexSpearItem.blocksMainHandShieldCheck(attacker, defender)
                && mainHand.canDisableShield(shield, defender, attacker);
    }
}
