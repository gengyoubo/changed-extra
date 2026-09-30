package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchMekanismEnergyHandler;
import net.foxyas.changedaddon.block.entity.CatalyzerBlockEntity;
import net.foxyas.changedaddon.block.entity.UnifuserBlockEntity;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.ltxprogrammer.changed.block.entity.PurifierBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {PurifierBlockEntity.class, UnifuserBlockEntity.class, CatalyzerBlockEntity.class}, remap = false)
public abstract class AddonMekanismEnergyMixin {
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

    @Inject(method = {"setRemoved", "m_7651_"}, at = @At("TAIL"))
    private void changede$invalidateStrictEnergy(CallbackInfo ci) { changede$strictEnergy.invalidate(); }
}
