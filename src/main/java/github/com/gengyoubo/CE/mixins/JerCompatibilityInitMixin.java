package github.com.gengyoubo.CE.mixins;

import fr.alasdiablo.jerintegration.compat.CompatibilityHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "jeresources.compatibility.Compatibility", remap = false)
public abstract class JerCompatibilityInitMixin {
    // Run before JERAPI.commit so the deferred entries are included in this JEI session.
    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private static void changede$registerIntegrations(CallbackInfo ci) {
        CompatibilityHandler.init();
    }
}
