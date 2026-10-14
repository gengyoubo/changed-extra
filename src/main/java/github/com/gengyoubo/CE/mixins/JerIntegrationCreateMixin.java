package github.com.gengyoubo.CE.mixins;

import com.simibubi.create.infrastructure.config.AllConfigs;
import github.com.gengyoubo.CE.compat.jer.JerCreateWorldGenCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Pseudo
@Mixin(targets = "fr.alasdiablo.jerintegration.compat.create.CreateWorldGen", remap = false)
public abstract class JerIntegrationCreateMixin {
    @Inject(method = "getValues", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$readCurrentZincFeature(String path, CallbackInfoReturnable<Map<String, Number>> cir) {
        // Older Create versions still have this configuration; keep their original adapter.
        if ("worldgen.v2.zinc_ore".equals(path)
                && AllConfigs.common().specification.getValues().get(path + ".clusterSize") == null) {
            cir.setReturnValue(JerCreateWorldGenCompat.zincOreValues());
        }
    }
}
