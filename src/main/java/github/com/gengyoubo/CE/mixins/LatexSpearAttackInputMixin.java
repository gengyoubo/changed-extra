package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.client.LatexSpearClient;
import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class LatexSpearAttackInputMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void changede$jabOnPress(CallbackInfoReturnable<Boolean> ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof LatexSpearItem) {
            LatexSpearClient.requestJab();
            ci.setReturnValue(false);
        }
    }
}
