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
    @Unique private boolean changede$clientCoreReady;
    @Unique private boolean changede$latexBatch;
    @Unique private boolean changede$pendingLatex;
    @Unique private BlockPos changede$core;
    @Unique private Object changede$structure;
    @Unique private long changede$checkedAt = Long.MIN_VALUE;
    @Override public MantleBlockEntity changede$parent() { return parent; }
    @Override public boolean changede$coreReady() {
        if (!(parent instanceof HeatingStructureBlockEntity controller) || parent.getLevel() == null) return false;
        if (parent.getLevel().isClientSide) return changede$clientCoreReady;
        if (controller.getStructure() == null) return false;
        if (changede$structure == controller.getStructure() && LatexSmelteryRules.coreAt(controller, changede$core)) return true;
        long now = parent.getLevel().getGameTime();
        if (changede$structure != controller.getStructure() || now < changede$checkedAt || now - changede$checkedAt >= 20 || changede$core != null) {
            changede$structure = controller.getStructure(); changede$checkedAt = now;
            changede$core = LatexSmelteryRules.findCore(controller);
        }
        return changede$core != null;
    }
    @Override public void changede$setCoreReady(boolean ready) { changede$clientCoreReady = ready; }
    @Override public boolean changede$latexBatch() { return changede$latexBatch; }
    @Override public void changede$setLatexBatch(boolean latex) { changede$latexBatch = latex; }
    @Inject(method = "findRecipe", at = @At("HEAD"), cancellable = true)
    private void changede$requireCore(Fluid fluid, CallbackInfoReturnable<MeltingFuel> cir) {
        if (LatexSmelteryRules.isLatex(fluid) && !changede$coreReady()) cir.setReturnValue(null);
    }
    @Inject(method = "tryLiquidFuel", at = @At("HEAD"))
    private void changede$rememberFluid(IFluidHandler handler, boolean consume, CallbackInfoReturnable<Integer> cir) {
        changede$pendingLatex = LatexSmelteryRules.isLatex(handler.getFluidInTank(0).getFluid());
    }
    @Inject(method = "tryLiquidFuel", at = @At("RETURN"))
    private void changede$trackBatch(IFluidHandler handler, boolean consume, CallbackInfoReturnable<Integer> cir) {
        if (consume && cir.getReturnValue() > 0) changede$latexBatch = changede$pendingLatex;
    }
    @Inject(method = "hasFuel", at = @At("HEAD"), cancellable = true)
    private void changede$stopDisabledBatch(CallbackInfoReturnable<Boolean> cir) {
        if (changede$latexBatch && !changede$coreReady()) {
            if (parent.getLevel() != null && !parent.getLevel().isClientSide && fuel > 0) {
                fuel = 0; fuelQuality = 0; temperature = 0; rate = 0; parent.setChangedFast();
            }
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "getFuel", at = @At("HEAD"), cancellable = true)
    private void changede$hideDisabledFlame(CallbackInfoReturnable<Integer> cir) {
        if (changede$latexBatch && !changede$coreReady()) cir.setReturnValue(0);
    }
    @Inject(method = "readFromTag", at = @At("RETURN"))
    private void changede$loadBatch(CompoundTag tag, CallbackInfo ci) { changede$latexBatch = tag.getBoolean("changede_latex_batch"); }
    @Inject(method = "writeToTag", at = @At("RETURN"))
    private void changede$saveBatch(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) { cir.getReturnValue().putBoolean("changede_latex_batch", changede$latexBatch); }
}
