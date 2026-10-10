package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexFuelAccess;
import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

@Mixin(value = MultitankFuelModule.class, remap = false)
public abstract class TinkersLatexMultitankMixin {
    // ContainerData overrides use Minecraft's SRG names outside the development runtime.
    @Inject(method = "getCount()I", at = @At("RETURN"), cancellable = true, remap = true)
    private void changede$extraFuelData(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(cir.getReturnValue() + 2); }
    @Inject(method = "get(I)I", at = @At("HEAD"), cancellable = true, remap = true)
    private void changede$sendFuelData(int index, CallbackInfoReturnable<Integer> cir) {
        LatexFuelAccess access = (LatexFuelAccess) this;
        if (index == 7) cir.setReturnValue(access.changede$coreReady() ? 1 : 0);
        if (index == 8) cir.setReturnValue(access.changede$latexBatch() ? 1 : 0);
    }
    @Inject(method = "set(II)V", at = @At("HEAD"), cancellable = true, remap = true)
    private void changede$receiveFuelData(int index, int value, CallbackInfo ci) {
        LatexFuelAccess access = (LatexFuelAccess) this;
        if (index == 7) { access.changede$setCoreReady(value != 0); ci.cancel(); }
        if (index == 8) { access.changede$setLatexBatch(value != 0); ci.cancel(); }
    }
    @Inject(method = "getFuelInfo", at = @At("HEAD"), cancellable = true)
    private void changede$displayUsableFuel(CallbackInfoReturnable<FuelModule.FuelInfo> cir) {
        var access = (LatexFuelAccess) this;
        if (access.changede$parent() instanceof slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity)
            cir.setReturnValue(LatexSmelteryRules.fuelInfo((MultitankFuelModule) (Object) this));
    }
}
