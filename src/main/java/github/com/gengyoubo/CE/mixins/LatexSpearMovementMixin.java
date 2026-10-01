package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Holding the spear engages its tip without the bow/trident movement slowdown. */
@Mixin(LocalPlayer.class)
public abstract class LatexSpearMovementMixin {
    @Redirect(method = {"aiStep", "canStartSprinting"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean changede$spearAllowsRunning(LocalPlayer player) {
        return player.isUsingItem() && !(player.getUseItem().getItem() instanceof LatexSpearItem);
    }
}
