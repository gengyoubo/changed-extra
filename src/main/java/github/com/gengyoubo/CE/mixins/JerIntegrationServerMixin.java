package github.com.gengyoubo.CE.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** JER's JEI worldgen integration is client data. Dedicated setup precedes Mekanism's world config. */
@Pseudo
@Mixin(targets = "fr.alasdiablo.jerintegration.compat.CompatibilityHandler", remap = false)
public abstract class JerIntegrationServerMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private static void changede$skipClientWorldgenOnServer(CallbackInfo ci) {
        ci.cancel();
    }
}
