package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class TwilightFinalLightningFireMixin {
    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void changede$preventIndirectBossDamage(int extraFire, CallbackInfo ci) {
        if (((LightningBolt) (Object) this).getPersistentData().getBoolean(TwilightFinalEncounter.ATTACK)) ci.cancel();
    }
}
