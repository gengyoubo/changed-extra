package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexFuelAccess;
import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

@Mixin(value = MultitankFuelModule.class, remap = false)
public abstract class TinkersLatexMultitankMixin {
    @Shadow private int tryFuelPosition(BlockPos pos, boolean consume) { throw new AssertionError(); }
    @Inject(method = "findFuel", at = @At("HEAD"), cancellable = true)
    private void changed_extra$chooseFuel(boolean consume, CallbackInfoReturnable<Integer> cir) {
        var access = (LatexFuelAccess) this;
        if (!(access.changed_extra$parent() instanceof slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity parent)
                || !LatexSmelteryRules.dynamicCoreReady(parent)) return;
        var module = (MultitankFuelModule) (Object) this;
        BlockPos selected = LatexSmelteryRules.selectFuel(module, parent);
        if (selected == null) {
            if (consume) { module.set(2, 0); module.set(3, 0); }
            cir.setReturnValue(0);
        } else cir.setReturnValue(tryFuelPosition(selected, consume));
    }
    // ContainerData overrides use Minecraft's SRG names outside the development runtime.
    @Inject(method = "getCount()I", at = @At("RETURN"), cancellable = true, remap = true)
    private void changed_extra$extraFuelData(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(cir.getReturnValue() + 2); }
    @Inject(method = "get(I)I", at = @At("HEAD"), cancellable = true, remap = true)
    private void changed_extra$sendFuelData(int index, CallbackInfoReturnable<Integer> cir) {
        LatexFuelAccess access = (LatexFuelAccess) this;
        if (index == 7) cir.setReturnValue(access.changed_extra$coreReady() ? 1 : 0);
        if (index == 8) cir.setReturnValue(access.changed_extra$latexBatch() ? 1 : 0);
    }
    @Inject(method = "set(II)V", at = @At("HEAD"), cancellable = true, remap = true)
    private void changed_extra$receiveFuelData(int index, int value, CallbackInfo ci) {
        LatexFuelAccess access = (LatexFuelAccess) this;
        if (index == 7) { access.changed_extra$setCoreReady(value != 0); ci.cancel(); }
        if (index == 8) { access.changed_extra$setLatexBatch(value != 0); ci.cancel(); }
    }
    @Inject(method = "getFuelInfo", at = @At("HEAD"), cancellable = true)
    private void changed_extra$displayUsableFuel(CallbackInfoReturnable<FuelModule.FuelInfo> cir) {
        var access = (LatexFuelAccess) this;
        if (access.changed_extra$parent() instanceof slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity)
            cir.setReturnValue(LatexSmelteryRules.fuelInfo((MultitankFuelModule) (Object) this));
    }
}
