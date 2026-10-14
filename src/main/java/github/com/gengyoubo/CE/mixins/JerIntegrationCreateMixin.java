package github.com.gengyoubo.CE.mixins;

import com.simibubi.create.infrastructure.config.AllConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import jeresources.api.IWorldGenRegistry;

@Pseudo
@Mixin(targets = "fr.alasdiablo.jerintegration.compat.create.CreateWorldGen", remap = false)
public abstract class JerIntegrationCreateMixin {
    @Inject(method = "registerWorldGen", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$useServerVerifiedCreateFeatures(IWorldGenRegistry registry, CallbackInfo ci) {
        // Create 6 uses datapack features. The generic bridge supplies its confirmed active features;
        // packaged defaults cannot prove what a server actually generates.
        if (AllConfigs.common().specification.getValues().get("worldgen.v2.zinc_ore.clusterSize") == null) ci.cancel();
    }
}
