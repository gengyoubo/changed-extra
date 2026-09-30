package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchMekanismEnergyHandler;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.InfuserPowerBlockEntity;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InfuserPowerBlockEntity.class, remap = false)
public abstract class InfuserMekanismEnergyMixin {
    @Unique private LazyOptional<IStrictEnergyHandler> changede$strictEnergy = LazyOptional.empty();

    @Unique
    private LazyOptional<IStrictEnergyHandler> changede$getStrictEnergy() {
        if (!changede$strictEnergy.isPresent()) {
            WorkbenchEnergyHolder holder = (WorkbenchEnergyHolder) (Object) this;
            changede$strictEnergy = LazyOptional.of(() -> new WorkbenchMekanismEnergyHandler(holder));
        }
        return changede$strictEnergy;
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void changede$exposeStrictEnergy(Capability<T> cap, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (cap == Capabilities.STRICT_ENERGY) cir.setReturnValue(changede$getStrictEnergy().cast());
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void changede$invalidateStrictEnergy(CallbackInfo ci) { changede$strictEnergy.invalidate(); }
}
