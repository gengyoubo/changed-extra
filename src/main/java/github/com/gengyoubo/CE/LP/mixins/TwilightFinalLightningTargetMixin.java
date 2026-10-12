package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TwilightFinalLightningTargetMixin {
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void changede$protectTwilightBosses(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if (bolt.getPersistentData().getBoolean(TwilightFinalEncounter.ATTACK)
                && TwilightFinalEncounter.isTwilightBoss((Entity) (Object) this)) ci.cancel();
    }
}
