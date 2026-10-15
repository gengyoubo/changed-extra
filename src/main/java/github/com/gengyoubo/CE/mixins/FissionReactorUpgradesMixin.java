package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgrade;
import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgradeAccess;
import github.com.gengyoubo.CE.compat.mekanism.fission.FissionUpgradeState;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FissionReactorMultiblockData.class, remap = false)
public abstract class FissionReactorUpgradesMixin implements FissionUpgradeAccess {
    @Unique private final FissionUpgradeState changede$upgrades = new FissionUpgradeState();

    @Override public FissionUpgradeState changede$fissionUpgrades() { return changede$upgrades; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void changede$prepare(Level level, CallbackInfoReturnable<Boolean> cir) {
        changede$upgrades.beforeTick((FissionReactorMultiblockData) (Object) this, level);
    }

    @Inject(method = "burnFuel", at = @At("HEAD"), cancellable = true)
    private void changede$burn(Level level, CallbackInfo ci) {
        if (changede$upgrades.has(FissionUpgrade.PROCESSOR_X) || changede$upgrades.has(FissionUpgrade.PROCESSOR_Y)) {
            changede$upgrades.burn((FissionReactorMultiblockData) (Object) this, level);
            ci.cancel();
        }
    }

    @Inject(method = "handleCoolant", at = @At("HEAD"), cancellable = true)
    private void changede$cool(CallbackInfo ci) {
        var reactor = (FissionReactorMultiblockData) (Object) this;
        if (changede$upgrades.has(FissionUpgrade.COOLING) && !reactor.fluidCoolantTank.isEmpty()) {
            changede$upgrades.cool(reactor);
            ci.cancel();
        }
    }
}
