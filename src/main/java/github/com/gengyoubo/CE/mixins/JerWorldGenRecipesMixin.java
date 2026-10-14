package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.jer.JerWorldGenCompat;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "jeresources.jei.JEIConfig", remap = false)
public abstract class JerWorldGenRecipesMixin {
    @Inject(method = "registerRecipes", at = @At("TAIL"), remap = false)
    private void changede$registerVerifiedOres(IRecipeRegistration registration, CallbackInfo ci) {
        JerWorldGenCompat.register(registration);
    }

    @Inject(method = "onRuntimeAvailable", at = @At("TAIL"), remap = false)
    private void changede$enableOreUpdates(IJeiRuntime runtime, CallbackInfo ci) {
        JerWorldGenCompat.runtimeAvailable(runtime);
    }
}
