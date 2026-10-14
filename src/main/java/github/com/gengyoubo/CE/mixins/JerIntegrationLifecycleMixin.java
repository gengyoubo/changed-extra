package github.com.gengyoubo.CE.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The API retains registrations across reloads, but biome/config data needs a connected world. */
@Pseudo
@Mixin(targets = "fr.alasdiablo.jerintegration.compat.CompatibilityHandler", remap = false)
public abstract class JerIntegrationLifecycleMixin {
    @Unique private static boolean changede$registered;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true, remap = false)
    private static void changede$deferUntilWorldLoaded(CallbackInfo ci) {
        if (Minecraft.getInstance().getConnection() == null || changede$registered) {
            ci.cancel();
        } else {
            changede$registered = true;
        }
    }
}
