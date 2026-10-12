package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.twilight.TwilightFinalEncounter;
import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TwilightFinalHealthLockMixin {
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float changede$clampCheckpoint(float health) {
        return (Object) this instanceof Experiment009BossEntity boss ? TwilightFinalEncounter.clamp(boss, health) : health;
    }
    @Inject(method = "setHealth", at = @At("RETURN"))
    private void changede$startGuardianWave(float health, CallbackInfo ci) {
        if ((Object) this instanceof Experiment009BossEntity boss) TwilightFinalEncounter.healthChanged(boss);
    }
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void changede$preventEarlyFinalDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Experiment009BossEntity boss && TwilightFinalEncounter.isFinal(boss)
                && (TwilightFinalEncounter.locked(boss) || TwilightFinalEncounter.stage(boss) < 6)) ci.cancel();
    }
}
