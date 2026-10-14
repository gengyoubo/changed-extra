package github.com.gengyoubo.CE.mixins;

import jeresources.api.IWorldGenRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server-confirmed adapter uses Mekanism's real dimension, biome and configurable height context. */
@Pseudo
@Mixin(targets = "fr.alasdiablo.jerintegration.compat.mekanism.MekanismWorldGen", remap = false)
public abstract class JerIntegrationMekanismMixin {
    @Inject(method = "registerWorldGen", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$useVerifiedMekanismFeatures(IWorldGenRegistry registry, CallbackInfo ci) {
        ci.cancel();
    }
}
