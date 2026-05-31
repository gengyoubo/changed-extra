package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import net.ltxprogrammer.changed.world.inventory.InfuserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfuserMenu.class, remap = false)
public abstract class InfuserMenuEnergyMixin {
    @Inject(method = {"slotsChanged", "m_6199_"}, at = @At("TAIL"))
    private void changede$hideResultWithoutEnergy(Container container, CallbackInfo ci) {
        InfuserMenu menu = (InfuserMenu) (Object) this;
        if (!WorkbenchEnergyRules.consumeInfuserCraft(menu.world, new BlockPos(menu.x, menu.y, menu.z), true)) {
            Slot resultSlot = menu.getResultSlot();
            resultSlot.set(ItemStack.EMPTY);
        }
    }
}
