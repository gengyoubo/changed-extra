package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgrade;
import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgradeAccess;
import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgradeState;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FissionReactorMultiblockData.class, remap = false)
public abstract class FissionReactorUpgradesMixin implements FissionUpgradeAccess {
    @Unique private final FissionUpgradeState changed_extra$upgrades = new FissionUpgradeState();
    @Shadow private double biomeAmbientTemp;

    @Override public FissionUpgradeState changed_extra$fissionUpgrades() { return changed_extra$upgrades; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void changed_extra$prepare(Level level, CallbackInfoReturnable<Boolean> cir) {
        changed_extra$upgrades.beforeTick((FissionReactorMultiblockData) (Object) this, level);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lmekanism/generators/common/content/fission/FissionReactorMultiblockData;handleCoolant()V"))
    private void changed_extra$thermalBeforeBoiling(Level level, CallbackInfoReturnable<Boolean> cir) {
        changed_extra$upgrades.thermal().beforeCoolant((FissionReactorMultiblockData) (Object) this, level, biomeAmbientTemp);
    }

    @Inject(method = "handleDamage", at = @At("TAIL"))
    private void changed_extra$thermalCriticalDamage(Level level, CallbackInfo ci) {
        changed_extra$upgrades.thermal().checkFuses((FissionReactorMultiblockData) (Object) this);
    }

    @Inject(method = "meltdownHappened", at = @At("HEAD"))
    private void changed_extra$thermalMeltdown(Level level, CallbackInfo ci) {
        changed_extra$upgrades.thermal().meltdown((FissionReactorMultiblockData) (Object) this, level);
    }

    @Inject(method = "burnFuel", at = @At("HEAD"), cancellable = true)
    private void changed_extra$burn(Level level, CallbackInfo ci) {
        if (changed_extra$upgrades.has(FissionUpgrade.PROCESSOR_X) || changed_extra$upgrades.has(FissionUpgrade.PROCESSOR_Y)) {
            changed_extra$upgrades.burn((FissionReactorMultiblockData) (Object) this, level);
            ci.cancel();
        }
    }

    @Inject(method = "handleCoolant", at = @At("HEAD"), cancellable = true)
    private void changed_extra$cool(CallbackInfo ci) {
        var reactor = (FissionReactorMultiblockData) (Object) this;
        if (changed_extra$upgrades.has(FissionUpgrade.COOLING) && !reactor.fluidCoolantTank.isEmpty()) {
            changed_extra$upgrades.cool(reactor);
            ci.cancel();
        }
    }
}
