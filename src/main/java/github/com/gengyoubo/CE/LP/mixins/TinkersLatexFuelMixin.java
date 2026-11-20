package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexFuelAccess;
import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;

@Mixin(value = FuelModule.class, remap = false)
public abstract class TinkersLatexFuelMixin implements LatexFuelAccess {
    @Shadow @Final protected MantleBlockEntity parent;
    @Shadow protected int fuel;
    @Shadow protected int fuelQuality;
    @Shadow protected int temperature;
    @Shadow protected int rate;
    @Unique private boolean changed_extra$clientCoreReady;
    @Unique private boolean changed_extra$latexBatch;
    @Unique private boolean changed_extra$pendingLatex;
    @Unique private BlockPos changed_extra$core;
    @Unique private Object changed_extra$structure;
    @Unique private long changed_extra$checkedAt = Long.MIN_VALUE;
    @Override public MantleBlockEntity changed_extra$parent() { return parent; }
    @Override public boolean changed_extra$coreReady() {
        if (!(parent instanceof HeatingStructureBlockEntity controller) || parent.getLevel() == null) return false;
        if (parent.getLevel().isClientSide) return changed_extra$clientCoreReady;
        if (controller.getStructure() == null) return false;
        if (changed_extra$structure == controller.getStructure() && LatexSmelteryRules.coreAt(controller, changed_extra$core)) return true;
        long now = parent.getLevel().getGameTime();
        if (changed_extra$structure != controller.getStructure() || now < changed_extra$checkedAt || now - changed_extra$checkedAt >= 20 || changed_extra$core != null) {
            changed_extra$structure = controller.getStructure(); changed_extra$checkedAt = now;
            changed_extra$core = LatexSmelteryRules.findCore(controller);
        }
        return changed_extra$core != null;
    }
    @Override public void changed_extra$setCoreReady(boolean ready) { changed_extra$clientCoreReady = ready; }
    @Override public boolean changed_extra$latexBatch() { return changed_extra$latexBatch; }
    @Override public void changed_extra$setLatexBatch(boolean latex) { changed_extra$latexBatch = latex; }
    @Inject(method = "findRecipe", at = @At("HEAD"), cancellable = true)
    private void changed_extra$requireCore(Fluid fluid, CallbackInfoReturnable<MeltingFuel> cir) {
        if (LatexSmelteryRules.isLatex(fluid) && !changed_extra$coreReady()) cir.setReturnValue(null);
    }
    @Inject(method = "tryLiquidFuel", at = @At("HEAD"))
    private void changed_extra$rememberFluid(IFluidHandler handler, boolean consume, CallbackInfoReturnable<Integer> cir) {
        changed_extra$pendingLatex = LatexSmelteryRules.isLatex(handler.getFluidInTank(0).getFluid());
    }
    @Inject(method = "tryLiquidFuel", at = @At("RETURN"))
    private void changed_extra$trackBatch(IFluidHandler handler, boolean consume, CallbackInfoReturnable<Integer> cir) {
        if (consume && cir.getReturnValue() > 0) changed_extra$latexBatch = changed_extra$pendingLatex;
    }
    @Inject(method = "hasFuel", at = @At("HEAD"), cancellable = true)
    private void changed_extra$stopDisabledBatch(CallbackInfoReturnable<Boolean> cir) {
        if (changed_extra$latexBatch && !changed_extra$coreReady()) {
            if (parent.getLevel() != null && !parent.getLevel().isClientSide && fuel > 0) {
                fuel = 0; fuelQuality = 0; temperature = 0; rate = 0; parent.setChangedFast();
            }
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "getFuel", at = @At("HEAD"), cancellable = true)
    private void changed_extra$hideDisabledFlame(CallbackInfoReturnable<Integer> cir) {
        if (changed_extra$latexBatch && !changed_extra$coreReady()) cir.setReturnValue(0);
    }
    @Inject(method = "readFromTag", at = @At("RETURN"))
    private void changed_extra$loadBatch(CompoundTag tag, CallbackInfo ci) { changed_extra$latexBatch = tag.getBoolean("changed_extra_latex_batch"); }
    @Inject(method = "writeToTag", at = @At("RETURN"))
    private void changed_extra$saveBatch(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) { cir.getReturnValue().putBoolean("changed_extra_latex_batch", changed_extra$latexBatch); }
}
